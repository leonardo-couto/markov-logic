package formulaLearner;

import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import math.Optimizer;
import math.LBFGS.ExceptionWithIflag;
import util.MyException;
import weightLearner.Score;
import fol.Formula;

public class TestFormula implements Runnable {
	
	private final double epslon = 0.5;
	private Score wscore;
	private Formula[] formulas;
	private Optimizer optmizer;
	private Vector<ClauseScore> candidates;
	private Vector<ClauseScore> bestClauses;
	private FormulaArray fArray;
	private double[] weights;
	private final double score;
	public final Thread t;
	private CountDownLatch done;
	
	public TestFormula(Score wscore, FormulaArray fArray, 
			Vector<ClauseScore> candidates, Vector<ClauseScore> bestClauses,
			double[] weights, double score, Optimizer optimizer,
			CountDownLatch done) {
		this.wscore = wscore.copy();
		this.fArray = fArray;
		this.candidates = candidates;
		this.bestClauses = bestClauses;
		this.weights = Arrays.copyOf(weights, weights.length);
		this.score = score;
		this.done = done;
		this.optmizer = optimizer.copy();
		this.t = new Thread(this);
		this.t.start();
	}

	@Override
	public void run() {
		try { 
			while (true) {
				this.formulas = this.fArray.getElements();
				if (this.formulas == null || Thread.interrupted()) {
					return;
				}
				double bestscore = this.fArray.getScore(); // TODO: REMOVE!
				for (Formula f : this.formulas) {

					if (!this.wscore.addFormula(f)) { continue; }
					double newScore = 0;
					double learnedWeight;
					double[] nweights;
					try {
						nweights = this.optmizer.max(this.weights, this.wscore);
						learnedWeight = nweights[nweights.length -1]; 
						newScore = this.optmizer.getValue();
					} catch (ExceptionWithIflag e) {
						this.wscore.removeFormula(f);
						continue;
					} catch (Exception e) {
						throw new MyException(e);
						//e.printStackTrace();
						//System.out.println(f + "\n" + Arrays.toString(this.weights) + "\n" + this.wscore.getFormulas());
						//System.exit(1);
						//continue;
					}
					this.wscore.removeFormula(f);

					if (Double.compare(newScore, this.score+0.01) > 0 && Double.compare(Math.abs(learnedWeight), epslon) > 0) {
						if (Double.compare(newScore, bestscore) > 0) {  // TODO: Remove
							System.out.println(Double.toString(newScore) + " " + f + "\n" + Arrays.toString(nweights));
							bestscore = newScore;
						}
						this.bestClauses.add(new ClauseScore(f, this.score - newScore, learnedWeight, nweights));
					} else {
						this.candidates.add(new ClauseScore(f, this.score - newScore, learnedWeight, nweights));
					}
				}
				this.fArray.setScore(bestscore);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			this.done.countDown();
		}
	}
}
