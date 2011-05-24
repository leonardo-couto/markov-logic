package fol;

import java.util.List;

/**
 * @author Leonardo Castilho Couto
 *
 */
public class Function extends Term {

	/**
	 * @param arg0
	 * @param arg1
	 */
	public Function(String arg0, List<Domain> arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 * @param domain
	 */
	public Function(String name, Domain domain) {
		super(name, domain);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see fol.Term#setDomain(java.util.List)
	 */
	@Override
	void setDomain(List<Domain> domain) {
		// TODO Auto-generated method stub

	}

}
