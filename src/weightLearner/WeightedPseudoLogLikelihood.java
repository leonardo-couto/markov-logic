package weightLearner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import main.Settings;
import stat.ConvergenceTester;
import stat.Sampler;
import util.ListPointer;
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
public class WeightedPseudoLogLikelihood extends AbstractScore {

	private final Map<Predicate, Long> inversePllWeight; // Not formulas weight! TODO: citar referencia
	private final Map<Predicate, DataCount> dataCounts;

	public WeightedPseudoLogLikelihood(Set<Predicate> predicates) {
		super(predicates);
		int defaultSize = (int) Math.ceil(predicates.size()*1.4);
		this.inversePllWeight = new HashMap<Predicate, Long>(defaultSize);
		this.dataCounts = new HashMap<Predicate, DataCount>(defaultSize);
		// populate inversePllWeight with the number of groundings for each predicate. 
		for (Predicate p : predicates) {
			this.inversePllWeight.put(p, p.totalGroundsNumber());
			this.dataCounts.put(p, new DataCount(p));
		}
	}

	/* (non-Javadoc)
	 * @see weightLearner.Score#getScore(double[])
	 */
	@Override
	public double getScore(double[] weights) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see math.RnToRnFunction#g(double[])
	 */
	@Override
	public double[] g(double[] x) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see weightLearner.AbstractScore#addFormula(fol.Formula)
	 */
	@Override
	public void addFormula(Formula f) {
		super.addFormula(f);
		for (Predicate p : formulaPredicates.get(f)) {
			this.dataCounts.get(p).addFormula(f);
		}
	}

	/* (non-Javadoc)
	 * @see weightLearner.AbstractScore#addFormulas(java.util.List)
	 */
	@Override
	public void addFormulas(List<Formula> formulas) {
		super.addFormulas(formulas);
		for (Formula f : formulas) {
			for (Predicate p : this.formulaPredicates.get(f)) {
				this.dataCounts.get(p).addFormula(f);
			}
		}
	}

	/* (non-Javadoc)
	 * @see weightLearner.AbstractScore#removeFormula(fol.Formula)
	 */
	@Override
	public boolean removeFormula(Formula f) {
		// TODO: remove counts
		return super.removeFormula(f);
	}

	private static class Data {
		public final FormulaCount trueCount;
		public final FormulaCount falseCount;
		public final double value;

		public Data(FormulaCount trueCount, FormulaCount falseCount, double value) {
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
	//         x -max(a,b) -log(1+e^min[a/b,b/a]) -> o log varia entre 2 e 1+e
	//     )
	// )
	//           --> fator mais importante eh a soma de de a e b, parar de usar o somatorio
	//               quando tiver convertido para a soma de x - max(a,b).
	//
	// onde x = Soma_{i=function}( w_i * n_i[gr = valor(gr)] )
	//      a = Soma_{i=function}( w_i * n_i[gr = true     ] )
	//      b = Soma_{i=function}( w_i * n_i[gr = false    ] )
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

		private final Set<Formula> formulas;
		private final List<Atom> atoms;
		public final Predicate predicate;
		private final Iterator<List<Atom>> iterator;

		public DataCount(Predicate p) {
			this.predicate = p;
			this.atoms = new ArrayList<Atom>();
			this.formulas = new HashSet<Formula>();
			Sampler<Atom> sampler = new Sampler<Atom>(p.getGroundings().keySet());
			sampler.setMaxSamples(Settings.formulaCountMaxSamples);
			this.iterator = sampler.iterator();
		}

		public int sampledAtoms() {
			return this.atoms.size();
		}

		private ListPointer<Formula> getPointer(Formula f) {
			if (f instanceof Atom) {
				List<Formula> list = Collections.singletonList(f);
				return new ListPointer<Formula>(list, 0);
			}
			ListPointer<Formula> out = f.getAtomPointer(this.predicate);
			if (out == null) {
				throw new RuntimeException("Formula \"" + f.toString() + 
						"\" contains no Predicate \"" + this.predicate.toString() + 
				"\" with Variables only.");
			}
			return out;
		}

		private static Sampler<Constant> getSampler(List<Variable> variables) {
			List<Set<Constant>> constants = new ArrayList<Set<Constant>>(variables.size());
			for (Variable v : variables) {
				constants.add(v.getConstants());
			}
			return new Sampler<Constant>(constants);
		}

		private List<Data> addAtoms(Formula formula, List<Atom> atoms) {
			List<Data> out = new ArrayList<Data>(atoms.size());
			if (formula instanceof Atom) {

				for (Atom a : atoms) {
					FormulaCount trueCount  = new FormulaCount(1, 1);
					FormulaCount falseCount = new FormulaCount(0, 1);
					Data d = new Data(trueCount, falseCount, a.getValue());
					this.get(a).put(formula, d);
					out.add(d);
				}

				return out;
			}

			Formula f = formula.copy();
			Set<Variable> variablesSet = f.getVariables();
			ListPointer<Formula> pA = this.getPointer(f);
			Atom at = (Atom) pA.get();
			List<Variable> atomVariables = new ArrayList<Variable>(at.getVariables());
			variablesSet.removeAll(atomVariables);
			List<Variable> variables = new ArrayList<Variable>(variablesSet);
			Sampler<Constant> sampler = getSampler(variables);
			sampler.setMaxSamples(Settings.formulaCountMaxSamples);
			int i = 0;

			for (Atom a : atoms) {

				Formula grounded;

				if (variables.isEmpty()) {              
					grounded = f;
				} else {
					List<Constant> constants = new ArrayList<Constant>(a.terms.length);
					for (Term t : at.terms) {
						constants.add((Constant) t);
					}
					grounded = f.replaceVariables(atomVariables, constants);
				}

				pA.set(Atom.TRUE);
				FormulaCount trueCount = grounded.trueCounts(variables, sampler);
				pA.set(Atom.FALSE);
				FormulaCount falseCount = grounded.trueCounts(variables, sampler);
				//tc[i] = trueCount.counts;
				//fc[i] = falseCount.counts; // TODO: Acho que soh precisa guardar a proporcao, ver!!
				double value = a.value*trueCount.counts + (1.0-a.value)*falseCount.counts;
				i++; 
				Data d = new Data(trueCount, falseCount, value);
				// TODO: o que acontece quando tc/fc = FormulaCount(0,0)?
				// TODO: ver se precisa do totalCount (se nao usar double para Data).
				this.get(a).put(formula, d);
				out.add(d);
			}

			return out;
		}


		public void addFormula(Formula formula) {
			ConvergenceTester tester = ConvergenceTester.lowPrecisionConvergence();


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
					Atom a = iterator.next().get(0);
					atoms.add(a);
					Map<Formula, Data> map = new HashMap<Formula, Data>(2*this.formulas.size());
					this.put(a, map);
					Data d = this.addAtoms(formula, Collections.singletonList(a)).get(0);
					tester.increment(d.value);
				}
				for (Formula f : this.formulas) {
					this.addAtoms(f, atoms);
				}
			}
			formulas.add(formula);
		}

		public boolean removeFormula(Formula f) {
			if(this.formulas.remove(f)) {
				for (Map<Formula, Data> map : this.values()) {
					map.remove(f);
				}
				return true;
			}
			return false;
		}

		//		public DataCount(long totalGrounds, int sampledGrounds, Set<Formula> formulas) {
		//			super();
		//			this.totalGrounds = totalGrounds;
		//			this.sampledGrounds = sampledGrounds;
		//			this.formulas = formulas;
		//			this.atoms = new ArrayList<Atom>();
		//		}

		//		@SuppressWarnings("unused")
		//		public static DataCount copy(DataCount old) {
		//			DataCount newDC = new DataCount(old.totalGrounds, old.sampledGrounds,
		//					new HashSet<Formula>(old.formulas));
		//			for (Atom a : old.keySet()) {
		//				newDC.put(a, new HashMap<Formula, Data>(old.get(a)));
		//			}
		//			return newDC;
		//		}

	}


}
