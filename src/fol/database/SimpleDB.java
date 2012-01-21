package fol.database;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import fol.Atom;
import fol.Constant;
import fol.Term;
import fol.Variable;

public class SimpleDB implements Database {
	
	private final HashMap<CompositeKey, Boolean> db;
//	private final HashMap<Predicate, List<List<CompositeKey>>> indexedValues;
//	private boolean readOnly;
	
	public SimpleDB() {
		this.db = new HashMap<CompositeKey, Boolean>();
//		this.indexedValues = new HashMap<Predicate, List<List<CompositeKey>>>();
//		this.readOnly = false;
//		for (Predicate p : predicates) {
//			int domains = p.getDomains().size();
//			List<List<CompositeKey>> indexes = new ArrayList<List<CompositeKey>>(domains);
//			indexedValues.put(p, indexes);
//			for (int i = 0; i < domains; i++) {
//				PriorityQueue<CompositeKey> pqueue = new PriorityQueue<CompositeKey>();
//				//indexes.add()
//			}
//		}
	}

	@Override
	public boolean valueOf(Atom a) {
		return this.valueOf(new CompositeKey(a.predicate, a.terms, false));
	}
	
	protected boolean valueOf(CompositeKey key) {
		Boolean value = this.db.get(key);
		return value != null && value.booleanValue();
	}

	@Override
	public boolean flip(Atom a) {
		CompositeKey key = new CompositeKey(a.predicate, a.terms, false);
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
		CompositeKey key = new CompositeKey(a.predicate, a.terms, false);
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
	
//	private Iterator<Atom> cwIterator(final List<? extends Collection<Constant>> domains, int maxElements) {
//		final Sampler<Constant> sampler = new DefaultSampler<Constant>(domains, maxElements);
//		return new Iterator<Atom>() {
//			
//			private final Iterator<List<Constant>> consts = sampler.iterator();
//			private final int arity = domains.size();
//
//			@Override
//			public boolean hasNext() {
//				return this.consts.hasNext();
//			}
//
//			@Override
//			public Atom next() {
//				Term[] t = this.consts.next().toArray(new Term[this.arity]);
//				return getGrounding(t);
//			}
//
//			@Override
//			public void remove() {
//				// do nothing					
//			}
//		};
//	}
	
	/**
	 * Return an iterator for groundings of Atom a
	 * @param maxElements
	 * @return
	 */
	public Iterator<Atom> groundingIterator(final Atom a) {
	
		if (a.isGrounded()) {
			return Collections.singleton(a).iterator();
		}
		
		final Map<Variable, Constant> groundings = new HashMap<Variable, Constant>();
		final Map<Variable, List<Constant>> constants = new HashMap<Variable, List<Constant>>();
		for (Term t : a.terms) {
			if (t instanceof Variable) {
				Variable v = (Variable) t;
				groundings.put(v, null);
				constants.put(v, v.getConstants());
			}
		}
		final Random r = new Random();
		
		return new Iterator<Atom>() {

			@Override
			public boolean hasNext() {
				return true;
			}

			@Override
			public Atom next() {
				for (Variable v : groundings.keySet()) {
					List<Constant> constantList = constants.get(v);
					groundings.put(v, constantList.get(r.nextInt(constantList.size())));
				}
				return a.ground(groundings);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("Can not remove element.");
			}
		};
		
	}

	@Override
	public void close() {
		// do nothing		
	}
	
//	public Iterator<Atom> groundingIterator(Atom a, int maxElements, boolean value) {
//		if (maxElements < 0) { maxElements = Integer.MAX_VALUE;	}
//		if (this.closedWorld) {
//			return this.cwIterator(this.argDomains, maxElements);
//		}
//		return (new RandomIterator<Atom>(this.groundings.values(), maxElements)).iterator();		
//	}
//	
//	/**
//	 * Return an iterator for this Predicate known groundigs, with
//	 * constraints defined in terms.
//	 * @param terms
//	 * @param maxElements
//	 * @return
//	 */
//	public Iterator<Atom> groundingFilter(Term[] terms, int maxElements) {
//		if (maxElements < 0) { maxElements = Integer.MAX_VALUE;	}
//		if (this.closedWorld) {
//			List<Set<Constant>> domains = new ArrayList<Set<Constant>>(this.argDomains.size());
//			for (Term t : terms) {
//				if (t instanceof Constant) {
//					domains.add(Collections.singleton((Constant) t));
//				} else if (t instanceof Variable) {
//					Variable v = (Variable) t;
//					domains.add(v.getConstants());
//				} else {
//					throw new UnsupportedOperationException();
//				}
//			}
//			return this.cwIterator(domains, maxElements);
//		} else {
//			List<Atom> atoms = new LinkedList<Atom>();
//			Map<Integer, Constant> filter = new HashMap<Integer, Constant>();
//			for (int i = 0; i < terms.length; i++) {
//				Term t = terms[i];
//				if (t instanceof Constant) {
//					filter.put(i, (Constant) t);
//				}
//			}
//			nextGrounding: for (Atom a : this.groundings.values()) {
//				for (Integer i : filter.keySet()) {
//					if (a.terms[i] != filter.get(i)) {
//						continue nextGrounding;
//					}
//				}
//				atoms.add(a);
//			}
//			return (new RandomIterator<Atom>(atoms, maxElements)).iterator();	
//		}
//	}
	
}