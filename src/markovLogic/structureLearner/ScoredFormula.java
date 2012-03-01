package markovLogic.structureLearner;

import markovLogic.WeightedFormula;
import fol.Formula;

public class ScoredFormula extends WeightedFormula implements Comparable<ScoredFormula> {
	
	public ScoredFormula(Formula clause, double score, double weight) {
		super(clause, weight);
		this.score = score;
	}
	
	final double score;
	
	@Override
	public int compareTo(ScoredFormula o) {
		return Double.compare(this.score, o.score);
	}
	
	public double getScore() {
		return this.score;
	}
	
}