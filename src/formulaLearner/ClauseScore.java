package formulaLearner;

import markovLogic.WeightedFormula;
import fol.Formula;

public class ClauseScore extends WeightedFormula implements Comparable<ClauseScore> {
	
	public ClauseScore(Formula clause, double score, double weight) {
		super(clause, weight);
		this.score = score;
	}
	
	final double score;
	
	@Override
	public int compareTo(ClauseScore o) {
		return -1 * Double.compare(this.score, o.score);
	}

	
}