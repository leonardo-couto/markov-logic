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
	
	public boolean addFormula(Formula f);
	
	public boolean addFormulas(List<Formula> formulas);
	
	public boolean removeFormula(Formula f);
	
	public double getScore(double[] weights);
	
	public Score copy();
	
	public List<Formula> getFormulas();

}
