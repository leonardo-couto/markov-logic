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
		return this.testConvergence();
	}

	public boolean increment(double next) {
		this.count++;
		this.mean.increment(next);
		this.variance.increment(next);
		return this.testConvergence();
	}

	private boolean testConvergence() {
		// need to converge twice before setting this.converged = true;
		if (tester.hasConverged(this.variance(), this.mean(), this.getCount())) {
			this.converged = this.convergedOnce ? true : false;
			this.convergedOnce = true;
			return this.converged;
		}
		this.convergedOnce = false;
		return false;
	}


	public boolean evaluate(double[] values) {
		this.count = values.length;
		this.mean.evaluate(values);
		this.variance.evaluate(values);
		if (tester.hasConverged(this.variance(), this.mean(), this.getCount())) {
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
	
	public int getCount() {
		return this.count;
	}
	
	public double getPrecision() {
		return this.tester.getPrecision();
	}

	public double getConfidenceLevel() {
		return this.tester.getConfidenceLevel();
	}	

}