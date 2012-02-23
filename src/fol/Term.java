package fol;

/**
 * <code>Term</code> is a <code>Constant</code>, <code>Variable</code> or 
 * <code>Function</code>. Here each <code>Term</code> has one (and only one) 
 * <code>Domain</code> eg. person, movie, etc.
 * 
 */
public abstract class Term implements Comparable<Term> {
	
	protected final Domain domain;
	private final String name;

	public Term(String name, Domain domain) {
		this.domain = domain;
		this.name = name;
		this.setDomain(domain);
	}
	
	@Override
	public int compareTo(Term o) {
		return this.name.compareTo(o.name);
	}
	
	public Domain getDomain() { 
		return this.domain;
	}

	abstract void setDomain(Domain domain);
//      domain.add(this);

	public String toString() {
		return this.name;
	}
	
}
