package fol;

import java.util.Collections;
import java.util.List;

import util.NameID;

/**
 * <code>Term</code> is a <code>Constant</code>, <code>Variable</code> or 
 * <code>Function</code>. Here each <code>Term</code> has one (and only one) 
 * <code>Domain</code> eg. person, movie, etc.
 * 
 * @author Leonardo Castilho Couto
 * 
 */
public abstract class Term implements NameID {
	// TODO: id for each term?
	
	protected List<Domain> domain;
	private String name;

	/**
	 * 
	 */
	public Term(String name, List<Domain> domain) {
		setDomain(domain);
		setName(name);
	}
		
	
	/**
	 * 
	 */
	public Term(String name, Domain domain) {
		setDomain(Collections.singletonList(domain));
		setName(name);
	}

	// TODO: maybe return a copy of this list?
	public List<Domain> getDomain() { 
		return domain;
	}

	public String getName() {
		return name;
	}

	abstract void setDomain(List<Domain> domain);
//	protected void setDomain(List<Domain> domain) {
//		this.domain = domain;
//		for (Domain d : domain) {
//		     d.add(this);
//		}
//	}

	private void setName(String name) {
		this.name = name;
	}
	
	public String toString() {
		return this.name;
	}
	
}
