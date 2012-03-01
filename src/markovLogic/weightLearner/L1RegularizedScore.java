package markovLogic.weightLearner;

import java.util.List;

import math.L1RegularizedFunction;
import fol.Formula;

public class L1RegularizedScore extends L1RegularizedFunction implements Score {
	
	private final Score score;

	public L1RegularizedScore(Score score) {
		super(score);
		this.score = score;
	}
	
	private L1RegularizedScore(L1RegularizedScore old, Score score) {
		super(old, score);
		this.score = score;
	}

	@Override
	public boolean addFormula(Formula f) {
		return this.score.addFormula(f);
	}

	@Override
	public boolean addFormulas(List<? extends Formula> formulas) {
		return this.score.addFormulas(formulas);
	}

	@Override
	public boolean removeFormula(Formula f) {
		return this.score.removeFormula(f);
	}

	@Override
	public double getScore(double[] weights) {
		return super.f(weights);
	}

	@Override
	public Score copy() {
		Score copy = this.score.copy();
		return new L1RegularizedScore(this, copy);
	}

	@Override
	public List<Formula> getFormulas() {
		return this.score.getFormulas();
	}
	
	@Override
	public L1RegularizedScore setConstantWeight(double c) {
		super.setConstantWeight(c);
		return this;
	}

}
