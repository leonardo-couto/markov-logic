package structureLearner;

import java.util.Queue;
import java.util.concurrent.CountDownLatch;

import math.OptimizationException;
import weightLearner.WeightLearner;
import fol.Formula;

/**
 * Scores a queue of candidate Formulas.
 * Scored formula are pushed into scoredCandidate queue.
 */
public class FormulaTester implements Runnable {
	
//	public static PrintStream out = System.out;
	
	private final WeightLearner learner;
	private final double[] lastWeights;
	private final double lastScore;
	private final CountDownLatch done;
	private final Queue<? extends Formula> candidates;
	private final Queue<ScoredFormula> scoredCandidates;
	private final double epslon; // only accepts formulas with weight > epslon

	
	public FormulaTester(WeightLearner learner, double[] lastWeights, 
			double lastScore, CountDownLatch done, Queue<? extends Formula> candidates, 
			Queue<ScoredFormula> scoredCandidates, double epslon) {
		this.learner = learner.copy();
		this.lastWeights = lastWeights;
		this.lastScore = lastScore;
		this.done = done;
		this.candidates = candidates;
		this.scoredCandidates = scoredCandidates;
		this.epslon = epslon;
	}

	@Override
	public void run() {
		try { 
			while (true) {
				Formula f = this.candidates.poll();
				if (f == null) {
					return;
				}
				
				if (!this.learner.addFormula(f)) { continue; }
				
				double newScore;
				double learnedWeight;
				double[] nweights;
				try {
					nweights = this.learner.learn(this.lastWeights);
					learnedWeight = nweights[nweights.length -1]; 
					newScore = this.learner.score();
				} catch (Exception e) {
					// TODO: logar a excecao
					// algum outro erro alem de otimizacao
					// que talvez nao aconteceria para outra formula
					// que vale a pena estar aqui?
//					e.printStackTrace();
					this.learner.removeFormula(f);
					continue;
				}
				
				this.learner.removeFormula(f);
				System.out.println(String.format("formula: %s, weight: %s, score: %s", f, learnedWeight, newScore-this.lastScore));

				if (Double.compare(newScore, this.lastScore) > 0 && Double.compare(Math.abs(learnedWeight), epslon) > 0) {
					this.scoredCandidates.offer(new ScoredFormula(f, newScore - this.lastScore, learnedWeight));
				}
			}
		} finally {
			this.done.countDown();
		}
	}
	
}
