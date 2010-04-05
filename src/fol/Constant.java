package fol;

import java.util.List;

/**
 * @author Leonardo Castilho Couto
 *
 */
public class Constant extends Term {

	/**
	 * @param arg0
	 * @param arg1
	 */
	public Constant(String arg0, List<Domain> arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 * @param domain
	 */
	public Constant(String name, Domain domain) {
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
			Domain parent = d.getParent();
			while (parent != null) {
				parent.add(this);
				parent = parent.getParent();
			}
		}
		this.domain = domain;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
