package fol;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Variable extends Term {
	
	private List<Constant> constants;
	private final LocalRandom random;
	private int size;
	
	/**
	 * @param name
	 * @param domain
	 */
	public Variable(String name, Domain domain) {
		super(name, domain);
		this.constants = new ArrayList<Constant>(this.domain);
		this.size = this.constants.size();
		this.random = new LocalRandom();
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
			this.constants = new ArrayList<Constant>(this.domain);
			this.size = this.constants.size();
		}
		return this.constants;
	}
	
	public Constant getRandomConstant() {
		if (this.size != this.domain.size()) {
			this.constants = new ArrayList<Constant>(this.domain);
			this.size = this.constants.size();
		}
		Random r = this.random.get();
		return this.constants.get(r.nextInt(this.size));
	}
	
	private static final class LocalRandom extends ThreadLocal<Random> {

		@Override
		protected Random initialValue() {
			return new Random();
		}

	}

}
