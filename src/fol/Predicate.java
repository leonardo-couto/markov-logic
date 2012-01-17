package fol;

import java.util.Arrays;
import java.util.List;

import util.Util;

public class Predicate {
	
	private final String name;
	private final List<Domain> argDomains;
	private boolean closedWorld;
	
	private final String toString;

	public static final Predicate equals = new Predicate("equals", Domain.universe, Domain.universe);
	public static final Predicate empty = new Predicate("empty");

	public Predicate(String name, Domain ... domains) {
		this.name = name;
		this.argDomains = Arrays.asList(domains);
		this.closedWorld = true;
		this.toString = this._toString();
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

	/**
	 * @return the closedWorld
	 */
	public boolean isClosedWorld() {
		return this.closedWorld;
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
	
}
