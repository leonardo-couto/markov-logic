package markovLogic.structureLearner;

import fol.Formula;
import fol.WeightedFormula;

public class ScoredFormula<T extends Formula> extends WeightedFormula<T> implements Comparable<ScoredFormula<?>> {
	
	public ScoredFormula(T formula, double score, double weight) {
		super(formula, weight);
		this.score = score;
	}
	
	final double score;
	
	@Override
	public int compareTo(ScoredFormula<?> o) {
		return Double.compare(this.score, o.score);
	}
	
	public double getScore() {
		return this.score;
	}
	
}