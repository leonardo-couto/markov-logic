package fol;

import java.util.Arrays;
import java.util.HashMap;

public class SimpleDB implements Database {
	
	private final HashMap<CompositeKey, Boolean> db;
	
	public SimpleDB() {
		this.db = new HashMap<CompositeKey, Boolean>();
	}

	@Override
	public boolean valueOf(Atom a) {
		Boolean value = this.db.get(new CompositeKey(a.predicate, a.terms));
		return value != null && value.booleanValue();
	}

	@Override
	public boolean flip(Atom a) {
		CompositeKey key = new CompositeKey(a.predicate, a.terms);
		Boolean value = this.db.get(key);
		if (value == null || !value.booleanValue()) { 
			this.db.put(key, Boolean.TRUE);
			return true;
		} else if (value != null) {
			this.db.put(key, Boolean.FALSE);
		}
		return false;
	}

	@Override
	public void set(Atom a, boolean value) {
		CompositeKey key = new CompositeKey(a.predicate, a.terms);
		Boolean current = this.db.get(key);
		if (current == null) {
			if (value) {
				this.db.put(key, Boolean.TRUE);
			}
		} else if (value != current.booleanValue()) {
			this.db.put(key, Boolean.valueOf(value));
		}
	}
	
	private static class CompositeKey {

		private final Term[] terms;
		private final Predicate predicate;
		
		public CompositeKey(Predicate p, Term[] terms) {
			this.predicate = p;
			this.terms = terms;
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
			return Arrays.hashCode(this.terms) + 17*this.predicate.hashCode();
		}
	}


}
