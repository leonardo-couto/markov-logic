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

import stat.convergence.DummyTester;
import stat.convergence.SequentialTester;
import stat.sampling.DefaultSampler;
import stat.sampling.Sampler;
import util.ListPointer;
import fol.operator.Operator;

/**
 * @author Leonardo Castilho Couto
 *
 */
public class Formula implements Comparable<Formula> {

	private List<Atom> atoms;
	private final List<Operator> operators;
	private final List<Boolean> stack; // TODO: usar ArrayStack (apache commons collection)
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
			if (a.predicate != Predicate.empty) {
				this.predicates.add(a.predicate);
			}
		}
	}

	public Set<Predicate> getPredicates() {
		return new HashSet<Predicate>(this.predicates);
	}

	public double getValue() {
		Stack<Double> values = new Stack<Double>();
		Iterator<Atom> atom = this.atoms.iterator();
		Iterator<Operator> operator = this.operators.iterator();
		for (Boolean isAtom : this.stack) {
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
				values.push(op.toString(args));
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
			sb.append(" ");
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
	 * Replaces all occurrences of Variable X[i] by the Constant c[i]
	 * @return a COPY of this formula with all variables in X replaced
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

	/**
	 * @return a new Set containing all variables in this formula
	 */
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

	private double trueCounts(List<Variable> variables, Sampler<Constant> sampler, SequentialTester tester, long cardinality) {
		
		if (tester.hasConverged()) {
			return 0;
		}
		
		List<Atom> original = this.atoms;
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
		
		double mean = tester.mean();
		tester.clear();
		return Double.isNaN(mean) ? 0.0d : cardinality * mean;
	}

	public double trueCounts() {
		return this.trueCounts(new DummyTester(-1));
	}
	
	public double trueCounts(SequentialTester tester) {
		// TODO: override no ATOM?
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

		DefaultSampler<Constant> sampler = new DefaultSampler<Constant>(constants);
				
		return this.trueCounts(variables, sampler, tester, sampler.getCardinality());
	}
	
	/**
	 * Returns the number of Atoms in this formula.
	 * 
	 * @return the number of Atoms in this formula
	 */
	public int length() {
		int i = this.atoms.size();
		for (Atom a : this.atoms) {
			if (a.predicate == Predicate.empty) {
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
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Formula) {
			Formula f = (Formula) obj;
			return this.toString().equals(f.toString());
		}
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	
	/**
	 * Apply a one arity operator to the formula f
	 * @param op one arity operator
	 * @param f formula where the opperator shall be applied
	 * @return formula with the applied operator
	 */
	public static Formula oneArityOp(Operator op, Formula f) {
		if (f instanceof Atom) {
			return new Formula(Collections.singletonList((Atom) f),
					Collections.singletonList(op),
					Arrays.asList(new Boolean[] {true, false}),
					Collections.singleton(((Atom) f).predicate));
		}
		List<Operator> newop = new ArrayList<Operator>(f.operators.size() +1);
		List<Boolean> newstack = new ArrayList<Boolean>(f.stack.size()+1);
		List<Atom> newAtoms = new ArrayList<Atom>(f.atoms);
		newop.addAll(f.operators);
		newop.add(op);
		newstack.addAll(f.stack);
		newstack.add(false);
		return new Formula(newAtoms, newop, newstack, f.getPredicates());
	}

	// TODO: FAZER BENCHMARK, VER SE A DIFERENCA ENTRE CHAMAR O TWOARITY
	// E O nARITY É SIGINIFICANTE, VER SE NAO TERA NENHUM BUG, CASO
	// NEGATIVO, CHAMAR O nARITY A PARTIR DESTE MÉTODO!!!!!!!
	public static Formula twoArityOp(Operator op, Formula f0, Formula f1) {
		boolean isAtomf0 = f0 instanceof Atom;
		boolean isAtomf1 = f1 instanceof Atom;
		List<Boolean> f0stack = f0.stack;
		List<Boolean> f1stack = f1.stack;
		List<Atom> f0atoms = f0.atoms;
		List<Atom> f1atoms = f1.atoms;
		List<Operator> f0operators = f0.operators;
		List<Operator> f1operators = f1.operators;
		Set<Predicate> f0predicates = f0.predicates;
		Set<Predicate> f1predicates = f1.predicates;
		if (isAtomf0) {
			Atom a = (Atom) f0;
			f0stack = Collections.singletonList(true);
			f0atoms = Collections.singletonList(a);
			f0operators = Collections.emptyList();
			f0predicates = Collections.singleton(a.predicate);
		}
		if (isAtomf1) {
			Atom a = (Atom) f1;
			f1stack = Collections.singletonList(true);
			f1atoms = Collections.singletonList(a);
			f1operators = Collections.emptyList();
			f1predicates = Collections.singleton(a.predicate);
		}
		List<Boolean> newStack = new ArrayList<Boolean>(f0stack.size() + f1stack.size() +1);
		newStack.addAll(f0stack);
		newStack.addAll(f1stack);
		newStack.add(false);
		List<Atom> newAtoms = new ArrayList<Atom>(f0atoms.size() + f1atoms.size());
		newAtoms.addAll(f0atoms);
		newAtoms.addAll(f1atoms);
		List<Operator> newOp = new ArrayList<Operator>(f0operators.size() + f1operators.size() +1);
		newOp.addAll(f0operators);
		newOp.addAll(f1operators);
		newOp.add(op);
		Set<Predicate> newPredicates = new HashSet<Predicate>(f0predicates);
		newPredicates.addAll(f1predicates);		
		return new Formula(newAtoms, newOp, newStack, newPredicates);
	}
	
	public static Formula nArityOp(Operator op, Formula ... formulas) {
		int stackSize = 1;
		int atomsSize = 0;
		int opSize = 1;
		boolean[] atom = new boolean[formulas.length];
		for (int i = 0; i < formulas.length; i++) {
			Formula f = formulas[i];
			if (f instanceof Atom) {
				atomsSize += 1;
				atom[i] = true;
			} else {
				stackSize += f.stack.size();
				atomsSize += f.atoms.size();
				opSize += f.operators.size();
			}
		}
		List<Boolean> newStack = new ArrayList<Boolean>(stackSize);
		List<Atom> newAtoms = new ArrayList<Atom>(atomsSize);
		List<Operator> newOp = new ArrayList<Operator>(opSize);
		Set<Predicate> newPredicates = new HashSet<Predicate>();
		for (int i = 0; i < formulas.length; i++) {
			Formula f = formulas[i];
			if (atom[i]) {
				Atom a = (Atom) f;
				newStack.add(true);
				newAtoms.add(a);
				newPredicates.add(a.predicate);
			} else {
				newStack.addAll(f.stack);
				newAtoms.addAll(f.atoms);
				newOp.addAll(f.operators);
				newPredicates.addAll(f.predicates);
			}
		}
		newStack.add(false);
		newOp.add(op);
		return new Formula(newAtoms, newOp, newStack, newPredicates);
	}

}
