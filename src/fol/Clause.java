package fol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fol.database.BinaryDB;
import fol.database.RealDB;
import fol.database.Groundings;
import fol.operator.Disjunction;

/**
 * Represents a Clause. A Clause is a disjunction of literals. 
 */
public class Clause implements Formula, Comparable<Clause> {
	
	public static final Clause FALSE = new Clause(Literal.FALSE);
	public static final Clause TRUE  = new Clause(Literal.TRUE);
	
	private static final String DISJUNCTION_CONNECTOR = " v ";
	
	private final List<Literal> literals;	

	/**
	 * Create a clause with this literals.
	 * @param literals a Collection of Literal.
	 */
	public Clause(Collection<? extends Literal> literals) {
		this(literals, false);
	}
	
	/**
	 * Creates a clause with a single literal
	 */
	public Clause(Literal literal) {
		this.literals = Collections.singletonList(literal);
	}
	
	Clause(Collection<? extends Literal> literals, boolean ordered) {
		Iterator<? extends Literal> iterator = literals.iterator();
		if (!ordered && !literals.isEmpty()) {
			Clause clause = new Clause(iterator.next());
			while (iterator.hasNext()) {
				clause = clause.addLiteral(iterator.next());
			}
			this.literals = clause.literals;
			
		} else {
			this.literals = new ArrayList<Literal>(literals);
		}
	}
	
	/**
	 * Extends this clause by adding another literal.
	 * If this clause already contains this literal, return this clause unmodified.
	 * If this clause contains 
	 * @param a Atom
	 * @param negated true for a negated literal
	 * @return
	 */
	public Clause addLiteral(Literal l) {
		if (TRUE.equals(this)) return TRUE;
		if (FALSE.equals(this)) return new Clause(l);
		
		Comparator<Literal> comparator = new Literal.AtomComparator();
		int i = Collections.binarySearch(this.literals, l, comparator);
		if (i >= 0) { // (+(insertion point) + 1)
			if (this.literals.get(i).signal == l.signal) return this;
			else return TRUE;
		}
		
		ArrayList<Literal> literals = new ArrayList<Literal>(this.literals.size()+1);
		literals.addAll(this.literals);
		literals.add(-i-1, l);
		
		return new Clause(literals, true);
	}
	
	@Override
	public int compareTo(Clause o) {
		int s0 = this.literals.size();
		int s1 = o.literals.size();
		int size = Math.min(s0, s1);
		
		for (int i = 0; i < size; i++) {
			Literal l0 = this.literals.get(i);
			Literal l1 = o.literals.get(i);
			if (!l0.equals(l1)) {
				return l0.compareTo(l1);
			}
		}

		return (s0 == s1) ? 0 : s0 > s1 ? 1 : -1;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o instanceof Clause) {
			Clause other = (Clause) o;
			return this.literals.equals(other.literals);
		}
		return false;
	}

	@Override
	public List<Atom> getAtoms() {
		List<Atom> atoms = new ArrayList<Atom>(this.literals.size());
		for (Literal l : this.literals) {
			atoms.add(l.atom);
		}
		return atoms;
	}
	
	@Override
	public List<FormulaComponent> getComponents() {
		int size = this.literals.size()*2 -1;
		List<FormulaComponent> components = new ArrayList<FormulaComponent>(size);
		Iterator<Literal> iterator = this.literals.iterator();
		
		components.add(iterator.next());
		while (iterator.hasNext()) {
			components.add(iterator.next());
			components.add(Disjunction.OPERATOR);
		}
		
		return components;
	}
	
	public List<Literal> getLiterals() {
		return new ArrayList<Literal>(this.literals);
	}

	@Override
	public Set<Predicate> getPredicates() {
		Set<Predicate> predicates = new HashSet<Predicate>(this.literals.size()*2);
		for (Literal l : this.literals) {
			predicates.addAll(l.getPredicates());
		}
		return predicates;
	}

	@Override
	public boolean getValue(BinaryDB db) {
		int size = this.literals.size();
		for (int i = 0; i < size; i++) {
			if (this.literals.get(i).getValue(db)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public double getValue(RealDB db) {
		int size = this.literals.size();
		double value = 0.0d;
		for (int i = 0; i < size; i++) {
			double literal = this.literals.get(i).getValue(db);
			if (literal == 1.0d) {
				return 1.0d;
			} else if (literal != 0.0d) {
				value += literal - (value*literal);
			}
		}
		return value;
	}

	@Override
	public boolean getValue(BinaryDB db, Map<Variable, Constant> groundings) {
		int size = this.literals.size();
		for (int i = 0; i < size; i++) {
			Literal grounded = this.literals.get(i).ground(groundings);
			if (grounded.getValue(db)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public double getValue(RealDB db, Map<Variable, Constant> groundings) {
		int size = this.literals.size();
		double value = 0.0d;
		for (int i = 0; i < size; i++) {
			Literal grounded = this.literals.get(i).ground(groundings);
			double literal = grounded.getValue(db);
			if (literal == 1.0d) {
				return 1.0d;
			} else if (literal != 0.0d) {
				value += literal - (value*literal);
			}
		}
		return value;
	}

	@Override
	public Set<Variable> getVariables() {
		Set<Variable> variables = new HashSet<Variable>();
		for (Literal l : this.literals) {
			variables.addAll(l.getVariables());
		}
		return variables;
	}

	@Override
	public Clause ground(Map<Variable, Constant> groundings) {
		List<Literal> groundLiterals = new ArrayList<Literal>(this.literals);
		
		boolean changed = false;
		for (int i = 0; i < groundLiterals.size(); i++) {
			Literal l = groundLiterals.get(i);
			Literal ground = l.ground(groundings);
			if (ground != l) {
				changed = true;
				groundLiterals.set(i, ground);
			}
		}
		
		return changed ? new Clause(groundLiterals, true) : this;
	}

	@Override
	public boolean hasPredicate(Predicate p) {
		for (Literal l : this.literals) {
			if (p == l.atom.predicate) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isGrounded() {
		for (Literal l : this.literals) {
			if (!l.isGrounded()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int length() {
		return this.literals.size();
	}
	
	/**
	 * Replace the variables in such a way that the first variable in a 
	 * domain that appears in the formula will be the first variable 
	 * from that domain list of variables ({@link Domain#getVariables}).
	 * @return A formula equivalent to this, with ordered variables.
	 */
	public Clause normalizeVariables() {
		Map<Domain, Iterator<Variable>> variables = new HashMap<Domain, Iterator<Variable>>();
		Map<Variable, Variable> map = new HashMap<Variable, Variable>();
		List<Literal> literals = new ArrayList<Literal>(this.literals.size());
		
		for (Literal l : this.literals) {
			Term[] oldTerms = l.atom.terms;
			Term[] terms = new Term[oldTerms.length];
			for (int i = 0; i < oldTerms.length; i++) {
				Variable v = (Variable) oldTerms[i];
				if (map.containsKey(v)) {
					terms[i] = map.get(v);
				} else {
					Domain d = v.getDomain();
					if (!variables.containsKey(d)) {
						variables.put(d, d.getVariables().iterator());
					}
					Variable v1 = variables.get(d).next();
					map.put(v, v1);
					terms[i] = v1;
				}
			}
			Atom atom = new Atom(l.atom.predicate, terms, false);
			Literal literal = new Literal(atom, l.signal); 
			this.addOrdered(literals, literal);
		}		
		
		return new Clause(literals, true);
	}
	
	private void addOrdered(List<Literal> literals, Literal literal) {
		literals.add(literal);
		Predicate p = literal.atom.predicate;
		for (int i = literals.size()-1; i > 0; i--) {
			Literal l = literals.get(i-1);
			if (p == l.atom.predicate && (literal.compareTo(l) < 0)) {
				literals.set(i, l);
				literals.set(i-1, literal);
			} else {
				return;
			}
		}
	}
	
	/**
	 * <p>Reduce trivial values when searching for a model that
	 * satisfies this Formula. Store the reduced Atoms and their
	 * values in the constants map in the result.</p>
	 * <p>
	 */
	public ReducedClause reduce(Map<Atom, Boolean> constants) {
		
		boolean modified = false;
		
		List<Literal> literals = new ArrayList<Literal>(this.literals.size());		
		for (Literal literal : this.literals) {
			if (literal.atom == Atom.TRUE) {
				if (literal.signal) return new ReducedClause(Clause.TRUE);
				modified = true;
			}
			Boolean b = constants.get(literal.atom);
			if (b != null) {
				if (b.booleanValue() == literal.signal) return new ReducedClause(Clause.TRUE);
				modified = true;
			} else {
				literals.add(literal);
			}
		}
		
		int size = literals.size();
		if (size == 0) return new ReducedClause(Clause.FALSE);
		if (size == 1) {
			Literal l = literals.get(0);
			Map<Atom, Boolean> map = Collections.singletonMap(l.atom, l.signal);
			return new ReducedClause(map, Clause.TRUE);
		}
		
		Clause c = modified ? new Clause(literals, true) : this;
		return new ReducedClause(c);
	}
	
	@Override
	public Clause replace(Atom original, Literal replacement) {
		ArrayList<Literal> literals = new ArrayList<Literal>(this.literals.size());
		
		boolean same = true;
		for (Literal literal : this.literals) {
			Literal replaced = literal.replace(original, replacement);
			literals.add(replaced);
			same = same && literal == replaced;
		}
		
		return same ? this : new Clause(literals);
	}
	
	@Override
	public CNF toCNF() {
		return new CNF(this);
	}
	
	@Override
	public String toString() {
		Deque<StringBuilder> deque = new LinkedList<StringBuilder>();
		for (Literal l : this.literals) {
			l.print(deque);
		}
		
		StringBuilder sb = deque.pollLast();
		while (!deque.isEmpty()) {
			sb.append(DISJUNCTION_CONNECTOR);
			sb.append(deque.pollLast());
		}
		
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
	 * Stores the result of a call to reduce a Clause.
	 * See {@link CNF#reduce()}
	 */
	public static class ReducedClause {
		
		public final Map<Atom, Boolean> constants;
		public final Clause formula;
		
		public ReducedClause(Map<Atom, Boolean> constants, Clause formula) {
			this.constants = constants;
			this.formula = formula;
		}
		
		public ReducedClause(Clause formula) {
			this.constants = Collections.emptyMap();
			this.formula = formula;
		}
		
	}
	
}
