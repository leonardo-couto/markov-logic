package math;

public interface MaxFinder {
	
	/**
	 * Finds the argument that maximizes the function <code>function</code>.
	 * 
	 * @param x Initial values to <code>function</code>
	 * @param function The function to maximize
	 * @param gradient The gradient to the function
	 * @return The parameters that maximize the function.
	 */
	public double[] max(double[] x, RnToRFunction function, RnToRnFunction gradient) throws OptimizationException;
	
	/**
	 * Finds the argument that minimizes the function <code>function</code>.
	 * 
	 * @param x Initial values to <code>function</code>
	 * @param function The function to minimize
	 * @param gradient The gradient to the function
	 * @return The parameters that minimize the function.
	 */
	public double[] min(double[] x, RnToRFunction function, RnToRnFunction gradient) throws OptimizationException;

}
