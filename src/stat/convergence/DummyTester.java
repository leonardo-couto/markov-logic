package stat.convergence;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.Variance;

/**
 * Converges when the number of elements tested is equal or greater then 
 * the constructor <code>max</code> param. 
 * If max < 0, <code>hasConverged</code> always returns false,
 * If max == 0 always true;
 */
public class DummyTester implements SequentialTester {

	protected final Mean mean;
	protected final Variance variance;
	private int max;
	private boolean hasLimit;
	private int count;
	
	public DummyTester(int max) {
		this.mean = new Mean();
		this.variance = new Variance();
		this.max = max;
		this.hasLimit = (max > -1);
		this.count = 0;
	}
	
	private boolean hasConverged(int n) {
		return this.hasLimit && n < this.max;
	}

	@Override
	public boolean hasConverged() {
		return this.hasConverged(this.count);
	}
	
	@Override
	public boolean increment(double next) {
		this.count++;
		this.mean.increment(next);
		this.variance.increment(next);
		return this.hasConverged();
	}

	@Override
	public boolean evaluate(double[] values) {
		return this.hasConverged(values.length);
	}

	@Override
	public void clear() {
		this.count = 0;
		this.mean.clear();
		this.variance.clear();
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
		return 0.0d;
	}

	@Override
	public double getConfidenceLevel() {
		return 0.0d;
	}

	@Override
	public void setSampleLimit(int n) {
		this.max = n;
		this.hasLimit = (n > -1);
	}

	@Override
	public int getSampleLimit() {
		return this.max;
	}
	
	@Override
	public DummyTester copy() {
		return new DummyTester(this.max);
	}

}