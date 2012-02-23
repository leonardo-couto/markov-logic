package fol;

import java.util.Arrays;
import java.util.List;

public class Predicate implements Comparable<Predicate> {
	
	private final String name;
	private final List<Domain> argDomains;
	private boolean closedWorld;
	
	public static final Predicate equals = new Predicate("equals", Domain.universe, Domain.universe);
	public static final Predicate empty = new Predicate("empty");

	public Predicate(String name, Domain ... domains) {
		this.name = name;
		this.argDomains = Arrays.asList(domains);
		this.closedWorld = true;
	}

	@Override
	public int compareTo(Predicate o) {
		return this.name.compareTo(o.name);
	}

	/**
	 * @return a List of each argument Domain
	 */
	public List<Domain> getDomains() {
		return this.argDomains;
	}

//	private String _toString() {
//		return this.name + "(" + Util.join(this.argDomains.toArray(), ",") + ")";
//	}
	
	@Override
	public String toString() {
		return this.name;
	}

	/**
	 * @return the closedWorld
	 */
	public boolean isClosedWorld() {
		return this.closedWorld;
	}
	
	/**
	 * @return The total possible number of groundings.
	 * Or Integer.MAX_VALUE if totalGrounds > Integer.MAX_VALUE
	 */
	public int totalGroundings() {
		int i = 1;		
		for (Domain d : this.argDomains) {
			int value = i * d.size();
			if (i > value) { 
				return Integer.MAX_VALUE; 
			} else {
				i = value;
			}
		}
		return i;
	}
	
}
