package formulaLearner;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

import math.LBFGS.ExceptionWithIflag;
import math.Optimizer;
import util.MyException;
import weightLearner.Score;
import fol.Formula;

public class TestFormula implements Runnable {
	
	private final Score score;
	private final Optimizer optmizer;
	private final double[] lastWeights;
	private final double lastScore;
	private final CountDownLatch done;
	private final BlockingQueue<Formula> candidates;
	private final Queue<ClauseScore> scoredCandidates;
	private final double epslon; // only accepts formulas with weight > epslon

	
	public TestFormula(Score score, Optimizer optimizer, double[] lastWeights, 
			double lastScore, CountDownLatch done, BlockingQueue<Formula> candidates, 
			Queue<ClauseScore> scoredCandidates, double epslon) {
		this.score = score.copy();
		this.optmizer = optimizer.copy();
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
				
				if (!this.score.addFormula(f)) { continue; }
				
				double newScore;
				double learnedWeight;
				double[] nweights;
				try {
					nweights = this.optmizer.max(this.lastWeights, this.score);
					learnedWeight = nweights[nweights.length -1]; 
					newScore = this.optmizer.getValue();
				} catch (ExceptionWithIflag e) {
					this.score.removeFormula(f);
					continue;
				} catch (Exception e) {
					throw new MyException(e);
				}
				
				this.score.removeFormula(f);

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
