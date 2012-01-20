package fol;

import java.util.Collections;
import java.util.List;

/**
 * <code>Term</code> is a <code>Constant</code>, <code>Variable</code> or 
 * <code>Function</code>. Here each <code>Term</code> has one (and only one) 
 * <code>Domain</code> eg. person, movie, etc.
 * 
 */
public abstract class Term {
	
	protected List<Domain> domain;
	private final String name;

	public Term(String name, List<Domain> domain) {
		this.name = name;
		this.setDomain(domain);
	}
		
	public Term(String name, Domain domain) {
		this(name, Collections.singletonList(domain));		
	}

	public List<Domain> getDomain() { 
		return this.domain;
	}

	abstract void setDomain(List<Domain> domain);
//	protected void setDomain(List<Domain> domain) {
//		this.domain = domain;
//		for (Domain d : domain) {
//		     d.add(this);
//		}
//	}

	public String toString() {
		return this.name;
	}
	
}
