package weightLearner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import stat.ConvergenceMethodTester;
import stat.ConvergenceTester;
import stat.Sampler;
import util.Convergent;
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
			// TODO: update counts de p
		}
	}

	/* (non-Javadoc)
	 * @see weightLearner.AbstractScore#addFormulas(java.util.List)
	 */
	@Override
	public void addFormulas(List<Formula> formulas) {
		super.addFormulas(formulas);
		Set<Predicate> predicates = new HashSet<Predicate>(this.predicateFormulas.keySet().size());
		for (Formula f : formulas) {
			predicates.addAll(formulaPredicates.get(f));
		}
		for (Predicate p : predicates) {
			// TODO: update counts de p
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
	
	private void updateCounts(Predicate p) {
		
		// Use only formulas where the Predicate p appears.
		Set<Formula> pFormulas = new HashSet<Formula>(predicateFormulas.get(p));
		
		DataCount dc;
		Set<Atom> sample;
		boolean hasCounts = false;
		
		if (dataCounts.containsKey(p)) {
			// Some counts have already been done.
			hasCounts = true;
			dc = dataCounts.get(p);
			sample = dc.keySet();  // NAO VAI MAIS USAR O MESMO CARA PARA TODOS
			pFormulas.removeAll(dc.formulas);
			dc.formulas.addAll(pFormulas);
			
		} else {
			// No counts done for this predicate
			
			//sample = sampler.samplePredicate(p);
			//dc = new DataCount(p.totalGroundsNumber(), sample.size(), pFormulas);
			dc = new DataCount(p.totalGroundsNumber(), (int) p.totalGroundsNumber(), pFormulas);
			
			// Store counts for this Predicate
			dataCounts.put(p, dc);
		}
		
		if (pFormulas.isEmpty()) {
			// Predicate has no Formula counts that has not been calculated.
			return; 
		}
		
		

		
	}
	
	
	private static class Data {
		public final FormulaCount trueCount;
		public final FormulaCount falseCount;
		
		public Data(FormulaCount trueCount, FormulaCount falseCount) {
			this.trueCount = trueCount;
			this.falseCount = falseCount;
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
	
  @SuppressWarnings("unused")
	private static class DataCount extends HashMap<Atom,Map<Formula,Data>> {
		
		public final long totalGrounds;
		private int sampledGrounds;
		private final Set<Formula> formulas;
		private final List<Atom> atoms;
		public final Predicate predicate;
		private Sampler<Atom> sampler;
		
		private static final Convergent<Atom> convergent = new Convergent<Atom>() {
		  
		  private Formula f;
		  
		  public void setFormula(Formula f) {
		    this.f = f;
		  }

      @Override
      public double method(List<Atom> args) {
        // TODO Auto-generated method stub
        return 0;
      }
		  
    };
		

    public void addFormula(Formula f) {
			boolean converged = false;
			
			if (f instanceof Atom) {
			  // TODO: tratar o caso de Atom
			}
			
			f = f.copy();
			ListPointer<Formula> pA = null;
			List<ListPointer<Formula>> apl = f.getAtoms();
			for (ListPointer<Formula> pointer : apl) {
			  if (((Atom) pointer.get()).predicate.equals(this.predicate)) {
			    if (((Atom) pointer.get()).variablesOnly()) {
			      pA = pointer;
			      break;
			    }
			  }
			}
			if (pA == null) {
	      throw new RuntimeException("Formula \"" + f.toString() + 
	              "\" contains no Predicate \"" + predicate.toString() + "." + 
	              "\" with only Variables.");
			}
			Atom at = (Atom) pA.get();
			List<Variable> variables = new ArrayList<Variable>(at.terms.length);
			for (Term t : at.terms) {
			  variables.add((Variable) t);			  
			}
			
			ConvergenceTester testerA = ConvergenceTester.lowPrecisionConvergence();
			ConvergenceTester testerB = ConvergenceTester.lowPrecisionConvergence();
			
			if (!this.atoms.isEmpty()) {
			  
			  int i = 0;
        double[] tc = new double[this.atoms.size()];
        double[] fc = new double[this.atoms.size()];
			
			  for (Atom a : this.atoms) {
			    
			    List<Constant> constants = new ArrayList<Constant>(a.terms.length);
			    for (Term t : at.terms) {
			      constants.add((Constant) t);
			    }
			    
			    Formula grounded = f.replaceVariables(variables, constants);
			    
			    pA.set(Atom.TRUE);
			    FormulaCount trueCount = grounded.trueCounts();
			    pA.set(Atom.FALSE);
			    FormulaCount falseCount = grounded.trueCounts();
			    tc[i] = trueCount.trueCounts;
			    fc[i] = falseCount.trueCounts; // TODO: Acho que soh precisa guardar a proporcao, ver!!
			                                   // de qualquer maneira, mudar de trueCounts para counts
			                                   // e tirar o NaNCounts.
			    i++;			    
			  }
			  
			  testerA.evaluate(tc);
			  testerB.evaluate(fc);			
			}
		}
		
		public boolean removeFormula(Formula f) {
			// TODO: remove counts
			return false;
		}
		
		public long getTotalGrounds(Predicate p) {
		  // TODO: Ver se jah nao foi feito em outro lugar
		  // se nao, implementar!
		  return 1;
		}
		
		public DataCount(Predicate p) {
			this.predicate = p;
			this.atoms = new ArrayList<Atom>();
			this.formulas = new HashSet<Formula>();
			this.totalGrounds = getTotalGrounds(p);
//			this.formulas = new HashSet<Formula>();
//			this.totalGrounds = p.totalGroundsNumber();
//			this.sampler = new Sampler<Atom>(p.getGroundings().keySet());
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
