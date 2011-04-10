package weightLearner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import stat.convergence.SequentialConvergenceTester;
import stat.sampling.RandomIterator;
import util.ListPointer;
import util.MyException;
import fol.Atom;
import fol.Constant;
import fol.Formula;
import fol.Predicate;
import fol.Term;
import fol.Variable;

/**
 * @author Leonardo Castilho Couto
 *
 */
public class CopyOfWeightedPseudoLogLikelihood extends AbstractScore {

	private final Map<Predicate, DataCount> dataCounts;
	private double[] grad = new double[0];

	public CopyOfWeightedPseudoLogLikelihood(Set<Predicate> predicates) {
		super(predicates);
		int defaultSize = (int) Math.ceil(predicates.size()*1.4);
		this.dataCounts = new HashMap<Predicate, DataCount>(defaultSize);
		// populate inversePllWeight with the number of groundings for each predicate. 
		for (Predicate p : predicates) {
			this.dataCounts.put(p, new DataCount(p));
		}
	}
	
	private CopyOfWeightedPseudoLogLikelihood(List<Formula> formulas,
			Set<Predicate> predicates,
			Map<Predicate, List<Formula>> predicateFormulas,
			Map<Predicate, DataCount> dataCounts) {
		super(formulas, predicates, predicateFormulas);
		this.dataCounts = dataCounts; // TODO: NAO EH USADO PELO COPY????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
	}

	/* (non-Javadoc)
	 * @see weightLearner.Score#getScore(double[])
	 */
	@Override
	public double getScore(double[] weights) {
		List<Formula> formulas = this.getFormulas();
		if (!(formulas.size() == weights.length)) {
			throw new RuntimeException("Different number of formulas and weights");
		}
		double wpll = 0;

		Map<Formula, Double> wf = new HashMap<Formula, Double>(formulas.size()*2);
		Map<Formula, Integer> idx = new HashMap<Formula, Integer>(formulas.size()*2);
		for (int i = 0; i < formulas.size(); i++) {
			wf.put(formulas.get(i), new Double(weights[i]));
			idx.put(formulas.get(i), new Integer(i));
		}

		this.grad = new double[weights.length];
		for (Predicate p : this.predicateFormulas.keySet()) {
			wpll = wpll + this.predicateWPll(p, wf, idx);
		}
		return wpll;
	}

	private double predicateWPll(Predicate p, Map<Formula, Double> weight, Map<Formula, Integer> idx) {

		DataCount dataCount = dataCounts.get(p);
		double predicatePLL = 0;
		double[] pGrad = new double[weight.size()];

		for (Atom atom : dataCount.keySet()) {

			// wx = sum_i(w_i * n_i[ground = x    ]);
			// a  = sum_i(w_i * n_i[ground = true ]);
			// b  = sum_i(w_i * n_i[ground = false]);
			double wx = 0, a = 0, b = 0;

			Map<Formula, Data> formulaCount = dataCount.get(atom);

			// Summation over formulas
			for (Formula f : formulaCount.keySet()) {
				double wi = weight.get(f);
				Data count = formulaCount.get(f);

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
			for (Formula f : formulaCount.keySet()) {
				Data count = formulaCount.get(f);
				double tc = count.trueCount;
				double fc = count.falseCount;
				double x  = count.value;
				int i = idx.get(f).intValue();

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
		boolean b = super.addFormulas(formulas);
		for (Formula f : formulas) {
			for (Predicate p : f.getPredicates()) {
				this.dataCounts.get(p).addFormula(f);
			}
		}
		return b;
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

	private static class Data {
		public final double trueCount;
		public final double falseCount;
		public final double value;

		public Data(double trueCount, double falseCount, double value) {
			this.trueCount = trueCount;
			this.falseCount = falseCount;
			this.value = value;
		}
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

	private static class DataCount extends HashMap<Atom,Map<Formula,Data>> {

		private static final long serialVersionUID = 8714785841871940035L;
		private final Set<Formula> formulas;
		private final List<Atom> atoms;
		public final Predicate predicate;
		private final Iterator<Atom> iterator;
		
		private final Map<Formula, Integer> atomPosition;
		private final Map<Formula, List<Variable>> atomVariables;

		public DataCount(Predicate p) {
			super();
			this.predicate = p;
			this.atoms = new ArrayList<Atom>();
			this.formulas = new HashSet<Formula>();
			this.iterator = new RandomIterator<Atom>(p.getGroundings().keySet()).iterator();
			this.atomPosition = new HashMap<Formula, Integer>();
			this.atomVariables = new HashMap<Formula, List<Variable>>();
		}
		
		private DataCount(DataCount old) {
			super();
			this.formulas = new HashSet<Formula>(old.formulas);
			this.atoms = new ArrayList<Atom>(old.atoms);
			this.predicate = old.predicate;
			Set<Atom> groundings = new HashSet<Atom>(this.predicate.getGroundings().keySet());
			groundings.removeAll(this.atoms);
			this.iterator = new RandomIterator<Atom>(groundings).iterator();
			this.atomPosition = new HashMap<Formula, Integer>(old.atomPosition);
			this.atomVariables = new HashMap<Formula, List<Variable>>(old.atomVariables);
		}

		public int sampledAtoms() {
			return this.atoms.size();
		}

		private ListPointer<Atom> getPointer(Formula f) {
			ListPointer<Atom> out = f.getAtomPointer(this.predicate);
			if (out == null) {
				throw new MyException("Formula \"" + f.toString() + 
						"\" contains no Predicate \"" + this.predicate.toString() + 
				"\" with Variables only.");
			}
			return out;
		}
		
		private Data addAtom(Formula formula, Atom atom) {
			if (formula instanceof Atom) {
				Data d = new Data(1.0, 0.0, atom.getValue());
				this.get(atom).put(formula, d);
				return d;
			}
			
			int idx = this.atomPosition.get(formula);
			List<Variable> vars = this.atomVariables.get(formula);
			List<Constant> cons = new ArrayList<Constant>(vars.size());
			for (Term t : atom.terms) { cons.add((Constant) t); }
			Formula grounded = formula.replaceVariables(vars, cons);
			List<Atom> atoms = grounded.getAtoms();

			atoms.set(idx, Atom.TRUE);
			double trueCount = grounded.trueCounts();
			atoms.set(idx, Atom.FALSE);
			double falseCount = grounded.trueCounts();
			double value = atom.value*trueCount + (1.0-atom.value)*falseCount;

			Data d = new Data(trueCount, falseCount, value);
			this.get(atom).put(formula, d);
			return d;	
		}
		
		private List<Data> addAtoms(Formula formula, List<Atom> atoms) {
			List<Data> out = new ArrayList<Data>(atoms.size());
			for (Atom a : atoms) {
				out.add(this.addAtom(formula, a));
			}
			return out;
		}


		public void addFormula(Formula formula) {
			
			if (!(formula instanceof Atom)) { // init atomPosition and atomVariables
				ListPointer<Atom> p = this.getPointer(formula);
				this.atomPosition.put(formula, p.i);
				Term[] terms = p.get().terms;
				List<Variable> vars = new ArrayList<Variable>(terms.length);
				for (Term t : terms) {
					vars.add((Variable) t);
				}
				this.atomVariables.put(formula, vars);
			}

			SequentialConvergenceTester tester = new SequentialConvergenceTester(.95, .05);


			if (!this.atoms.isEmpty()) {
				double[] v = new double[this.atoms.size()];
				List<Data> data = this.addAtoms(formula, this.atoms);
				for (int i = 0; i < v.length; i++) {
					v[i] = data.get(i).value;
				}
				tester.evaluate(v);
			}

			if(!tester.hasConverged()) {
				List<Atom> atoms = new ArrayList<Atom>();
				while(!tester.hasConverged() && iterator.hasNext()) {
					Atom a = iterator.next();
					atoms.add(a);
					Map<Formula, Data> map = new HashMap<Formula, Data>(2*this.formulas.size());
					this.put(a, map);
					Data d = this.addAtom(formula, a);
					tester.increment(d.value);
				}
				this.atoms.addAll(atoms);
				for (Formula f : this.formulas) {
					this.addAtoms(f, atoms);
				}
			}
			this.formulas.add(formula);
		}

		public boolean removeFormula(Formula f) {
			if(this.formulas.remove(f)) {
				for (Map<Formula, Data> map : this.values()) {
					map.remove(f);
				}
				this.atomPosition.remove(f);
				this.atomVariables.remove(f);
				return true;
			}
			return false;
		}
		
		public DataCount copy() {
			DataCount copy = new DataCount(this);
			for (Atom a : this.atoms) {
				copy.put(a, new HashMap<Formula, Data>(this.get(a)));
			}
			return copy;
		}

	}

	@Override
	public Score copy() {
		Map<Predicate, List<Formula>> predicateFormulas = 
			new HashMap<Predicate, List<Formula>>(this.predicateFormulas);
		Set<Predicate> predicates = this.getPredicates();
		List<Formula> formulas = this.getFormulas();
		for (Predicate p : predicates) {
			predicateFormulas.put(p, new LinkedList<Formula>(predicateFormulas.get(p)));
		}
		Map<Predicate, DataCount> dataCounts = new HashMap<Predicate, DataCount>(this.dataCounts);
		for (Predicate p : predicates) {
			dataCounts.put(p, dataCounts.get(p).copy());
		}
		return new CopyOfWeightedPseudoLogLikelihood(formulas, predicates, 
				predicateFormulas, dataCounts);
	}

}
