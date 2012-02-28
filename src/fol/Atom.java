package fol;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fol.database.Database;
import fol.operator.Operator;

public class Atom implements Formula, FormulaComponent, Comparable<Atom> {

	private final boolean grounded;
	public final Predicate predicate;
	public final Term[] terms;
	
	private static final Character COMMA = ',';
	
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
		return compare(this, o);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Atom other = (Atom) obj;
		if (!predicate.equals(other.predicate))
			return false;
		if (!Arrays.equals(terms, other.terms))
			return false;
		return true;
	}
	
	@Override
	public void evaluate(Deque<Boolean> stack, Database db) {
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
	public boolean getValue(Database db) {
		if (!this.grounded) {
			throw new RuntimeException("getValue of non-grounded Atom " + this.toString());
		}
		return db.valueOf(this);
	}

	@Override
	public boolean getValue(Database db, Map<Variable, Constant> groundings) {
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((predicate == null) ? 0 : predicate.hashCode());
		result = prime * result + Arrays.hashCode(terms);
		return result;
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
	
	private StringBuilder print() {
		StringBuilder b = new StringBuilder();
		b.append(this.predicate.toString()).append(Operator.LEFTP);
		for (Term t : terms) {
			b.append(t.toString());
			b.append(COMMA);
		}
		b.setCharAt(b.length()-1, Operator.RIGHTP);
		return b;
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
	public List<Clause> toCNF() {
		Literal l = new Literal(this, true);
		List<Literal> singleton = Collections.singletonList(l);
		Clause cnf = new Clause(singleton);
		return Collections.singletonList(cnf);
	}
	
	public static void main(String[] args) {
		for (int i = 0; i < 11; i++) {
			System.out.println(1 << i);
		}
	}
	
	@Override
	public String toString() {
		return this.print().toString();
	}

	@Override
	public double trueCount(Database db) {
		return (double) db.groundingCount(this, true);
	}
	
	private static int compare(Atom a0, Atom a1) {
		if (a0.predicate == a1.predicate) {
			Term t1;
			Term t2;
			for (int i = 0; i < a0.terms.length; i++) {
				t1 = a0.terms[i];
				t2 = a1.terms[i];
				if (t1 != t2) {
					return t1.compareTo(t2);
				}
			}
			return 0;
		}
		return a0.predicate.compareTo(a1.predicate);
	}
	
	public static final class DefaultComparator implements Comparator<Atom> {

		@Override
		public int compare(Atom a0, Atom a1) {
			return Atom.compare(a0, a1);
		}

	}

}
