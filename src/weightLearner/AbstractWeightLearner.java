package weightLearner;

import fol.Formula;

public abstract class AbstractWeightLearner {
	// TODO: USAR ISSO!!
	
	public abstract boolean addFormula(Formula f);
	
	public abstract boolean removeFormula(Formula f);
	
	public abstract double[] weights();

	public abstract double[] score();
	
	public abstract double[] learn(double[] weights);

}
