/**
 * 
 */
package weightLearner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import markovLogic.MarkovLogicNetwork;
import math.AutomatedLBFGS;
import math.MaxFinder;
import math.OptimizationException;
import util.MyException;
import fol.Formula;

/**
 * @author leonardo.couto
 * TODO: COLOCAR O SPRING E USAR ESSE CARA!
 */
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
  
  public static void updateWeights(MarkovLogicNetwork mln) {
		List<Formula> formulas = new ArrayList<Formula>(mln.size());
		double[] weights = new double[mln.size()];
		{
			int i = 0;
			for (Entry<Formula, Double> entry : mln.entrySet()) {
				formulas.add(entry.getKey());
				weights[i] = entry.getValue();
				i++;
			}
		}
		Score score = new WeightedPseudoLogLikelihood(mln.getPredicates());
		score.addFormulas(formulas);
		MaxFinder maxFinder = new AutomatedLBFGS();
		WeightLearner wlearn = new WeightLearner(score, maxFinder);
		try {
			weights = wlearn.learn(weights);
		} catch (OptimizationException e) {
			throw new MyException("Fatal error while learning the MLN weights.", e);
		}
		for (int i = 0; i < formulas.size(); i++) {
			mln.put(formulas.get(i), weights[i]);
		}
  }
  
  
}
