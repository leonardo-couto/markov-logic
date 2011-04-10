package stat.convergence;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.Variance;

public class SequentialConvergenceTester implements SequentialTester {

	protected final Mean mean;
	protected final Variance variance;
	private boolean converged;
	private boolean convergedOnce;
	private final ConvergenceTester tester;
	private int count;
	private int max;
	private boolean hasLimit;

	public SequentialConvergenceTester(double confidenceLevel, double precision) {
		this.tester = ConvergenceTester.getTester(confidenceLevel, precision);
		this.mean = new Mean();
		this.variance = new Variance();
		this.converged = false;
		this.convergedOnce = false;
		this.count = 0;
		this.max = -1;
		this.hasLimit = false;
	}

	@Override
	public boolean hasConverged() {
		return this.testConvergence();
	}

	@Override
	public boolean increment(double next) {
		this.count++;
		this.mean.increment(next);
		this.variance.increment(next);
		return this.testConvergence();
	}

	private boolean testConvergence() {
		if (this.hasLimit && this.max <= this.count) {
			return true;
		}
		// need to converge twice before setting this.converged = true;
		if (tester.hasConverged(this.variance(), this.mean(), this.getCount())) {
			this.converged = this.convergedOnce ? true : false;
			this.convergedOnce = true;
			return this.converged;
		}
		this.convergedOnce = false;
		return false;
	}


	@Override
	public boolean evaluate(double[] values) {
		if (this.hasLimit && this.max <= values.length) {
			return true;
		}
		this.count = values.length;
		this.mean.evaluate(values);
		this.variance.evaluate(values);
		if (tester.hasConverged(this.variance(), this.mean(), this.getCount())) {
			this.convergedOnce = true;
			this.converged = true;
		}
		return this.converged;
	}

	@Override
	public void clear() {
		this.count = 0;
		this.mean.clear();
		this.variance.clear();
		this.converged = false;
		this.convergedOnce = false;
	}
	
	/**
	 * Returns the current value of the mean.
	 * @return value of the mean, <code>Double.NaN</code> if it
	 * has been just instantiated.
	 */
	@Override
	public double mean() {
		return this.mean.getResult();
	}

	/**
	 * Returns the current value of the variance.
	 * @return value of the variance, <code>Double.NaN</code> if it
	 * has been just instantiated.
	 */
	@Override
	public double variance() {
		return this.variance.getResult();
	}
	
	@Override
	public int getCount() {
		return this.count;
	}
	
	@Override
	public double getPrecision() {
		return this.tester.getPrecision();
	}

	@Override
	public double getConfidenceLevel() {
		return this.tester.getConfidenceLevel();
	}
	
	@Override
	public void setSampleLimit(int n) {
		this.max = n;
		this.hasLimit = n > -1;
	}

	@Override
	public int getSampleLimit() {
		return this.max;
	}	

	@Override
	public SequentialConvergenceTester copy() {
		SequentialConvergenceTester sct = new SequentialConvergenceTester(this.getConfidenceLevel(), this.getPrecision());
		sct.setSampleLimit(this.max);
		return sct;
	}

}