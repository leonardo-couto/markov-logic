package inference;

import java.util.Set;

import fol.Atom;
import fol.Formula;

public interface Inference {
	
	/**
	 * Returns the probability of <code>f</code> given
	 * a <code>Set</code> of <code>Formula</code>.
	 */
	public double pr(Formula f, Set<Atom> given);
	
	// TODO: probabilidade de f dado um conjunto de formulas com
	// probabilidades associadas.
	
	public double pr(Formula f);

}
