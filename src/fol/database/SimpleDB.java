package fol.database;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import weightLearner.wpll.Count;

import fol.Atom;
import fol.Constant;
import fol.Formula;
import fol.Term;
import fol.Variable;

public class SimpleDB implements Database {
	
	private final HashMap<CompositeKey, Boolean> db;
	private final CountCache counter;
	
	public SimpleDB() {
		this.db = new HashMap<CompositeKey, Boolean>();
		this.counter = new CountCache(this);
	}

	@Override
	public boolean valueOf(Atom a) {
		if (Atom.TRUE == a) return true;
		return this.valueOf(new CompositeKey(a.predicate, a.terms));
	}
	
	protected boolean valueOf(CompositeKey key) {
		Boolean value = this.db.get(key);
		return value != null && value.booleanValue();
	}

	@Override
	public boolean flip(Atom a) {
		if (Atom.TRUE == a) return true;
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
		if (Atom.TRUE == a) return;
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

	@Override
	public Database getLocalCopy() {
		return new LocalDB(this);
	}
	
	/**
	 * Return an iterator for groundings of Atom a
	 * @return
	 */
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
	
	/**
	 * Return an iterator for groundings of Atom a
	 * @return
	 */
	@Override
	public Iterator<Atom> groundingIterator(Atom filter, boolean value) {
		final Iterator<Atom> iterator = this.groundingIterator(filter);
		final int max = this.groundingCount(filter);
		final boolean b = value;
		return new Iterator<Atom>() {

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public Atom next() {
				for (int i = 0; i < max; i++) {
					Atom a = iterator.next();
					if (valueOf(a) == b) {
						return a;
					}
				}
				// return an atom even if it doesn't satisfies the value condition
				return iterator.next(); 
			}

			@Override
			public void remove() {
				// do nothing
			}
		};
	}
	
	@Override
	public int groundingCount(Atom filter) {
		long l = 1;
		for (Term t : filter.terms) {
			if (t instanceof Variable) {
				l = l * t.getDomain().size();
			}
		}
		return (int) Math.min(l, Integer.MAX_VALUE);
	}

	@Override
	public int groundingCount(Atom filter, boolean value) {
		int total = this.groundingCount(filter);
		int sample = total < 700 ? total : 700; // 500 for a error of at most 5%
		// TODO corrigir sample

		int count = 0;
		Iterator<Atom> atoms = this.groundingIterator(filter);
		for (int i = 0; i < sample; i++) {
			if (this.valueOf(atoms.next()) == value) {
				count++;
			}
		}
		double ratio = ((double) count) / sample;
		return (int) Math.round(ratio * total);
	}

	@Override
	public List<Count> getCounts(Formula formula, int sampleSize) {
		return this.counter.getCounts(formula, sampleSize);
	}

	
}
