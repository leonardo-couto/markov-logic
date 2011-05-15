package formulaLearner;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

import math.LBFGS.ExceptionWithIflag;
import util.MyException;
import weightLearner.WeightLearner;
import fol.Formula;

public class TestFormula implements Runnable {
	
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
				} catch (ExceptionWithIflag e) {
					this.learner.removeFormula(f);
					continue;
				} catch (Exception e) {
					throw new MyException(e);
				}
				
				this.learner.removeFormula(f);

				System.out.println(newScore - this.lastScore + " : " + f);
				if (Double.compare(newScore, this.lastScore) > 0 && Double.compare(Math.abs(learnedWeight), epslon) > 0) {
					this.scoredCandidates.offer(new ClauseScore(f, newScore - this.lastScore, learnedWeight, nweights));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			this.done.countDown();
		}
	}
}
