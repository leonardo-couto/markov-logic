package fol;

import java.util.ArrayList;
import java.util.Deque;
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
	
	private final List<Atom> atoms;
	private final List<FormulaComponent> components;
	
	public GeneralFormula(List<FormulaComponent> components) {
		this.components = components;
		this.atoms = new ArrayList<Atom>(components.size());
		this.initAtoms();
	}
	
	private GeneralFormula(List<FormulaComponent> components, List<Atom> atoms) {
		this.atoms = atoms;
		this.components = components;
	}

	@Override
	public List<Atom> getAtoms() {
		return new ArrayList<Atom>(this.atoms);
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
		List<FormulaComponent> groundedComponents = new ArrayList<FormulaComponent>(this.components.size());
		
		int j = 0;
		Atom ground;
		for (Atom a : this.atoms) {
			ground = a.isGrounded() ? a : a.ground(groundings);
			
			if (a != ground) {
				while (a != this.components.get(j)) {
					groundedComponents.add(this.components.get(j));
					j++;
				}
				groundedComponents.add(ground);
				j++;
			}
		}
		
		for (FormulaComponent component : groundedComponents) {
			component.evaluate(stack, db);
		}
		
		for (int i = j; i < this.components.size(); i++) {
			this.components.get(i).evaluate(stack, db);
		}
		
		return stack.pop().booleanValue();
	}

	@Override
	public Set<Predicate> getPredicates() {
		Set<Predicate> predicates = new HashSet<Predicate>();
		for (Atom a : this.atoms) {
			predicates.add(a.predicate);
		}
		return predicates;
	}

	@Override
	public Set<Variable> getVariables() {
		Set<Variable> variables = new HashSet<Variable>();
		for (Atom a : this.atoms) {
			variables.addAll(a.getVariables());
		}
		return variables;
	}

	@Override
	public boolean hasPredicate(Predicate p) {
		for (Atom a : this.atoms) {
			if (p == a.predicate) {
				return true;
			}
		}
		return false;
	}
	
	private void initAtoms() {
		for (FormulaComponent component : this.components) {
			if (component instanceof Atom) {
				this.atoms.add((Atom) component);
			}
		}
	}

	@Override
	public boolean isGrounded() {
		for (Atom a : this.atoms) {
			if (!a.isGrounded()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int length() {
		return this.atoms.size();
	}

	@Override
	public GeneralFormula ground(Map<Variable, Constant> groundings) {
		List<Atom> groundedAtoms = new ArrayList<Atom>(this.atoms.size());
		List<FormulaComponent> groundedComponents = new ArrayList<FormulaComponent>(this.components.size());
		
		int j = 0;
		Atom ground;
		for (Atom a : this.atoms) {
			ground = a.isGrounded() ? a : a.ground(groundings);
			groundedAtoms.add(ground);
			
			if (a != ground) {
				while (a != this.components.get(j)) {
					groundedComponents.add(this.components.get(j));
					j++;
				}
				groundedComponents.add(ground);
				j++;
			}
		}
		
		for (int i = j; i < this.components.size(); i++) {
			groundedComponents.add(this.components.get(i));
		}
		
		// old code:
		//		for (FormulaComponent component : this.components) {
		//			if (component instanceof Atom) {
		//				Atom groundedAtom = ((Atom) component).replaceVariables(groundings);
		//				groundedAtoms.add(groundedAtom);
		//				groundedComponents.add(groundedAtom);
		//			} else {
		//				groundedComponents.add(component);
		//			}
		//		}

		return new GeneralFormula(groundedComponents, groundedAtoms);
	}
	
	@Override
	public List<ConjunctiveNormalForm> toCNF() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public GeneralFormula toNegationNormalForm() {
		List<FormulaComponent> nnfComponents = toNegationNormalForm(this.components);
		return new GeneralFormula(nnfComponents, this.atoms);
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
					
				} else { // Atom
					nnf.push(Negation.OPERATOR);
					nnf.push(component);
				}
				
				if (deMorgan) {
					subtract(negate, 2);
					negate.push(Integer.valueOf(i-2));
					negate.push(Integer.valueOf(i-1));
				}
				
			} else if (component instanceof Atom) {
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
	
}
