package fol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import stat.sampling.DefaultSampler;
import stat.sampling.RandomIterator;
import stat.sampling.Sampler;
import util.NameID;
import util.Util;

/**
 * @author Leonardo Castilho Couto
 *
 */
public class Predicate implements NameID {
	// TODO: ASSURE UNIQUE PREDICATE NAMES.

	private final String name;
	private final List<Domain> argDomains;
	private boolean closedWorld;
	private final Map<CompositeKey, Atom> groundings;
	
	private final String toString;
	private final int hash;

	public static final Predicate equals = new Predicate("equals", Domain.universe, Domain.universe);
	public static final Predicate empty = new Predicate("empty", Collections.<Domain>emptyList());

	/**
	 * 
	 */
	public Predicate(String name, List<Domain> domains) {
		this.name = name;
		this.argDomains = domains;
		this.closedWorld = false;
		this.groundings = new HashMap<CompositeKey, Atom>();
		this.toString = this._toString();
		this.hash = this.toString.hashCode();
	}

	public Predicate(String name, Domain ... domains) {
		this.name = name;
		this.argDomains = Arrays.asList(domains);
		this.closedWorld = false;
		this.groundings = new HashMap<CompositeKey, Atom>();
		this.toString = this._toString();
		this.hash = this.toString.hashCode();
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return a List of each argument Domain
	 */
	public List<Domain> getDomains() {
		return this.argDomains;
	}

	private String _toString() {
		return this.name + "(" + Util.join(this.argDomains.toArray(), ",") + ")";
	}
	
	@Override
	public String toString() {
		return this.toString;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if ( !(o instanceof Predicate) ) return false;

		Predicate p = (Predicate) o;
		return this.hash == p.hash;
	}

	public int hashCode() {
		return this.hash;
	}

	/**
	 * @return the closedWorld
	 */
	public boolean isClosedWorld() {
		return this.closedWorld;
	}

	/**
	 * @param closedWorld the closedWorld to set
	 */
	public void setClosedWorld(boolean b) {
		this.closedWorld = b;
	}
	
	public void addGrounding(Atom a) {
		if (a.predicate == this) {
			CompositeKey ck = new CompositeKey(a.terms);
			this.groundings.put(ck, a);
		}
	}
	
	public boolean hasGrounding(Term ... terms ) {
		return this.groundings.get(new CompositeKey(terms)) != null;
	}
	
	public Atom getGrounding(Term ... terms) {
		Atom a = this.groundings.get(new CompositeKey(terms));
		if (a == null) {
			return this.closedWorld ? new Atom(this, 0.0, terms) : 
				new Atom(this, Double.NaN, terms);
		}
		return a;
	}
	
	public double groundingValue(Term ... terms) {
		Atom a = this.groundings.get(new CompositeKey(terms));
		if (a == null) {
			return this.closedWorld ? 0.0 : Double.NaN;
		}
		return a.value;
	}
	
	private Iterator<Atom> cwIterator(final List<? extends Collection<Constant>> domains, int maxElements) {
		final Sampler<Constant> sampler = new DefaultSampler<Constant>(domains, maxElements);
		return new Iterator<Atom>() {
			
			private final Iterator<List<Constant>> consts = sampler.iterator();
			private final int arity = domains.size();

			@Override
			public boolean hasNext() {
				return this.consts.hasNext();
			}

			@Override
			public Atom next() {
				Term[] t = this.consts.next().toArray(new Term[this.arity]);
				return getGrounding(t);
			}

			@Override
			public void remove() {
				// do nothing					
			}
		};
	}
	
	public Iterator<Atom> groundingIterator(int maxElements) {
		if (maxElements < 0) { maxElements = Integer.MAX_VALUE;	}
		if (this.closedWorld) {
			return this.cwIterator(this.argDomains, maxElements);
		}
		return (new RandomIterator<Atom>(this.groundings.values(), maxElements)).iterator();		
	}
	
	public Iterator<Atom> groundingFilter(Term[] terms, int maxElements) {
		if (maxElements < 0) { maxElements = Integer.MAX_VALUE;	}
		if (this.closedWorld) {
			List<Set<Constant>> domains = new ArrayList<Set<Constant>>(this.argDomains.size());
			for (Term t : terms) {
				if (t instanceof Constant) {
					domains.add(Collections.singleton((Constant) t));
				} else if (t instanceof Variable) {
					Variable v = (Variable) t;
					domains.add(v.getConstants());
				} else {
					throw new UnsupportedOperationException();
				}
			}
			return this.cwIterator(domains, maxElements);
		} else {
			List<Atom> atoms = new LinkedList<Atom>();
			Map<Integer, Constant> filter = new HashMap<Integer, Constant>();
			for (int i = 0; i < terms.length; i++) {
				Term t = terms[i];
				if (t instanceof Constant) {
					filter.put(i, (Constant) t);
				}
			}
			nextGrounding: for (Atom a : this.groundings.values()) {
				for (Integer i : filter.keySet()) {
					if (a.terms[i] != filter.get(i)) {
						continue nextGrounding;
					}
				}
				atoms.add(a);
			}
			return (new RandomIterator<Atom>(atoms, maxElements)).iterator();	
		}
	}

	/**
	 * @return the groundings
	 */
	//public Map<Atom, Double> getGroundings() {
	//	return this.groundings;
	//}
	
	public long groundingsSize() {
		return this.closedWorld ? this.totalGroundings() : this.groundings.size();
	}

	/**
	 * @return The total possible number of groundings.
	 */
	public long totalGroundings() {
		long i = 1;		
		for (Domain d : argDomains) {
			i = i * (long) d.size();
		}
		return i;
	}
	
	private static class CompositeKey {
		private final Term[] terms;
		
		public CompositeKey(Term[] terms) {
			this.terms = terms;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj != null && obj instanceof CompositeKey) {
				CompositeKey ck1 = (CompositeKey) obj;
				return Arrays.equals(this.terms, ck1.terms);
			}
			return false;
		}
		
		@Override
		public int hashCode() {
			return Arrays.hashCode(this.terms);
		}
	}

}
