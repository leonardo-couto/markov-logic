package formulaLearner;

import java.io.PrintStream;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

import markovLogic.weightLearner.WeightLearner;
import math.OptimizationException;
import fol.Formula;

public class TestFormula implements Runnable {
	
	public static PrintStream out = System.out;
	
	private final WeightLearner learner;
	private final double[] lastWeights;
	private final double lastScore;
	private final CountDownLatch done;
	private final BlockingQueue<Formula> candidates;
	private final Queue<ClauseScore> scoredCandidates;
	private final double epslon; // only accepts formulas with weight > epslon

	
	public TestFormula(WeightLearner learner, double[] lastWeights, 
			double lastScore, CountDownLatch done, BlockingQueue<Formula> candidates, 
			Queue<ClauseScore> scoredCandidates, double epslon) {
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
				Formula f = this.candidates.take();
				if (f == FindCandidates.END) {
					this.candidates.offer(FindCandidates.END);
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
				} catch (OptimizationException e) {
					// TODO: logar a excecao
					// algum outro erro alem de otimizacao
					// que talvez nao aconteceria para outra formula
					// que vale a pena estar aqui?
					this.learner.removeFormula(f);
					continue;
				}
				
				this.learner.removeFormula(f);

				out.println(newScore - this.lastScore + " : " + f);
				if (Double.compare(newScore, this.lastScore) > 0 && Double.compare(Math.abs(learnedWeight), epslon) > 0) {
					this.scoredCandidates.offer(new ClauseScore(f, newScore - this.lastScore, learnedWeight));
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			// TODO: apenas logar a excessao
			// nao tem porque para o algoritmo, tem?
		} finally {
			this.done.countDown();
		}
	}
	
}
