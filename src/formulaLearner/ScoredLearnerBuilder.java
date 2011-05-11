package formulaLearner;

import java.util.Collection;

import weightLearner.WeightLearner;

import fol.Atom;

public interface ScoredLearnerBuilder extends FormulaLearnerBuilder {

	@Override
	public ScoredLearner build();
	
	@Override
	public ScoredLearnerBuilder setAtoms(Collection<Atom> atoms);
	
	public ScoredLearnerBuilder setWeightLearner(WeightLearner wLearner);
	
	public WeightLearner getWeightLearner();

	
}
