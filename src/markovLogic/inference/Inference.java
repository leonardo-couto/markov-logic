package markovLogic.inference;

import fol.Atom;

public interface Inference {
	
	/**
	 * Returns the probability of <code>ground</code> given
	 * a <code>Set</code> of <code>Formula</code>.
	 */
	public double pr(Atom ground, Evidence evidence);

}
