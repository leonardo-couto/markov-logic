package weightLearner;

import markovLogic.MarkovLogicNetwork;
import markovLogic.WeightedFormula;
import markovLogic.WeightedFormula.FormulasAndWeights;
import math.AutomatedLBFGS;
import math.MaxFinder;
import math.OptimizationException;
import util.MyException;

public class WeightLearner {

	private final Score score;
	private final MaxFinder optmizator;

	public WeightLearner(Score score, MaxFinder maxFinder) {
		this.score = score;
		this.optmizator = maxFinder;
	}

	public Score getScore() {
		return score;
	}

	public double[] learn(double[] initialWeights) throws OptimizationException {
		return optmizator.max(initialWeights, score, score);
	}

	public static MarkovLogicNetwork updateWeights(MarkovLogicNetwork mln) {
		
		FormulasAndWeights fw = WeightedFormula.toFormulasAndWeights(mln);

		Score score = new WeightedPseudoLogLikelihood(mln.getPredicates());
		score.addFormulas(fw.formulas);
		MaxFinder maxFinder = new AutomatedLBFGS();
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
