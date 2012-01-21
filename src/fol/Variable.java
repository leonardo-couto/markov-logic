package fol;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Variable extends Term implements Comparable<Variable> {
	
	private List<Constant> constants;
	private final Random r;
	private int size;
	
	/**
	 * @param name
	 * @param domain
	 */
	public Variable(String name, Domain domain) {
		super(name, domain);
		this.r = new Random();
		this.size = -1;
	}
	
	/**
	 * Keeps the Constants list updated
	 */
	private void refreshConstants() {
		this.constants = new ArrayList<Constant>(this.domain);
		this.size = this.constants.size();
	}

	/* (non-Javadoc)
	 * @see fol.Term#setDomain(java.util.List)
	 */
	@Override
	void setDomain(Domain domain) {
		domain.add(this);
	}
	
	/**
	 * Check if the Constant c belongs to this Variable Domain.
	 */
	public boolean inDomain(Constant c) {
		return Domain.in(c, this.domain);
	}
	
	/**
	 * Return a List with all Constants belonging to the domain of this Variable.
	 */
	public List<Constant> getConstants() {
		if (this.size != this.domain.size()) {
			this.refreshConstants();
		}
		return this.constants;
	}
	
	public Constant getRandomConstant() {
		if (this.size != this.domain.size()) {
			this.refreshConstants();
		}
		return this.constants.get(r.nextInt(size));
	}
	
	@Override
	public int compareTo(Variable o) {
		return this.toString().compareTo(o.toString());
	}

}
