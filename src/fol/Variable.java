package fol;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Leonardo Castilho Couto
 *
 */
public class Variable extends Term implements Comparable<Variable>{

	/**
	 * @param arg0
	 * @param arg1
	 */
	public Variable(String arg0, List<Domain> arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 * @param domain
	 */
	public Variable(String name, Domain domain) {
		super(name, domain);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see fol.Term#setDomain(java.util.List)
	 */
	@Override
	void setDomain(List<Domain> domain) {
		for(Domain d: domain) {
			d.add(this);
		}
		this.domain = domain;
	}
	
	/**
	 * Check if the Constant c belongs to this Variable Domain.
	 */
	public boolean inDomain(Constant c) {
		for (Domain d : this.getDomain()) {
			if (Domain.in(c, d)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Return a set with all Constants belonging to the Domain of this Variable.
	 */
	public Set<Constant> getConstants() {
		if (this.getDomain().size() == 1) {
			return this.getDomain().get(0);
		} else {
			Set<Constant> set = new HashSet<Constant>();
			for (Domain d : this.getDomain()) {
				set.addAll(d);
			}
			return set;
		}
	}
	

	@Override
	public int compareTo(Variable o) {
		return this.toString().compareTo(o.toString());
	}

}
