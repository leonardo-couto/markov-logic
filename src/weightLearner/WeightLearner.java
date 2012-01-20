package weightLearner;

import java.util.List;

import math.OptimizationException;
import math.Optimizer;
import fol.Formula;

public class WeightLearner {

	private final Score score;
	private final Optimizer optmizer;

	public WeightLearner(Score score, Optimizer optmizer) {
		this.score = score;
		this.optmizer = optmizer;
	}

	public boolean addFormula(Formula f) {
		return this.score.addFormula(f);
	}

	public boolean addFormulas(List<? extends Formula> formulas) {
		return this.score.addFormulas(formulas);
	}

	public WeightLearner copy() {
		Score s = this.score.copy();
		Optimizer o = this.optmizer.copy();
		return new WeightLearner(s, o);
	}

	public List<Formula> getFormulas() {
		return this.score.getFormulas();
	}

	public Score getScore() {
		return this.score;
	}

	public double[] learn(double[] initialWeights) throws OptimizationException {
		return this.optmizer.max(initialWeights, this.score);
	}

	public boolean removeFormula(Formula f) {
		return this.score.removeFormula(f);
	}

	/**
	 * After a successful call to learn, it returns the score with the optimized
	 * args.
	 * 
	 * @return the score got with the weights returned in the last call to learn
	 *         method. Or NaN if the method were never called.
	 */
	public double score() {
		return this.optmizer.getValue();
	}

	/**
	 * After a successful call learn, it returns the arguments that optimized
	 * the score. It is the same value that was returned by the learn method.
	 * 
	 * @return An array with the arguments returned in the last call to learn
	 *         method. Or an empty array if the method were never called.
	 */
	public double[] weights() {
		return this.optmizer.getArgs();
	}

}
