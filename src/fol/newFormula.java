package fol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import main.Settings;
import stat.ConvergenceTester;
import stat.Sampler;
import util.ListPointer;

/**
 * @author Leonardo Castilho Couto
 *
 */
public class newFormula implements Comparable<newFormula> {
  
  private List<Atom> atoms;
  private final List<Operator> operators;
  private final List<Boolean> stack;
	private final Set<Predicate> predicates;

	/**
	 * @param formulas
	 */
	public newFormula(List<Atom> atoms, List<Operator> operators, List<Boolean> stack) {
		this.atoms = atoms;
		this.operators = operators;
		this.stack = stack;
		this.predicates = new HashSet<Predicate>();
		for(Atom a : atoms) {
		  this.predicates.add(a.predicate);
		}
	}
	
	public Set<Predicate> getPredicates() {
		return new HashSet<Predicate>(this.predicates);
	}
	
	public double getValue() {
	  Stack<Double> values = new Stack<Double>();
	  Iterator<Atom> atom = this.atoms.iterator();
	  Iterator<Operator> operator = this.operators.iterator();
	  int operatorIndex = 0;
	  for (Boolean isAtom : stack) {
	    if(isAtom) {
	      values.push(atom.next().getValue());
	    } else { // operator
	      Operator op = operator.next();
	      double[] args = new double[op.getArity()];
	      for (int i = 0; i < op.getArity(); i++) {
	        args[i] = values.pop();
	      }
	      values.push(op.value(args));
	    }
	  }
	  
	  if (values.size() != 1) {
	    throw new RuntimeException("Malformed Formula: " + this.toPostfixString());
	  }
	  return values.pop();
	}
	
	@Override
	public String toString() {
    Stack<String> values = new Stack<String>();
    Iterator<Atom> atom = this.atoms.iterator();
    Iterator<Operator> operator = this.operators.iterator();
    for (Boolean isAtom : stack) {
      if (isAtom) {
        values.push(atom.next().toString());
      } else {
        Operator op = operator.next();
        String[] args = new String[op.getArity()];
        for (int i = 0; i < op.getArity(); i++) {
          args[i] = values.pop();
        }
        values.push("( " + op.toString(args) + " )");
      }
    }
    if (values.size() != 1) {
      throw new RuntimeException("Malformed Formula: " + this.toPostfixString());
    }
    return values.pop();
	}
	
	public String toPostfixString() {
	  StringBuffer sb = new StringBuffer();
    Iterator<Atom> atom = this.atoms.iterator();
    Iterator<Operator> operator = this.operators.iterator();
	  for (Boolean isAtom : stack) {
	    if (isAtom) {
	      sb.append(atom.next());
	    } else {
	      sb.append(operator.next());
	    }
	  }
	  return sb.toString();
	}
	
	public boolean hasPredicate(Predicate p) {
		return this.predicates.contains(p);
	}
	
	private static List<Atom> replaceAtomVariables(List<Atom> atoms, List<Variable> x, List<Constant> c) {
    List<Atom> newAtoms = new ArrayList<Atom>(atoms.size());
    for (Atom a : atoms) {
      newAtoms.add(a.replaceVariables(x, c));
    }
    return newAtoms;
	}
	
	/**
	 * Replaces all occurrences of Variable X[i] by the Constant c[i].
	 */
	public newFormula replaceVariables(List<Variable> x, List<Constant> c) {
	  List<Atom> newAtoms = replaceAtomVariables(this.atoms, x, c);
	  List<Operator> newOperators = new ArrayList<Operator>(this.operators);
	  List<Boolean> newStack = new ArrayList<Boolean>(this.stack);
	  return new newFormula(newAtoms, newOperators, newStack);
	}
	
	/**
	 * @return A copy of this Formula.
	 */
	public newFormula copy() {
	  List<Atom> atomsCopy = new LinkedList<Atom>(this.atoms);
	  List<Operator> operatorsCopy = new LinkedList<Operator>(this.operators);
	  List<Boolean> stackCopy = new LinkedList<Boolean>(this.stack);
	  return new newFormula(atomsCopy, operatorsCopy, stackCopy);
	}
	
	public Set<Variable> getVariables() {
		Set<Variable> set = new HashSet<Variable>();
		for(Atom a : this.atoms) {
			set.addAll(a.getVariables());
		}
		return set;
	}

	/**
	 * @return A List<Atom> with all Atoms that belong to this Formula.
	 */
	public List<Atom> getAtoms() {
		return this.atoms;
	}
	
	/**
	 * Search the Formula for the first occurrence of an Atom of
	 * Predicate p with all Terms as Variables.
	 * 
	 * @return A ArrayPointer that points to the Atom found.
	 * Or null if the Atom was not found.
	 */
	public ListPointer<Atom> getAtomPointer(Predicate p) {
	  for (int i = 0; i < this.atoms.size(); i++) {
	    Atom a = this.atoms.get(i);
	    if (a.predicate.equals(p)) {
	      if (a.variablesOnly()) {
	        return new ListPointer<Atom>(this.atoms, i);
	      }
	    }
	  }
		return null;		
	}
	
	public double trueCounts(List<Variable> variables, Sampler<Constant> sampler) {
		
		if (variables.isEmpty()) {
			// Formula is grounded
			double d = this.getValue();
			if (Double.isNaN(d)) {
				return 0;
			} else {
				return 0;
			}
		}
		
		List<Atom> original = this.atoms;
		ConvergenceTester tester = ConvergenceTester.lowPrecisionConvergence();
		
		for (List<Constant> arg : sampler) {
		  this.atoms = replaceAtomVariables(original, variables, arg);
		  double d = this.getValue();
		  if (!Double.isNaN(d)) {
		    if(tester.increment(d)) {
		      break;
		    }
		  }
		}
		this.atoms = original;
	
		if (!tester.hasConverged()) {
			// TODO: tirar, ou colocar num log
			System.out.println("nao convergiu");
			return 0;
		}

		return sampler.n*tester.mean();
		
	}
	
	// TODO: Testar!!!!!!!!!!!!!!!!!
	public double trueCounts() {
		List<Variable> variables = new ArrayList<Variable>(this.getVariables());
		
		if (variables.isEmpty()) {
			// Formula is grounded
			double d = this.getValue();
			if (Double.isNaN(d)) {
				return 0;
			} else {
				return d;
			}
		}
		
		List<Set<Constant>> constants = new ArrayList<Set<Constant>>(variables.size());
		for (Variable v : variables) {
		  constants.add(v.getConstants());
		}

		Sampler<Constant> sampler = new Sampler<Constant>(constants);
		sampler.setMaxSamples(Settings.formulaCountMaxSamples);
		
		return this.trueCounts(variables, sampler);

	}
	
	/**
	 * The number of Atoms in a formula
	 * Does not consider Atoms of Predicate Predicate.equals
	 */
	public int length() {
		int i = 0;
		for (newFormula f : formulas) {
			i = i + f.length();
		}
		return i;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(newFormula o) {
		return this.toString().compareTo(o.toString());
	}
	
}
