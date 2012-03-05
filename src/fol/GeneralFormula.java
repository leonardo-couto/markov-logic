package fol;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import fol.database.Database;
import fol.operator.Biconditional;
import fol.operator.Conjunction;
import fol.operator.Disjunction;
import fol.operator.Negation;

public class GeneralFormula implements Formula {
	
	private List<Atom> atoms; // lazy instantiated
	
	private final List<FormulaComponent> components;
	
	public GeneralFormula(List<FormulaComponent> components) {
		this.components = new ArrayList<FormulaComponent>(components);
		this.atoms = null;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GeneralFormula other = (GeneralFormula) obj;
		return this.components.equals(other.components);
	}

	@Override
	public List<Atom> getAtoms() {
		return new ArrayList<Atom>(this._getAtoms());
	}
	
	private List<Atom> _getAtoms() {
		if (this.atoms != null) return this.atoms;
		
		List<Atom> atoms = new ArrayList<Atom>(this.components.size());
		for (FormulaComponent component : this.components) {
			if (component instanceof Atom) {
				atoms.add((Atom) component);

			} else if (component instanceof Literal) {
				atoms.add(((Literal) component).atom);				
			}
		}
		this.atoms = atoms;			
		return this.atoms;
	}
	
	/**
	 * Get this Formula components, a Postfix ordered Array
	 * of Atoms and Operators that fully represent this Formula
	 * 
	 * @return List of components
	 */
	@Override
	public List<FormulaComponent> getComponents() {
		return new ArrayList<FormulaComponent>(this.components);
	}

	@Override
	public boolean getValue(Database db) {
		Deque<Boolean> stack = new LinkedList<Boolean>();
		for (FormulaComponent component : this.components) {
			component.evaluate(stack, db);
		}
		return stack.pop().booleanValue();
	}

	@Override
	public boolean getValue(Database db, Map<Variable, Constant> groundings) {
		Deque<Boolean> stack = new LinkedList<Boolean>();
		
		for (FormulaComponent component : this.components) {
			if (component instanceof Atom) {
				Atom atom = (Atom) component;
				component = atom.ground(groundings);
				
			} else if (component instanceof Literal) {
				Literal literal = (Literal) component;
				component = literal.ground(groundings);
				
			}
			
			component.evaluate(stack, db);
		}
		
		return stack.pop().booleanValue();
	}

	@Override
	public Set<Predicate> getPredicates() {
		Set<Predicate> predicates = new HashSet<Predicate>();
		for (Atom a : this._getAtoms()) {
			predicates.add(a.predicate);
		}
		return predicates;
	}

	@Override
	public Set<Variable> getVariables() {
		Set<Variable> variables = new HashSet<Variable>();
		for (Atom a : this._getAtoms()) {
			variables.addAll(a.getVariables());
		}
		return variables;
	}

	@Override
	public int hashCode() {
		return this.components.hashCode();
	}

	@Override
	public boolean hasPredicate(Predicate p) {
		for (Atom a : this._getAtoms()) {
			if (p == a.predicate) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isGrounded() {
		for (Atom a : this._getAtoms()) {
			if (!a.isGrounded()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int length() {
		return this._getAtoms().size();
	}

	@Override
	public GeneralFormula ground(Map<Variable, Constant> groundings) {
		int size = this.components.size();
		List<FormulaComponent> groundedComponents = new ArrayList<FormulaComponent>(size);
		
		for (FormulaComponent component : this.components) {
			if (component instanceof Atom) {
				Atom atom = (Atom) component;
				component = atom.ground(groundings);
				
			} else if (component instanceof Literal) {
				Literal literal = (Literal) component;
				component = literal.ground(groundings);
				
			}
			
			groundedComponents.add(component);
		}

		return new GeneralFormula(groundedComponents);
	}
	
	@Override
	public GeneralFormula replace(Atom original, Literal replacement) {
		
		List<FormulaComponent> components = new ArrayList<FormulaComponent>(this.components.size());

		for (FormulaComponent component : this.components) {
			if (component instanceof Atom) {
				Atom atom = (Atom) component;
				if (original.equals(atom)) {
					component = replacement;
				}
				
			} else if (component instanceof Literal) {
				Literal literal = (Literal) component;
				component = literal.replace(original, replacement);				
			}
			
			components.add(component);
		}
		
		return new GeneralFormula(components);
	}
	
	@Override
	public CNF toCNF() {
		List<FormulaComponent> components = this.replaceBiconditional(this.components); 
		List<FormulaComponent> nnf = toNegationNormalForm(components);
		Deque<CNF> formulas = new LinkedList<CNF>();
		
		for (int i = 0; i < nnf.size(); i++) {
			FormulaComponent component = nnf.get(i);

			if (component instanceof Literal) {
				Literal literal = (Literal) component;
				formulas.add(literal.toCNF());

			} else if (component == Conjunction.OPERATOR) {
				List<Clause> clauses = new ArrayList<Clause>();
				clauses.addAll(formulas.pollLast().getClauses());
				clauses.addAll(formulas.pollLast().getClauses());
				CNF cnf = new CNF(clauses);
				formulas.offerLast(cnf);

			} else if (component == Disjunction.OPERATOR) {
				CNF cnf1 = formulas.pollLast();
				CNF cnf2 = formulas.pollLast();
				CNF disjunction = this.applyDisjunction(cnf1, cnf2);
				formulas.add(disjunction);

			} else {
				String message = String.format("Don't know how to handle %s", component);
				throw new UnsupportedOperationException(message);
			}
		}

		if (formulas.size() != 1) { 
			String message = String.format("Malformed Formula: %s", this.components);
			throw new RuntimeException(message);
		}
		
		return formulas.getFirst();
	}
	
	private CNF applyDisjunction(CNF cnf1, CNF cnf2) {
		List<Clause> clauses1 = cnf1.getClauses();
		List<Clause> clauses2 = cnf2.getClauses();
		
		int size1 = clauses1.size();
		int size2 = clauses2.size();
		
		List<Clause> join = new ArrayList<Clause>(size1 * size2);
		for (int i = 0; i < size1; i++) {
			List<Literal> literals = clauses1.get(i).getLiterals();
			for (int j = 0; j < size2; j++) {
				Clause clause = clauses2.get(j);
				for (Literal l : literals) {
					clause = clause.addLiteral(l);
				}
				join.add(clause);
			}
		}
		
		return new CNF(join);
	}
	
	/**
	 * Replaces the Biconditional operators for the equivalent formula with Conjunctions,
	 * Disjunctions and Negations.
	 */
	private List<FormulaComponent> replaceBiconditional(List<FormulaComponent> components) {
		if (!components.contains(Biconditional.OPERATOR)) return components;
		
		Deque<List<FormulaComponent>> factors = new LinkedList<List<FormulaComponent>>();
		
		for (FormulaComponent component : components) {
			if (component instanceof Atom || component instanceof Literal) {
				List<FormulaComponent> singleton = new ArrayList<FormulaComponent>();
				singleton.add(component);
				factors.offerLast(singleton);
				
			} else if (component == Biconditional.OPERATOR) {
				List<FormulaComponent> f1 = factors.pollLast();
				List<FormulaComponent> f2 = factors.pollLast();			
				factors.add(this.applyBiconditional(f1, f2));				
				
			} else if (component == Negation.OPERATOR) {
				factors.peekLast().add(component);
				
			} else { // Conjunction of Disjunction
				if (component != Conjunction.OPERATOR && component != Disjunction.OPERATOR) {
					String message = String.format("Don't know how to handle %s", component);
					throw new UnsupportedOperationException(message);
				}
				
				List<FormulaComponent> f1 = factors.pollLast();
				f1.add(component);
				factors.peekLast().addAll(f1);				
			}
		}
		
		if (factors.size() != 1) { 
			String message = String.format("Malformed Formula: %s", this.components);
			throw new RuntimeException(message);
		}
		
		return factors.getFirst();
	}
	
	/**
	 * Applies the Biconditional operator in the extended form.
	 * @param f1 
	 * @param f2 
	 * @return (f1 ^ f2) v (!f1 ^ !f2)
	 */
	private List<FormulaComponent> applyBiconditional(List<FormulaComponent> f1, List<FormulaComponent> f2) {
		List<FormulaComponent> nf1 = new ArrayList<FormulaComponent>(f1);
		List<FormulaComponent> nf2 = new ArrayList<FormulaComponent>(f2);
		nf1.add(Negation.OPERATOR);
		nf2.add(Negation.OPERATOR);
		nf1 = toNegationNormalForm(nf1);
		nf2 = toNegationNormalForm(nf2);
		
		List<FormulaComponent> biconditional = new ArrayList<FormulaComponent>();
		biconditional.addAll(f1);
		biconditional.addAll(f2);
		biconditional.add(Conjunction.OPERATOR);
		biconditional.addAll(nf1);
		biconditional.addAll(nf2);
		biconditional.add(Conjunction.OPERATOR);
		biconditional.add(Disjunction.OPERATOR);	
		
		return biconditional;
	}
	
	public GeneralFormula toNegationNormalForm() {
		List<FormulaComponent> components = this.replaceBiconditional(this.components); 
		List<FormulaComponent> nnfComponents = toNegationNormalForm(components);
		return new GeneralFormula(nnfComponents);
	}
	
	private static List<FormulaComponent> toNegationNormalForm(List<FormulaComponent> components) {
		LinkedList<Integer> negate = new LinkedList<Integer>();
		Deque<FormulaComponent> nnf = new LinkedList<FormulaComponent>();
		boolean deMorgan, neg;
		FormulaComponent component;
		
		for (int i = components.size() -1; i >= 0; i--) {
			deMorgan = false;
			neg = (!negate.isEmpty() && negate.peek().intValue() == i);
			component = components.get(i);
			
			if (neg) {
				negate.pop();
				if (component == Negation.OPERATOR) {
					subtract(negate, 1);
					
				} else if (component == Conjunction.OPERATOR) {
					deMorgan = true;
					nnf.push(Disjunction.OPERATOR);
					
				} else if (component == Disjunction.OPERATOR) {
					deMorgan = true;
					nnf.push(Conjunction.OPERATOR);
					
				} else if (component == Biconditional.OPERATOR) {
					nnf.push(component);
					subtract(negate, 2);
					negate.push(Integer.valueOf(i-1));
					
				} else if (component instanceof Atom){
					Atom atom = (Atom) component;
					nnf.push(new Literal(atom, false));
					
				} else if (component instanceof Literal){
					Literal literal = (Literal) component;
					nnf.push(new Literal(literal.atom, !literal.signal));
					
				} else {
					String message = String.format("Don't know how to handle %s", component);
					throw new UnsupportedOperationException(message);
				}
				
				if (deMorgan) {
					subtract(negate, 2);
					negate.push(Integer.valueOf(i-2));
					negate.push(Integer.valueOf(i-1));
				}
				
			} else if (component instanceof Atom) {
				nnf.push(new Literal((Atom) component, true));
			} else if (component instanceof Literal) {
				nnf.push(component);
			} else if (component == Negation.OPERATOR) {
				negate.push(Integer.valueOf(i-1));
			} else { // disjunction, conjunction or biconditional
				nnf.push(component);
				subtract(negate, 2);
			}
		}
		
		return new ArrayList<FormulaComponent>(nnf);
	}
	
	@Override
	public String toString() {
		Deque<StringBuilder> stack = new LinkedList<StringBuilder>();
		for (FormulaComponent component : this.components) {
			component.print(stack);
		}
		boolean endInNeg = this.components.get(this.components.size()-1) == Negation.OPERATOR;
		StringBuilder formula = stack.pop();
		return endInNeg ? formula.toString() : formula.substring(1,formula.length()-1);
	}
	
	/**
	 * Subtract each member of list by value
	 * @param list
	 * @param value 
	 */
	private static void subtract(List<Integer> list, int value) {
		ListIterator<Integer> iterator = list.listIterator();
		int current;
		while (iterator.hasNext()) {
			current = iterator.next().intValue();
			iterator.set(Integer.valueOf(current-value));
		}
	}

	@Override
	public double trueCount(Database db) {
		Set<Variable> vars = this.getVariables();
		Map<Variable, Constant> groundings = new HashMap<Variable, Constant>();
		int total = 1;
		for (Variable v : vars) {
			total = total * v.getConstants().size();
		}
		int sample = total < 700 ? total : 700; // 500 for a error of at most 5%
		// TODO tirar o 2* daqui e do SimpleDB, fazer as amostras serem exatas para < 100
		
		int count = 0;
		for (int i = 0; i < sample; i++) {
			for (Variable v : vars) {
				groundings.put(v, v.getRandomConstant());
			}
			if (this.getValue(db, groundings)) {
				count++;
			}
		}
		
		double ratio = ((double) count) / sample;
		return ratio * total;
	}
	
}
