package formulaLearner;

import markovLogic.WeightedFormula;
import fol.Formula;

public class ClauseScore extends WeightedFormula implements Comparable<ClauseScore> {
	
	public ClauseScore(Formula clause, double score, double weight, double[] weights) {
		super(clause, weight);
		this.score = score;
		this.weights = weights;
	}
	
	final double score;
	final double[] weights; // TODO: REMOVER!!!!!!!!!!!!!!!!
	
	@Override
	public int compareTo(ClauseScore o) {
		return Double.compare(this.score, o.score);
	}

	
}