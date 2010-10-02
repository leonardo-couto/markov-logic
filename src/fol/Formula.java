package fol;

import java.util.ArrayList;
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
public class Formula implements Comparable<Formula> {

	private List<Atom> atoms;
	private final List<Operator> operators;
	private final List<Boolean> stack;
	private final Set<Predicate> predicates;
	
	private Formula(List<Atom> atoms, List<Operator> operators, 
			List<Boolean> stack, Set<Predicate> predicates) {
		this.atoms = atoms;
		this.operators = operators;
		this.stack = stack;
		this.predicates = predicates;
	}

	protected Formula() {
		this.atoms = null;
		this.operators = null;
		this.stack = null;
		this.predicates = null;
	}

	/**
	 * @param formulas
	 */
	public Formula(List<Atom> atoms, List<Operator> operators, List<Boolean> stack) {
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
		//Operator lastOperator = null; // TODO: passar para o toString do operador
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
	public Formula replaceVariables(List<Variable> x, List<Constant> c) {
		List<Atom> newAtoms = replaceAtomVariables(this.atoms, x, c);
		List<Operator> newOperators = new ArrayList<Operator>(this.operators);
		List<Boolean> newStack = new ArrayList<Boolean>(this.stack);
		return new Formula(newAtoms, newOperators, newStack);
	}

	/**
	 * @return A copy of this Formula.
	 */
	public Formula copy() {
		List<Atom> atomsCopy = new LinkedList<Atom>(this.atoms);
		List<Operator> operatorsCopy = new LinkedList<Operator>(this.operators);
		List<Boolean> stackCopy = new LinkedList<Boolean>(this.stack);
		return new Formula(atomsCopy, operatorsCopy, stackCopy);
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
	 * @return A List<Boolean> that represents this Formula Stack.
	 */
	public List<Boolean> getStack() {
		return this.stack;
	}

	/**
	 * @return A List<Operator> with all operators that belong to this Formula.
	 */
	public List<Operator> getOperators() {
		return this.operators;
	}

	
	/**
	 * Search the Formula for the first occurrence of an Atom of
	 * Predicate p with all Terms as Variables.
	 * 
	 * @return A ArrayPointer that points to the Atom found.
	 * Or null if the Atom was not found.
	 */
	public ListPointer<Atom> getAtomPointer(Predicate p) {
		// TODO: override no ATOM?
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
		// TODO: override no ATOM?

		if (variables.isEmpty()) {
			// Formula is grounded
			double d = this.getValue();
			if (Double.isNaN(d)) {
				return 0;
			} else {
				return d;
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
		int i = this.atoms.size();
		for (Atom a : this.atoms) {
			if (a.predicate.equals(Predicate.equals)) {
				i--;
			}
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
	
	
	public static Formula oneArityOp(Operator op, Formula f) {
		List<Operator> newop = new ArrayList<Operator>(f.operators.size() +1);
		List<Boolean> newstack = new ArrayList<Boolean>(f.stack.size()+1);
		List<Atom> newAtoms = new ArrayList<Atom>(f.atoms);
		newop.addAll(f.operators);
		newop.add(op);
		newstack.addAll(f.stack);
		newstack.add(false);
		return new Formula(newAtoms, newop, newstack, f.getPredicates());
	}

	public static Formula twoArityOp(Operator op, Formula f0, Formula f1) {
		List<Boolean> newStack = new ArrayList<Boolean>(f0.stack.size() + f1.stack.size() +1);
		newStack.addAll(f0.stack);
		newStack.addAll(f1.stack);
		newStack.add(false);
		List<Atom> newAtoms = new ArrayList<Atom>(f0.atoms.size() + f1.atoms.size());
		newAtoms.addAll(f0.atoms);
		newAtoms.addAll(f1.atoms);
		List<Operator> newOp = new ArrayList<Operator>(f0.operators.size() + f1.operators.size() +1);
		newOp.addAll(f0.operators);
		newOp.addAll(f1.operators);
		newOp.add(op);
		Set<Predicate> newPredicates = new HashSet<Predicate>(f0.predicates);
		newPredicates.addAll(f1.predicates);		
		return new Formula(newAtoms, newOp, newStack, newPredicates);
	}
	
	public static Formula nArityOp(Operator op, Formula ... formulas) {
		int stackSize = 1;
		int atomsSize = 0;
		int opSize = 1;
		for (Formula f : formulas) {
			stackSize += f.stack.size();
			atomsSize += f.atoms.size();
			opSize += f.operators.size();
		}
		List<Boolean> newStack = new ArrayList<Boolean>(stackSize);
		List<Atom> newAtoms = new ArrayList<Atom>(atomsSize);
		List<Operator> newOp = new ArrayList<Operator>(opSize);
		Set<Predicate> newPredicates = new HashSet<Predicate>();
		for (Formula f : formulas) {
			newStack.addAll(f.stack);
			newAtoms.addAll(f.atoms);
			newOp.addAll(f.operators);
			newPredicates.addAll(f.predicates);
		}
		newStack.add(false);
		newOp.add(op);
		return new Formula(newAtoms, newOp, newStack, newPredicates);
	}

}
