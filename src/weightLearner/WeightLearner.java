package weightLearner;

import markovLogic.MarkovLogicNetwork;
import markovLogic.WeightedFormula;
import markovLogic.WeightedFormula.FormulasAndWeights;
import math.AutomatedLBFGS;
import math.Optimizer;
import math.OptimizationException;
import util.MyException;
import weightLearner.wpll.WeightedPseudoLogLikelihood;

public class WeightLearner {

	private final Score score;
	private final Optimizer optmizer;

	public WeightLearner(Score score, Optimizer optmizer) {
		this.score = score;
		this.optmizer = optmizer;
	}

	public Score getScore() {
		return this.score;
	}

	public double[] learn(double[] initialWeights) throws OptimizationException {
		return this.optmizer.max(initialWeights, this.score);
	}

	public static MarkovLogicNetwork updateWeights(MarkovLogicNetwork mln) {
		
		FormulasAndWeights fw = WeightedFormula.toFormulasAndWeights(mln);

		Score score = new WeightedPseudoLogLikelihood(mln.getPredicates());
		score.addFormulas(fw.formulas);
		Optimizer maxFinder = new AutomatedLBFGS(0.001);
		WeightLearner wlearn = new WeightLearner(score, maxFinder);
		double weights[];
		try {
			weights = wlearn.learn(fw.weights);
		} catch (OptimizationException e) {
			throw new MyException("Fatal error while learning the MLN weights.", e);
		}
		return new MarkovLogicNetwork(WeightedFormula.toWeightedFormulas(fw.formulas, weights));
	}


}
