package fol.database;

import java.util.Arrays;

import fol.Predicate;
import fol.Term;

class CompositeKey {
	
	private final Term[] terms;
	private final Predicate predicate;
	private final int hashcode;
	private boolean value;
	
	public CompositeKey(Predicate p, Term[] terms, boolean value) {
		this.predicate = p;
		this.terms = terms;
		this.value = value;
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
	
	public boolean getValue() {
		return this.value;
	}
	
	public void setValue(boolean value) {
		this.value = value;
	}

}
