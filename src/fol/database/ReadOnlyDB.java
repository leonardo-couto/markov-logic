package fol.database;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import fol.Atom;
import fol.Constant;
import fol.Predicate;
import fol.Term;
import fol.Variable;

class ReadOnlyDB implements Database {
	
	private final Set<CompositeKey> db;
	private final Map<Predicate, CompositeKey[][]> indexedValues;
	
	public ReadOnlyDB(Set<CompositeKey> db, Map<Predicate, CompositeKey[][]> indexedValues) {
		this.db = db;
		this.indexedValues = indexedValues;
	}

	@Override
	public boolean valueOf(Atom a) {
		return this.valueOf(new CompositeKey(a.predicate, a.terms, false));
	}
	
	protected boolean valueOf(CompositeKey key) {
		return this.db.contains(key);
	}

	@Override
	public boolean flip(Atom a) {
		throw new UnsupportedOperationException("Read only database.");
	}

	@Override
	public void set(Atom a, boolean value) {
		throw new UnsupportedOperationException("Read only database.");		
	}

	@Override
	public Database getLocalCopy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<Atom> groundingIterator(Atom filter) {
		final Atom seed = filter;
		final HashMap<Variable, Constant> groundings = new HashMap<Variable, Constant>();
		long lSize = 1;
		for (Term t : filter.terms) {
			if (t instanceof Variable) {
				lSize = lSize * t.getDomain().size();
				groundings.put((Variable) t, null);
			}
		}
		final int size = (int) Math.min(lSize, Integer.MAX_VALUE);
		return new Iterator<Atom>() {
			
			int i = 0;

			@Override
			public boolean hasNext() {
				return i < size;
			}

			@Override
			public Atom next() {
				i++;
				for (Variable v : groundings.keySet()) {
					groundings.put(v, v.getRandomConstant());
				}
				return seed.ground(groundings);
			}

			@Override
			public void remove() {
				// do nothing
			}
		};
	}

}
