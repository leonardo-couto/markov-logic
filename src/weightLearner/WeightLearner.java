/**
 * 
 */
package weightLearner;

import math.MaxFinder;
import math.OptimizationException;

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
  
}
