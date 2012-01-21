package fol.database;

import java.util.Arrays;
import java.util.Comparator;

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
	
	/**
	 * Gets a comparator for CompositeKeys 
	 * @param domain index of terms, the term that will be used to compare
	 * @return
	 */
	public static Comparator<CompositeKey> getComparator(final int domain) {
		return new Comparator<CompositeKey>() {

			@Override
			public int compare(CompositeKey o1, CompositeKey o2) {
				return o1.terms[domain].toString().compareTo(o2.terms[domain].toString());
			}
		};
	}

}
