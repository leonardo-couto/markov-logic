package fol;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fol.database.BinaryDB;
import fol.database.RealDB;
import fol.database.Groundings;
import fol.operator.Operator;

public final class Atom implements Formula, FormulaComponent, Comparable<Atom> {
	
	private final boolean grounded;
	public final Predicate predicate;
	public final Term[] terms;
	
	// lazy instantiated
	private int hashCode = -1;
	
	private static final Character COMMA = ',';
	private static final Predicate EMPTY = new Predicate("empty");
	public static final Atom TRUE = new Atom(EMPTY, new Term[0], true);
	
	public Atom(Predicate predicate, Term ... terms) {
		this.predicate = predicate;
		this.terms = terms;
		for (int i = 0; i < terms.length; i++) {
			if (!(terms[i] instanceof Constant)) {
				this.grounded = false;
				return;
			}
		}
		this.grounded = true;
	}
	
	protected Atom(Predicate predicate, Term[] terms, boolean grounded) {
		this.predicate = predicate;
		this.terms = terms;
		this.grounded = grounded;
	}
	
	@Override
	public int compareTo(Atom o) {
		if (this.predicate == o.predicate) {
			Term t1;
			Term t2;
			for (int i = 0; i < this.terms.length; i++) {
				t1 = this.terms[i];
				t2 = o.terms[i];
				if (t1 != t2) {
					return t1.compareTo(t2);
				}
			}
			return 0;
		}
		return this.predicate.compareTo(o.predicate);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		Atom other = (Atom) obj;
		if (this.predicate != other.predicate) return false;
		for (int i = 0; i < this.terms.length; i++) {
			if (this.terms[i] != other.terms[i]) return false;
		}
		return true;
	}
	
	@Override
	public void evaluate(Deque<Boolean> stack, BinaryDB db) {
		stack.push(this.getValue(db));
	}
	
	@Override
	public void evaluate(Deque<Double> stack, RealDB db) {
		stack.push(this.getValue(db));
	}

	@Override
	public List<Atom> getAtoms() {
		return Collections.singletonList(this);
	}
	
	@Override
	public List<FormulaComponent> getComponents() {
		return Collections.<FormulaComponent>singletonList(this);
	}
	
	@Override
	public Set<Predicate> getPredicates() {
		return Collections.singleton(this.predicate);
	}
	
	public static Set<Predicate> getPredicates(Collection<Atom> atoms) {
		Set<Predicate> set = new HashSet<Predicate>();
		for (Atom a : atoms) {
			set.add(a.predicate);
		}
		return set;
	}
	
	@Override
	public boolean getValue(BinaryDB db) {
		if (!this.grounded) {
			throw new RuntimeException("getValue of non-grounded Atom " + this.toString());
		}
		return this.predicate == EMPTY ? true : db.valueOf(this);
	}
	
	@Override
	public double getValue(RealDB db) {
		if (!this.grounded) {
			throw new RuntimeException("getValue of non-grounded Atom " + this.toString());
		}
		return this.predicate == EMPTY ? 1.0d : db.valueOf(this);
	}

	@Override
	public boolean getValue(BinaryDB db, Map<Variable, Constant> groundings) {
		Atom a = this.ground(groundings);
		return a.getValue(db);
	}
	
	@Override
	public double getValue(RealDB db, Map<Variable, Constant> groundings) {
		Atom a = this.ground(groundings);
		return a.getValue(db);
	}

	@Override
	public Set<Variable> getVariables() {
		Set<Variable> v = new HashSet<Variable>();
		for (Term t : this.terms) {
			if (t instanceof Variable) {
				v.add((Variable) t);
			}
		}
		return v;
	}
	
	@Override
	public Atom ground(Map<Variable, Constant> groundings) {
		if (this.grounded) return this;
		int length = this.terms.length;
		boolean modified = false;
		boolean grounded = true;
		
		Term[] groundedTerms = new Term[length];
		for (int i = 0; i < length; i++) {
			Term t = this.terms[i];
			if (t instanceof Constant) {
				groundedTerms[i] = t;				
			} else {
				Constant candidate = groundings.get(t);
				if (candidate == null) {
					grounded = false;
					groundedTerms[i] = t;	
				} else {
					modified = true;
					groundedTerms[i] = candidate;
				}
			}
		}
		
		return modified ? new Atom(this.predicate, groundedTerms, grounded) : this;
	}
	
	@Override
	public int hashCode() {
		if (this.hashCode != -1) return this.hashCode;
		this.hashCode = Arrays.hashCode(this.terms) + 17*this.predicate.hashCode();
		return this.hashCode;
	}

	@Override
	public boolean hasPredicate(Predicate p) {
		return this.predicate == p;
	}

	@Override
	public boolean isGrounded() {
		return this.grounded;
	}

	@Override
	public int length() {
		return 1;
	}
	
	@Override
	public void print(Deque<StringBuilder> stack) {
		stack.push(this.print());
	}
	
	StringBuilder print() {
		StringBuilder b = new StringBuilder();
		if (this.predicate == EMPTY) {
			b.append("TRUE");
			return b;
		}
		b.append(this.predicate.toString()).append(Operator.LEFTP);
		for (Term t : terms) {
			b.append(t.toString());
			b.append(COMMA);
		}
		b.setCharAt(b.length()-1, Operator.RIGHTP);
		return b;
	}

	@Override
	public Formula replace(Atom original, Literal replacement) {
		return this.equals(original) ? replacement : this;
	}

	@Override
	public CNF toCNF() {
		if (this.predicate == EMPTY) return Clause.TRUE.toCNF();
		return (new Literal(this, true)).toCNF();
	}
	
	@Override
	public String toString() {
		return this.print().toString();
	}

	@Override
	public double trueCount(BinaryDB db) {
		return Groundings.count(this, true, db);
	}

	@Override
	public double trueCount(RealDB db) {
		return Groundings.count(this, true, db);
	}

}
