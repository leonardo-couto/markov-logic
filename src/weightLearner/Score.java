package weightLearner;

import java.util.List;

import math.DifferentiableFunction;

import fol.Formula;

/**
 * Given a list of formulas, computes the score given 
 * the function's weights and its first derivative.
 */
public interface Score extends DifferentiableFunction {
	
	public boolean addFormula(Formula f);
	
	public boolean addFormulas(List<? extends Formula> formulas);
	
	public boolean removeFormula(Formula f);
	
	public double getScore(double[] weights);
	
	public Score copy();
	
	public List<Formula> getFormulas();

}
