package math;

/**
 * <p>Wrap a function and its gradient in a L1 norm penalized 
 * function.</p>
 * 
 * <p>Uses an differentiable approximation on the L1 norm as in
 * Schmidt et al. paper of 2007 "Fast Optimization Methods for L1 
 * Regularization: A Comparative Study and Two New Approaches"</p>
 */
public class L1RegularizedFunction implements DifferentiableFunction {
	
	private final ScalarFunction function;
	private final VectorFunction gradient;
//	private final ArrayList<Double> l1Weights;
//	private boolean constant;
	private double weight;
	private int start;
	private int end;
	
	public L1RegularizedFunction(ScalarFunction function, VectorFunction gradient) {
		this.function = function;
		this.gradient = gradient;
//		this.l1Weights = new ArrayList<Double>();
		this.start = 0;
		this.end = Integer.MAX_VALUE;
//		this.constant = false;
		this.weight = 0;
	}
	
	public L1RegularizedFunction(DifferentiableFunction function) {
		this(function, function);
	}
	
	protected L1RegularizedFunction(L1RegularizedFunction old, DifferentiableFunction function) {
		this.function = function;
		this.gradient = function;
		this.start = old.start;
		this.end = old.end;
		this.weight = old.weight;
	}
	
	/**
	 * @return the value attributed in {@link #setStart}
	 */
	public int getStart() {
		return this.start;
	}
	
	/**
	 * <p>Set the position from where the weights will be applied. 
	 * Arguments before this position will have a L1 weight equals to zero.</p>
	 * 
	 * <p>Zero-based position, i.e. for excluding the first element call
	 * this function with argument One.</p>
	 * 
	 * @param i position from where L1 weights will be applied. 
	 * Default value is zero
	 */
	public void setStart(int i) {
		this.start = i;
	}

	/**
	 * @return the value attributed in {@link #setEnd}
	 */
	public int getEnd() {
		return this.end;
	}
	
	/**
	 * <p>Set the position from where the weights will stop being applied. 
	 * Arguments after this position will have a L1 weight equals to zero.</p>
	 * 
	 * <p>Zero-based position, i.e. for excluding all but first element call
	 * this function with argument One.</p>
	 * 
	 * @param i position from where L1 weights will be applied. 
	 * Default value is Integer.MAX_INT
	 */
	public void setEnd(int i) {
		this.end = i;
	}
	
	/**
	 * <p>Set a constant L1 weight. This weight will be used for
	 * all parameters in the {@link #getStart} to {@link #getEnd} interval.</p>
	 * <p>C should be positive for functions that have a global minimum and 
	 * negative for functions with a global maximum.</p>
	 * @param c 
	 * @return
	 */
	public L1RegularizedFunction setConstantWeight(double c) {
//		this.constant = true;
		this.weight = c;
		return this;
	}
	
	private double l1Norm(double x) {
		return Math.abs(x);
	}
	
	private double l1Grad(double x, double grad) {
		if (this.weight == 0) { return 0; }
		if (x == 0) {
			double absweight = Math.abs(this.weight);
			if (Math.abs(grad) > absweight) {
				return grad*this.weight > 0 ? -1 : +1;
			} else {
				return -grad/this.weight;
			}
		}
		return x > 0 ? 1 : -1;
	}

	@Override
	public double f(double[] x) {
		int end = Math.min(x.length, this.end);
		double l1 = 0;
		for (int i = start; i < end; i++) {
			l1 += this.l1Norm(x[i]);
		}
		return this.function.f(x) + this.weight*l1;
	}

	@Override
	public int lengthInput() {
		return this.function.lengthInput();
	}

	@Override
	public double[] g(double[] x) {
		int end = Math.min(x.length, this.end);
		double[] grad = this.gradient.g(x);
		for (int i = start; i < end; i++) {
			grad[i] += this.weight * this.l1Grad(x[i], grad[i]);
		}
		return grad;
	}

	@Override
	public int lengthOutput() {
		return this.gradient.lengthOutput();
	}
	

}
