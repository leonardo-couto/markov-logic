package math;

import edu.stanford.nlp.optimization.DiffFunction;

public class OptimizableWrapperB implements DiffFunction {
	
	private final RnToRFunction function;
	private final RnToRnFunction gradient;
	private final boolean invert;
	
	public OptimizableWrapperB(FunctionAndGradient function, boolean invert) {
		this(function, function, invert);
	}
	
	public OptimizableWrapperB(RnToRFunction function, RnToRnFunction gradient, boolean invert) {
		this.function = function;
		this.gradient = gradient;
		this.invert = invert;
	}

	@Override
	public double valueAt(double[] x) {
		return this.invert ? -1.0 * this.function.f(x) : this.function.f(x);
	}

	@Override
	public int domainDimension() {
		return this.function.lengthInput();
	}

	@Override
	public double[] derivativeAt(double[] x) {
		return invert ? changeSign(this.gradient.g(x)) : this.gradient.g(x);
	}
	
	private static double[] changeSign(double[] d) {
		for (int i = 0; i < d.length; i++) {
			d[i] = -1.0d * d[i];
		}
		return d;
	}



}
