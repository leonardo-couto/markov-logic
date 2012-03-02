package fol;

import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fol.database.Database;
import fol.database.Groundings;

public class Literal implements Formula, FormulaComponent, Comparable<Literal> {

	public final Atom atom;
	public final boolean signal;
	
	public static final Literal FALSE = new Literal(Atom.TRUE, false);
	public static final Literal TRUE  = new Literal(Atom.TRUE, true);
	
	private static final Character NEGATION = '!';
	
	public Literal(Atom atom, boolean signal) {
		this.atom = atom;
		this.signal = signal;
	}

	@Override
	public int compareTo(Literal o) {
		int compare = this.atom.compareTo(o.atom);
		if (compare == 0) {
			return this.signal ? (o.signal ? 0 : -1) : (o.signal ? 1 : 0);
		}
		return compare;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof Literal)) return false;
		Literal other = (Literal) obj;
		return this.atom.equals(other.atom) ? this.signal == other.signal : false;
	}

	@Override
	public void evaluate(Deque<Boolean> stack, Database db) {
		stack.push(this.getValue(db));
	}
	
	private StringBuilder print() {
		if (this.atom == Atom.TRUE) {
			return this.signal ? new StringBuilder("TRUE") : new StringBuilder("FALSE");
		}
		StringBuilder sb = atom.print();
		if (!this.signal) {
			sb.insert(0, NEGATION);
		}
		return sb;
	}

	@Override
	public void print(Deque<StringBuilder> stack) {
		stack.push(this.print());
	}

	@Override
	public List<Atom> getAtoms() {
		return Collections.singletonList(this.atom);
	}

	@Override
	public List<FormulaComponent> getComponents() {
		return Collections.<FormulaComponent>singletonList(this);
	}

	@Override
	public Set<Predicate> getPredicates() {
		return this.atom.getPredicates();
	}

	@Override
	public boolean getValue(Database db) {
		return this.signal == this.atom.getValue(db);
	}

	@Override
	public boolean getValue(Database db, Map<Variable, Constant> groundings) {
		return this.signal == this.atom.getValue(db, groundings);
	}

	@Override
	public Set<Variable> getVariables() {
		return this.atom.getVariables();
	}

	@Override
	public Literal ground(Map<Variable, Constant> groundings) {
		Atom grounded = this.atom.ground(groundings);
		return this.atom == grounded ? this : new Literal(grounded, this.signal);
	}

	@Override
	public int hashCode() {
		int hash = this.atom.hashCode();
		return this.signal ? hash : -hash;
	}
	
	@Override
	public boolean hasPredicate(Predicate p) {
		return this.atom.predicate == p;
	}

	@Override
	public boolean isGrounded() {
		return this.atom.isGrounded();
	}

	@Override
	public int length() {
		return 1;
	}

	@Override
	public CNF toCNF() {
		return (new Clause(this)).toCNF();
	}
	
	@Override
	public String toString() {
		return this.print().toString();
	}

	@Override
	public double trueCount(Database db) {
		return Groundings.groundingCount(this.atom, this.signal, db);
	}
	
	public static final class AtomComparator implements Comparator<Literal> {

		@Override
		public int compare(Literal l0, Literal l1) {
			return l0.atom.compareTo(l1.atom);
		}

	}

}
