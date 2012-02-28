package fol;

import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import fol.database.Database;

public class Literal extends Atom {
	
	private static final Character NEGATION = '!';
	
	public final boolean signal;
	
	public Literal(Predicate predicate, boolean signal, Term ... terms) {
		super(predicate, terms);
		this.signal = signal;
	}
	
	public Literal(Atom atom, boolean signal) {
		super(atom.predicate, atom.terms, atom.isGrounded());
		this.signal = signal;
	}
	
	Literal(Predicate predicate, Term[] terms, boolean grounded, boolean signal) {
		super(predicate, terms, grounded);
		this.signal = signal;
	}
	
	@Override
	public int compareTo(Atom atom) {
//		int compare = super.compareTo(atom);
//		if (compare == 0 && atom instanceof Literal) {
//			Literal literal = (Literal) atom;
//			return this.signal ? (literal.signal ? 0 : -1) : (literal.signal ? 1 : 0);
//		}
//		return compare;

		if (this.predicate == atom.predicate) {
			Term t1;
			Term t2;
			for (int i = 0; i < this.terms.length; i++) {
				t1 = this.terms[i];
				t2 = atom.terms[i];
				if (t1 != t2) {
					return t1.compareTo(t2);
				}
			}
			if (atom instanceof Literal) {
				return this.signal ? (((Literal) atom).signal ? 0 : -1) : (((Literal) atom).signal ? 1 : 0);
			}			
			return 0;
		}
		return this.predicate.compareTo(atom.predicate);
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj)) {
			if (obj instanceof Literal) {
				return this.signal == ((Literal) obj).signal;
			}
			return this.signal; // positive literal is an atom
		}
		return false;
	}

	@Override
	public boolean getValue(Database db) {
		return this.signal == super.getValue(db);
	}

	@Override
	public int hashCode() {
		int hashCode = super.hashCode();
		return this.signal ? hashCode : -1 * hashCode;
	}

	@Override
	public void print(Deque<StringBuilder> stack) {
		super.print(stack);
		if (!this.signal) {
			stack.peek().insert(0, NEGATION);
		}
	}
	
	@Override
	public Literal ground(Map<Variable, Constant> groundings) {
//		Atom ground = super.ground(groundings);
//		return (this == ground) ? this : new Literal(ground, this.signal);
		if (this.isGrounded()) return this;
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
		
		return modified ? new Literal(this.predicate, groundedTerms, grounded, this.signal) : this;
	}

	@Override
	public List<Clause> toCNF() {
		List<Literal> singleton = Collections.singletonList(this);
		Clause cnf = new Clause(singleton);
		return Collections.singletonList(cnf);
	}
	
	public Atom toAtom() {
		return new Atom(this.predicate, this.terms);
	}

	@Override
	public String toString() {
		String atom = super.toString();
		return this.signal ? atom : NEGATION.toString() + atom; 
	}

	@Override
	public double trueCount(Database db) {
		return (double) db.groundingCount(this, this.signal);
	}
	
}
