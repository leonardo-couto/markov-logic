package stat;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.Variance;

/**
 * @author Leonardo Castilho Couto
 *
 * @param <T>
 */
public class SequentialConvergenceTester {

	protected final Mean mean;
	protected final Variance variance;
	private boolean converged;
	private boolean convergedOnce;
	private final ConvergenceTester tester;
	private int count;

	public SequentialConvergenceTester(double confidenceLevel, double precision) {
		this.tester = ConvergenceTester.getTester(confidenceLevel, precision);
		this.mean = new Mean();
		this.variance = new Variance();
		this.converged = false;
		this.convergedOnce = false;
		this.count = 0;
	}

	public boolean hasConverged() {
		return this.converged;
	}

	public boolean increment(double next) {
		this.count++;
		this.testConvergence(next);
		return this.converged;
	}

	private void testConvergence(double nextValue) {
		this.mean.increment(nextValue);
		this.variance.increment(nextValue);

		// need to converge twice before setting this.converged = true;
		if (tester.hasConverged(this.variance(), this.mean(), this.count)) {
			if(this.convergedOnce) {
				this.converged = true;
			} else {
				this.convergedOnce = true;
			}
		} else {
			this.convergedOnce = false;
		}
	}


	public boolean evaluate(double[] values) {
		this.count = values.length;
		this.mean.evaluate(values);
		this.variance.evaluate(values);
		if (tester.hasConverged(this.variance(), this.mean(), this.count)) {
			this.convergedOnce = true;
			this.converged = true;
		}
		return this.converged;
	}

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
	public double mean() {
		return this.mean.getResult();
	}

	/**
	 * Returns the current value of the variance.
	 * @return value of the variance, <code>Double.NaN</code> if it
	 * has been just instantiated.
	 */
	public double variance() {
		return this.variance.getResult();
	}
	

}
