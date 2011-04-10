package stat.convergence;

public interface SequentialTester {

	public boolean hasConverged();

	public boolean increment(double next);

	public boolean evaluate(double[] values);

	public void clear();
	
	/**
	 * Returns the current value of the mean.
	 * @return value of the mean, <code>Double.NaN</code> if it
	 * has been just instantiated.
	 */
	public double mean();

	/**
	 * Returns the current value of the variance.
	 * @return value of the variance, <code>Double.NaN</code> if it
	 * has been just instantiated.
	 */
	public double variance();
	
	public int getCount();
	
	public double getPrecision();

	public double getConfidenceLevel();
	
	public void setSampleLimit(int n);
	
	public int getSampleLimit();
	
	/**
	 * Creates a new instance with the same parameters as this.
	 * All the memory (mean, variance, etc..) will be cleared up.
	 */
	public SequentialTester copy();

}