package formulaLearner;

import markovLogic.weightLearner.WeightLearner;

public interface ScoredLearner extends FormulaLearner {
	
	public WeightLearner getWeightLearner();
	public ScoredLearner copy();

}
