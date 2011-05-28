package weightLearner.wpll;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import stat.convergence.SequentialTester;
import util.MyException;
import weightLearner.AbstractScore;
import weightLearner.Score;
import fol.Formula;
import fol.Predicate;

/**
 * @author Leonardo Castilho Couto
 *
 */
public class WeightedPseudoLogLikelihood extends AbstractScore {
	
	private final Map<Predicate, DataCount> dataCounts;
	private double[] grad = new double[0];
	private SequentialTester tester;
	private int sampleLimit;

	public WeightedPseudoLogLikelihood(Set<Predicate> predicates) {
		super(predicates);
		int defaultSize = (int) Math.ceil(predicates.size()*1.4);
		this.dataCounts = new HashMap<Predicate, DataCount>(defaultSize);
		// populate inversePllWeight with the number of groundings for each predicate. 
		for (Predicate p : predicates) {
			this.dataCounts.put(p, new DataCount(p));
		}
		this.sampleLimit = -1;
	}
	
	private WeightedPseudoLogLikelihood(List<Formula> formulas,
			Set<Predicate> predicates,
			Map<Predicate, List<Formula>> predicateFormulas,
			Map<Predicate, DataCount> dataCounts) {
		super(formulas, predicates, predicateFormulas);
		this.dataCounts = dataCounts;
		this.sampleLimit = -1;
	}

	/* (non-Javadoc)
	 * @see weightLearner.Score#getScore(double[])
	 */
	@Override
	public double getScore(double[] weights) {
		List<FormulaData> fdata = new LinkedList<FormulaData>();
		double wpll = 0;
		

		List<Formula> formulas = this.getFormulas();
		Iterator<Formula> it = formulas.iterator();
		for (int i = 0; i < formulas.size(); i++) {
			Formula f = it.next();
			double d = (i < weights.length) ? weights[i] : 0;
			fdata.add(new FormulaData(f, d, i));
		}

		this.grad = new double[formulas.size()];
		for (Entry<Predicate, DataCount> e : this.dataCounts.entrySet()) {
			Predicate p = e.getKey();
			boolean isEmpty = this.predicateFormulas.get(p).isEmpty();
			wpll += isEmpty ? -.69 : this.predicateWPll(p, e.getValue(), fdata);
		}
		return wpll;
	}

	private double predicateWPll(Predicate p, DataCount dataCount, List<FormulaData> fdata) {

		double predicatePLL = 0;
		double[] pGrad = new double[fdata.size()];

		for (List<FormulaCount> formulaCount : dataCount) { // para cada ground Atom de p
			// wx = sum_i(w_i * n_i[ground = x    ]);
			// a  = sum_i(w_i * n_i[ground = true ]);
			// b  = sum_i(w_i * n_i[ground = false]);
			double wx = 0, a = 0, b = 0;

			// Summation over formulas
			for (FormulaCount fct : formulaCount) { // para cada Formula com o predicado p
				Formula f = fct.formula;
				FormulaData fd = FormulaData.get(fdata, f);
				double wi = fd.w;
				Data count = fct.data;

				wx = wx + wi*count.value;
				a = a + wi*count.trueCount;
				b = b + wi*count.falseCount;
			}

			// exp = e^(abs(a-b)), invexp = exp^-1
			// if invexp ~ 0, ignore it.
			boolean ignoreExp = false;
			double exp = 0, invexp = 0;
			double diff = Math.abs(a - b);
			if (Double.compare(diff, 50) > -1) {
				ignoreExp = true;
			} else {
				exp = Math.exp(diff);
				invexp = Math.exp(-diff);
			}

			predicatePLL = predicatePLL + wx - Math.max(a, b) - Math.log(1+invexp);

			// compute the partial derivative with respect to w_i
			// if a > b then the derivative = 
			// n_i[x] - n_i[true] + (n_i[true] - n_i[false])/(1+exp)
			for (FormulaCount fct : formulaCount) {
				Formula f = fct.formula;
				FormulaData fd = FormulaData.get(fdata, f);
				Data count = fct.data;
				double tc = count.trueCount;
				double fc = count.falseCount;
				double x  = count.value;
				int i = fd.i;

				if (a > b) {
					pGrad[i] = pGrad[i] + x - tc;
					if(!ignoreExp) {
						pGrad[i] = pGrad[i] + (tc-fc)/(exp+1.0);
					}

				} else {
					pGrad[i] = pGrad[i] + x - fc;			
					if(!ignoreExp) {
						pGrad[i] = pGrad[i] + (fc-tc)/(exp+1.0);
					}
				}
			}
		}

		// multiply PllWeight (the inverse number of groundings for each predicate)
		// see paper (TODO: CITE PAPER!!)
		// also need to multiply (number of grounds)/(number of sampled grounds).
		// so, only divide by the number of sampled grounds
		for (int i = 0; i < pGrad.length; i++) {
			this.grad[i] = this.grad[i] + (pGrad[i] / dataCount.sampledAtoms());
		}
		return (predicatePLL / dataCount.sampledAtoms());
	}


	/* (non-Javadoc)
	 * @see math.RnToRnFunction#g(double[])
	 */
	@Override
	public double[] g(double[] x) {
		return this.grad;
	}

	/* (non-Javadoc)
	 * @see weightLearner.AbstractScore#addFormula(fol.Formula)
	 */
	@Override
	public boolean addFormula(Formula f) {
		boolean b = super.addFormula(f);
		for (Predicate p : f.getPredicates()) {
			if (p != Predicate.equals) {
				this.dataCounts.get(p).addFormula(f);
			}
		}
		return b;
	}

	/* (non-Javadoc)
	 * @see weightLearner.AbstractScore#addFormulas(java.util.List)
	 */
	@Override
	public boolean addFormulas(List<Formula> formulas) {
		return super.addFormulas(formulas);
	}

	/* (non-Javadoc)
	 * @see weightLearner.AbstractScore#removeFormula(fol.Formula)
	 */
	@Override
	public boolean removeFormula(Formula f) {
		for (Predicate p : f.getPredicates()) {
			if (p != Predicate.equals) {
				this.dataCounts.get(p).removeFormula(f);
			}
		}
		return super.removeFormula(f);
	}

	// inicialmente:
	//   - apenas uma funcao
	//   vou escolhendo Atoms pertencentes a p.getGrounds aleatoriamente
	//   e calculando data counts para esse ground atom (usando sampling para encontrar
	//   os data counts). vou passando cada um desses datacounts para o algoritmo
	//   do criterio de parada, ateh nao precisar mais de atoms para estimativa.
	// depois:
	//   - adicionada a segunda funcao:
	//   inicialmente vou utilizando os atoms que jah foram usados (fazem parte do keySet)
	//   se convergir antes de utilizar todos, nao calcula todos, se utilizou todos e ainda
	//   nao convergiu, pega mais atoms aleatoriamente, e calcula para as outras funcoes também
	//   - adicionada a n-esima funcao:
	//   para cada atom que for calcular, ver se foi calculada para todas as funcoes.

	// colocarei o trabalho de fazer o sampling nessa classe.
	// deixar transparente para o wpll, como se ele nao estivesse fazendo sampling, e sim
	// calculando exatamente a funcao!!

	// Soma_{r=predicate} (
	//     Soma_{gr=ground_r} ( 
	//         x -max(a,b) -log(1+e^min[a-b,b-a]) -> o log varia entre 2 e 1+e
	//     )
	// )
	//           --> fator mais importante eh a soma de de a e b, parar de usar o somatorio
	//               quando tiver convertido para a soma de x - max(a,b).
	//
	// onde x = Soma_{i=function}( w_i * n_i[gr = valor(gr)] = w_i * n_i[x] )
	//      a = Soma_{i=function}( w_i * n_i[gr = true     ] = w_i * n_i[a] )
	//      b = Soma_{i=function}( w_i * n_i[gr = false    ] = w_i * n_i[b] )
	//      n_i = numero de counts da formula i com o valor de gr especificado
	//      gr = atom do predicado r, com constantes (grounded)
	//
	// converge se:
	//   n_i: separado
	//   apos calculado n_i, assumir w_i = 1 e ver a convergencia para a e para b.
	//
	//   n_i terah o valor como se nao tivesse passado por sampling
	//   a e b terao um indicativo de quandos samples eh nescessario
	//   datacount tera um procedimento para passar o valor do somatorio
	//   em gr, dado os w_i's
	//
	//   Problema: se passar essa soma, tambem tem que passar o gradiente!
	//    ver se da para calcular o gradiente sem ter de elevar a e, como feito
	//    para o somatorio, ver as notas.

	@Override
	public Score copy() {
		Map<Predicate, List<Formula>> predicateFormulas = 
			new HashMap<Predicate, List<Formula>>(this.predicateFormulas);
		Set<Predicate> predicates = this.getPredicates();
		List<Formula> formulas = new LinkedList<Formula>(this.getFormulas());
		for (Predicate p : predicates) {
			predicateFormulas.put(p, new LinkedList<Formula>(predicateFormulas.get(p)));
		}
		Map<Predicate, DataCount> dataCounts = new HashMap<Predicate, DataCount>(this.dataCounts);
		for (Predicate p : predicates) {
			dataCounts.put(p, dataCounts.get(p).copy());
		}
		WeightedPseudoLogLikelihood wpll = new WeightedPseudoLogLikelihood(
				formulas, predicates, predicateFormulas, dataCounts);
		wpll.sampleLimit = this.sampleLimit;
		wpll.tester = this.tester;
		return wpll;
	}
	
	private static final class FormulaData {
		
		public final Formula f;
		public final double w;
		public final int i;
		
		public FormulaData(Formula f, double w, int i) {
			this.f = f;
			this.w = w;
			this.i = i;
		}
		
		public static FormulaData get(List<FormulaData> l, Formula f) {
			for (FormulaData fd : l) {
				if (fd.f == f) {
					return fd;
				}
			}
			throw new MyException("List l does not contains f");
		}
		
	}
	
	/**
	 * Set the SequentialTester that will be used to compute
	 * the convergence of formulaCounts for each formula. 
	 * It will limit the number of samples used to get the formulaCount.
	 */
	public void setTester(SequentialTester tester) {
		this.tester = tester;
		for (DataCount d : this.dataCounts.values()) {
			d.setTester(tester);
		}
	}
	
	/**
	 * Set the max number of groundings per predicate that will
	 * be used to approximate the predicateWpll.<br><br>
	 * 
	 * WARNING! This parameter can only be set BEFORE any
	 * formulas were added to the wpll. Or else will throw an
	 * UnsupportedOperationException.
	 * 
	 * @param n the sample limit
	 */
	public void setSampleLimit(int n) {
		this.sampleLimit = n;
		for (DataCount d : this.dataCounts.values()) {
			d.setSampleSize(n);
		}
	}

}
