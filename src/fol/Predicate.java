package fol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	private final Map<Atom, Double> groundings;
	private Set<Atom> neGroundings;
	
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
		this.groundings = new HashMap<Atom, Double>();
		this.neGroundings = new HashSet<Atom>();
		this.toString = this._toString();
		this.hash = this.toString.hashCode();
	}

	public Predicate(String name, Domain ... domains) {
		this.name = name;
		this.argDomains = Arrays.asList(domains);
		this.closedWorld = false;
		this.groundings = new HashMap<Atom, Double>();
		this.neGroundings = new HashSet<Atom>();
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
		return this.toString.equals(p.toString);
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
		if (b == true && this.closedWorld == false) { 
			if (this.neGroundings.isEmpty()) {
				setGroundings();
			} else {
				for (Atom a : this.neGroundings) {
					this.groundings.put(a, a.value);
				}
			}
		}
		if (b == false && this.closedWorld == true) {
			for (Atom a : this.neGroundings) {
				this.groundings.remove(a);
			}
		}
		this.closedWorld = b;
	}

	/**
	 * @param p
	 * @return A Set of all groundings of p.
	 * Assumes closedWorld.
	 */
	private void setGroundings() {
		Set<Atom> neGroundings = new HashSet<Atom>();
		List<List<Constant>> cll = listGroundings();
		for (List<Constant> cList : cll) {
			Atom a = new Atom(this, 0.0, cList);
			if (!this.groundings.containsKey(a)) {
				neGroundings.add(a);
			}
		}
		this.neGroundings = neGroundings;
		for (Atom a : neGroundings) {
			this.groundings.put(a, a.value);
		}
	}	

	private List<List<Constant>> listGroundings() {
		List<List<Constant>> out = new ArrayList<List<Constant>>();
		List<List<Constant>> prev;
		boolean firstLoop = true;
		for (Domain d : argDomains) {
			prev = out;
			out = new ArrayList<List<Constant>>();
			if (firstLoop) {
				firstLoop = false;
				for (Constant c : d) {
					out.add(Collections.singletonList(c));
				}
				continue;
			}
			for (Constant c : d) {
				for (List<Constant> list : prev) {
					List<Constant> lc = new ArrayList<Constant>(list);
					lc.add(c);
					out.add(lc);
				}
			}
		}
		return out;
	}

	/**
	 * @return the groundings
	 */
	public Map<Atom, Double> getGroundings() {
		return groundings;
	}

	/**
	 * @return The total possible number of groundings.
	 */
	public long totalGroundsNumber() {
		long i = 1;		
		for (Domain d : argDomains) {
			i = i * (long) d.size();
		}
		return i;
	}

}
