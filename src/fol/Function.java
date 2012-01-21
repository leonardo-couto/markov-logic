package fol;

/**
 * @author Leonardo Castilho Couto
 *
 */
public class Function extends Term {

	/**
	 * @param name
	 * @param domain
	 */
	public Function(String name, Domain domain) {
		super(name, domain);
	}

	/* (non-Javadoc)
	 * @see fol.Term#setDomain(java.util.List)
	 */
	@Override
	void setDomain(Domain domain) {
		domain.add(this);
	}

}
