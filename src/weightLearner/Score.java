package weightLearner;

import java.util.List;

import math.RnToRFunction;
import math.RnToRnFunction;

import fol.Formula;

/**
 * Given a list of formulas, computes the score given 
 * the function's weights and its first derivative.
 * 
 * @author Leonardo Castilho Couto
 *
 */
public interface Score extends RnToRFunction, RnToRnFunction {
	
	public void addFormula(Formula f);
	
	public void addFormulas(List<Formula> formulas);
	
	public boolean removeFormula(Formula f);
	
	public double getScore(double[] weights);

}
