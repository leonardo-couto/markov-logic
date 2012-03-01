package markovLogic.weightLearner.wpll;

import fol.Atom;

/**
 * Stores the partial pseudo-log-likelihood and its gradient.
 */
public class DataPll {
	
	public static final DataPll END = new DataPll(null, 0, null);

	public final Atom grounding;
	public final double pll;
	public final double[] grad;
	
	public DataPll(Atom grounding, double pll, double[] grad) {
		this.grounding = grounding;
		this.pll = pll;
		this.grad = grad;
	}
	
}
