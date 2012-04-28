package fol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fol.database.BinaryDB;
import fol.database.RealDB;
import fol.database.Groundings;
import fol.operator.Conjunction;

/**
 * Represents a Formula in a <b>Conjunctive Normal Form</b> (CNF).
 * A CNF formula is a conjunction of Clauses.
 */
public class CNF implements Formula {
	
	private final List<Clause> clauses;
	
	public CNF(Collection<? extends Clause> clauses) {
		this.clauses = new ArrayList<Clause>(clauses);
	}
	
	/**
	 * Creates a CNF formula with a single clause
	 * @param clause
	 */
	public CNF(Clause clause) {
		this.clauses = Collections.singletonList(clause);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CNF other = (CNF) obj;
		if (!this.clauses.equals(other.clauses))
			return false;
		return true;
	}

	@Override
	public List<Atom> getAtoms() {
		List<Atom> atoms = new ArrayList<Atom>();
		for (Clause c : this.clauses) {
			atoms.addAll(c.getAtoms());
		}
		return atoms;
	}
	
	public List<Clause> getClauses() {
		return new ArrayList<Clause>(this.clauses);
	}

	@Override
	public List<FormulaComponent> getComponents() {
		List<FormulaComponent> components = new ArrayList<FormulaComponent>();
		Iterator<Clause> iterator = this.clauses.iterator();
		
		components.addAll(iterator.next().getComponents());
		while (iterator.hasNext()) {
			components.addAll(iterator.next().getComponents());
			components.add(Conjunction.OPERATOR);
		}
		
		return components;
	}

	@Override
	public Set<Predicate> getPredicates() {
		Set<Predicate> predicates = new HashSet<Predicate>();
		for (Clause c : this.clauses) {
			predicates.addAll(c.getPredicates());
		}
		return predicates;
	}

	@Override
	public boolean getValue(BinaryDB db) {
		int size = this.clauses.size();
		for (int i = 0; i < size; i++) {
			if (!this.clauses.get(i).getValue(db)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean getValue(BinaryDB db, Map<Variable, Constant> groundings) {
		int size = this.clauses.size();
		for (int i = 0; i < size; i++) {
			Clause grounded = this.clauses.get(i).ground(groundings);
			if (!grounded.getValue(db)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public double getValue(RealDB db) {
		int size = this.clauses.size();
		double value = 1.0d;
		for (int i = 0; i < size; i++) {
			double clause = this.clauses.get(i).getValue(db);
			if (clause == 0.0d) {
				return 0.0d;
			} else if (clause != 1.0d) {
				value *= clause;
			}
		}
		return value;
	}

	@Override
	public double getValue(RealDB db, Map<Variable, Constant> groundings) {
		int size = this.clauses.size();
		double value = 1.0d;
		for (int i = 0; i < size; i++) {
			Clause grounded = this.clauses.get(i).ground(groundings);
			double clause = grounded.getValue(db);
			if (clause == 0.0d) {
				return 0.0d;
			} else if (clause != 1.0d) {
				value *= clause;
			}
		}
		return value;
	}

	@Override
	public Set<Variable> getVariables() {
		Set<Variable> variables = new HashSet<Variable>();
		for (Clause c : this.clauses) {
			variables.addAll(c.getVariables());
		}
		return variables;
	}

	@Override
	public CNF ground(Map<Variable, Constant> groundings) {
		List<Clause> groundClauses = new ArrayList<Clause>(this.clauses);
		
		boolean changed = false;
		for (int i = 0; i < groundClauses.size(); i++) {
			Clause c = groundClauses.get(i);
			Clause ground = c.ground(groundings);
			if (ground != c) {
				changed = true;
				groundClauses.set(i, ground);
			}
		}
		
		return changed ? new CNF(groundClauses) : this;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime + this.clauses.hashCode();
		return result;
	}

	@Override
	public boolean hasPredicate(Predicate p) {
		for (Clause c : this.clauses) {
			if (c.hasPredicate(p)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isGrounded() {
		for (Clause c : this.clauses) {
			if (!c.isGrounded()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int length() {
		int length = 0;
		for (Clause c : this.clauses) {
			length += c.length();
		}
		return length;
	}
	
	/**
	 * <p>Reduce trivial values when searching for a model that
	 * satisfies this Formula. Store the reduced Atom and its value
	 * in the constants and values Array in the result.</p>
	 * <p>
	 * 
	 * <p>Trivial values are the ones that appears alone in a clause,
	 * consider for example, the following formula:<p>
	 * 
	 * <p><code>(a2 v !a3 v a4) ^ a1 ^ (a1 v !a2)</code></p>
	 * 
	 * <p>For it to be satisfied, <code>a1</code> need to be true. 
	 * Then <code>a1 = true</code> is a trivial value. Replacing 
	 * <code>a1</code> for <code>true</code>, we got:</p>
	 * 
	 * <p><code>(a2 v !a3 v a4) ^ true ^ (true v !a2)</code><br>
	 * <code> = (a2 v !a3 v a4) ^ !a2</code></p>
	 * 
	 * <p>Now <code>a2 = false</code> is a trivial value. This method 
	 * will recursively  reduce the formula until there is no trivial 
	 * values left. In this example the final result would be the formula </p>
	 * 
	 * <p><code>!a3 v a4, a1 = true, a2 = false</code></p>
	 * 
	 */
	public ReducedCNF reduce() {
		Map<Atom, Boolean> constants = new HashMap<Atom, Boolean>();
		
		List<Clause> clauses = this.clauses;
		boolean modified = true;
		
		while (modified) {
			modified = false;
			
			// store the reduced clauses
			List<Clause> reduced = new ArrayList<Clause>(clauses.size());
			
			for (Clause c : clauses) {				
				// reduce and checks for new constants
				Clause.ReducedClause rc = c.reduce(constants);
				Clause formula = rc.formula;
				if (formula == Clause.FALSE) return new ReducedCNF(new CNF(Clause.FALSE));
				if (formula != Clause.TRUE) reduced.add(formula);
				if (!rc.constants.isEmpty()) {
					modified = true;
					constants.putAll(rc.constants);
				}
			}

			clauses = reduced;
		}
		
		// All elements have been reduced, complete the list and return
		if (clauses.isEmpty()) return new ReducedCNF(constants, new CNF(Clause.TRUE));		
		CNF formula = new CNF(clauses);
		return new ReducedCNF(constants, formula);
		
	}
	
	@Override
	public CNF replace(Atom original, Literal replacement) {
		ArrayList<Clause> clauses = new ArrayList<Clause>(this.clauses.size());
		
		boolean same = true;
		for (Clause clause : this.clauses) {
			Clause replaced = clause.replace(original, replacement);
			clauses.add(replaced);
			same = same && clause == replaced;
		}
		
		return same ? this : new CNF(clauses);
	}

	@Override
	public CNF toCNF() {
		return this;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		Iterator<Clause> iterator = this.clauses.iterator();
		
		sb.append('(');
		sb.append(iterator.next().toString());
		while (iterator.hasNext()) {
			sb.append(") ^ (");
			sb.append(iterator.next().toString());
		}
		sb.append(')');
		
		return sb.toString();
	}

	@Override
	public double trueCount(BinaryDB db) {
		return Groundings.count(this, true, db);
	}
	
	@Override
	public double trueCount(RealDB db) {
		return Groundings.count(this, true, db);
	}
	
	/**
	 * Stores the result of a call to reduce a CNF formula.
	 * See {@link CNF#reduce()}
	 */
	public static class ReducedCNF {
		
		public final Map<Atom, Boolean> constants;
		public final CNF formula;
		
		public ReducedCNF(Map<Atom, Boolean> constants, CNF formula) {
			this.constants = constants;
			this.formula = formula;
		}
		
		public ReducedCNF(CNF formula) {
			this.constants = Collections.emptyMap();
			this.formula = formula;
		}
		
	}

}
