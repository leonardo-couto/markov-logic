package fol.database;

import java.util.Arrays;

import fol.Atom;
import fol.Predicate;
import fol.Term;

public class CompositeKey {
	
	private final Term[] terms;
	public final Predicate predicate;
	private final int hashcode;
	
	public CompositeKey(Atom atom) {
		this(atom.predicate, atom.terms);
	}
	
	public CompositeKey(Predicate p, Term[] terms) {
		this.predicate = p;
		this.terms = terms;
		this.hashcode = Arrays.hashCode(this.terms) + 17*this.predicate.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof CompositeKey) {
			CompositeKey ck1 = (CompositeKey) obj;
			return this.predicate == ck1.predicate && 
			       Arrays.equals(this.terms, ck1.terms);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return this.hashcode;
	}
	
}
