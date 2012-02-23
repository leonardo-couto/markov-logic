package fol.database;

import java.util.Arrays;
import java.util.Comparator;

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
