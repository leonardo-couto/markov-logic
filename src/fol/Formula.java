package fol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import stat.ConvergenceTester;
import stat.Sampler;
import util.ListPointer;
import weightLearner.FormulaCount;

/**
 * @author Leonardo Castilho Couto
 *
 */
public abstract class Formula implements Comparable<Formula> {
	
	protected final List<Formula> formulas;
	private final Set<Predicate> predicates;

	/**
	 * @param formulas
	 */
	public Formula() {
		this.formulas = Collections.emptyList();
		this.predicates = Collections.emptySet();
	}	
	
	/**
	 * @param formulas
	 */
	public Formula(Formula ... formulas) {
		this.formulas = colapse(Arrays.asList(formulas));
		this.predicates = new HashSet<Predicate>();
		for (Formula f : this.formulas) {
			this.predicates.addAll(f.predicates);
		}
	}
	
	/**
	 * @param formulas
	 */
	public Formula(List<Formula> formulas) {
		this.formulas = colapse(formulas);
		this.predicates = new HashSet<Predicate>();
		for (Formula f : this.formulas) {
			this.predicates.addAll(f.predicates);
		}
	}
	
	/**
	 * @param formulas
	 */
	public Formula(Formula formula) {
		this.formulas = Collections.singletonList(formula);
		this.predicates = formula.predicates;
	}
	
	public Set<Predicate> getPredicates() {
		return new HashSet<Predicate>(this.predicates);
	}
	
	public abstract double getValue();
	
	protected String print() {
		// Aux from toString(). The same as toString, but enclosed with parenthesis.
		return "( " + toString() + " )";
	}
	
	public String toString() {
		boolean first = true;
		String s = "";
		for (Formula f : formulas) {
			if (first) {
				s = f.print();
				first = false;
				continue;
			}
			s = s + " " + operator() + " " + f.print();
		}
		return s;
	}
	
	// colapses and sort Formulas: i.e: Given f0 = f1 v f2, f1 = f3 v f4;
	// a f0.colapse() change f0 to f0 = f2 v f3 v f4.
	// if n0 = !n1, and n1 = !n2, n0.colapses -> n0 = n2
	protected abstract List<Formula> colapse(List<Formula> fa);
	
	protected abstract String operator();

	public boolean hasPredicate(Predicate p) {
		for (Formula f : this.formulas) {
			if (f.hasPredicate(p)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Replaces Variable X[i] by the Constant c[i].
	 */
	public Formula replaceVariables(List<Variable> X, List<Constant> c) {
		// Check domains compatibility.
		//if (X.length != c.length) {
		//	throw new IllegalArgumentException();
		//}
		//for (int i = 0; i < X.length; i++) {
		//	if(!X[i].inDomain(c[i])) {
		//		throw new IllegalArgumentException();
		//	}
		//}
		return recursiveReplaceVariable(X, c);
	}
	
	protected Formula recursiveReplaceVariable(List<Variable> X, List<Constant> c) {
		for (int i = 0; i < this.formulas.size(); i++) {
			this.formulas.set(i, this.formulas.get(i).recursiveReplaceVariable(X, c));
		}
		return this;
	}
	
	/**
	 * @return A copy of this Formula.
	 */
	public abstract Formula copy();
	
	public Set<Variable> getVariables() {
		Set<Variable> set = new HashSet<Variable>();
		for(Formula f : formulas) {
			set.addAll(f.getVariables());
		}
		return set;
	}

	/**
	 * @return A Set<Atom> with all Atoms that belong to this Formula.
	 */
	public List<ListPointer<Formula>> getAtoms() {
		List<ListPointer<Formula>> out = new ArrayList<ListPointer<Formula>>();
		for (int i = 0; i < this.formulas.size(); i++) {
			if (this.formulas.get(i) instanceof Atom) {
				out.add(new ListPointer<Formula>(formulas, i));
			} else {
				out.addAll(formulas.get(i).getAtoms());
			}
		}
		return out;		
	}
	
	/**
	 * Recursively add all Atoms that belong to this Formula.
	 * 
	 * @param Set<Atom> the Set where all the Atoms will be 
	 * recursively added.
	 */
	protected void addAtoms(Set<Atom> set) {
		for (Formula f : formulas) {
			f.addAtoms(set);
		}
	}
	
	/**
	 * Search the Formula for the first occurrence of an Atom of
	 * Predicate p with all Terms as Variables.
	 * 
	 * @return A ArrayPointer that points to the Atom found.
	 * Or null if the Atom was not found.
	 */
	public ListPointer<Formula> getAtomPointer(Predicate p) {
		ListPointer<Formula> out;
		for (int i = 0; i < formulas.size(); i++) {
			if (formulas.get(i) instanceof Atom) {
				Atom a = (Atom) formulas.get(i);
				if (p.equals(a.predicate)) {
					if (a.variablesOnly()) {
						return new ListPointer<Formula>(formulas, i);
					}
				}
			} else {
				out = formulas.get(i).getAtomPointer(p);
				if (out != null) {
					return out;
				}
			}
		}
		return null;		
	}
	
	public FormulaCount trueCount(List<Variable> variables, Sampler<Constant> sampler) {
		List<ListPointer<Formula>> apl = this.getAtoms();
		ConvergenceTester tester = ConvergenceTester.lowPrecisionConvergence();
		
		for (List<Constant> arg : sampler) {
		  this.replaceVariables(variables, arg);
		  double d = this.getValue();
		  if (!Double.isNaN(d)) {
		    if(tester.increment(d)) {
		      break;
		    }
		  }
		  for (ListPointer<Formula> pointer : apl) {
		    pointer.set(pointer.original);
		  }
		}
		
		if(tester.hasConverged()) {
		  
		}
		
		return null;

	}
	
	// TODO: Testar!!!!!!!!!!!!!!!!!
	// TODO: da para chamar varias vezes com o mesmo Sampler para economizar!
	// mas ficaria um pouco feio. Ver o que fazer.
	public FormulaCount trueCounts() {
		List<Variable> variables = new ArrayList<Variable>(this.getVariables());
		
		if (variables.isEmpty()) {
			// Formula is grounded
			double d = this.getValue();
			if (Double.isNaN(d)) {
				return new FormulaCount(0,0);
			} else {
				return new FormulaCount(d, 1);
			}
		}
		
		List<Set<Constant>> constants = new ArrayList<Set<Constant>>(variables.size());
		for (Variable v : variables) {
		  constants.add(v.getConstants());
		}

		Sampler<Constant> sampler = new Sampler<Constant>(constants);
		
		return this.trueCount(variables, sampler);

	}
	
	/**
	 * The number of Atoms in a formula
	 * Does not consider Atoms of Predicate Predicate.equals
	 */
	public int length() {
		int i = 0;
		for (Formula f : formulas) {
			i = i + f.length();
		}
		return i;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Formula o) {
		return this.toString().compareTo(o.toString());
	}
	
}
