package fol;

public class Constant extends Term {

	/**
	 * @param name
	 * @param domain
	 */
	public Constant(String name, Domain domain) {
		super(name, domain);
	}

	/* (non-Javadoc)
	 * @see fol.Term#setDomain(java.util.List)
	 */
	@Override
	void setDomain(Domain domain) {
		domain.add(this);
		Domain parent = domain.getParent();
		while (parent != null) {
			parent.add(this);
			parent = parent.getParent();
		}
	}

}
