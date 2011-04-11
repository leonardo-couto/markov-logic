package math;

public interface Optimizer {
	
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
	 * Finds the argument that maximizes the function <code>function</code>.
	 * 
	 * @param x Initial values to <code>function</code>
	 * @param function The function to maximize and the gradient to the function
	 * @return The parameters that maximize the function.
	 */
	public double[] max(double[] x, FunctionAndGradient function) throws OptimizationException;

	
	/**
	 * Finds the argument that minimizes the function <code>function</code>.
	 * 
	 * @param x Initial values to <code>function</code>
	 * @param function The function to minimize
	 * @param gradient The gradient to the function
	 * @return The parameters that minimize the function.
	 */
	public double[] min(double[] x, RnToRFunction function, RnToRnFunction gradient) throws OptimizationException;

	/**
	 * Finds the argument that minimizes the function <code>function</code>.
	 * 
	 * @param x Initial values to <code>function</code>
	 * @param function The function to minimize and the gradient to the function
	 * @param gradient The gradient to the function
	 * @return The parameters that minimize the function.
	 */
	public double[] min(double[] x, FunctionAndGradient function) throws OptimizationException;

	
	public void setPrecision(double eps);
	
	/**
	 * @return An instance of Optimizer with the same parameters
	 * as the one that copy was called.
	 */
	public Optimizer copy();
	
	/**
	 * After a successful call to min or max, it returns the 
	 * arguments that maximize/minimize the function. It is 
	 * the same value that was returned by the min/max method.
	 * @return An array with the arguments returned in the last
	 * call to min/max method. Or an empty array if these methods
	 * were never called.
	 */
	public double[] getArgs();
	
	/**
	 * After a successful call to min or max, it returns the 
	 * value of the function with the optimized args.
	 * @return the value of the function with arguments returned in 
	 * the last call to min/max method. Or NaN if these methods
	 * were never called.
	 * 
	 */
	public double getValue();
	
}
