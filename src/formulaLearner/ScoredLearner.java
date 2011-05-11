package formulaLearner;

import weightLearner.WeightLearner;

public interface ScoredLearner extends FormulaLearner {
	
	public WeightLearner getWeightLearner();
	public ScoredLearner copy();

}
