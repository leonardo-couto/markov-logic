package math;

import cc.mallet.optimize.Optimizable;

public class OptimizableWrapper implements Optimizable.ByGradientValue {
	
	private final RnToRFunction function;
	private final RnToRnFunction gradient;
	private double[] parameters;
	
	public OptimizableWrapper(FunctionAndGradient function) {
		this(function, function);
	}
	
	public OptimizableWrapper(RnToRFunction function, RnToRnFunction gradient) {
		this.function = function;
		this.gradient = gradient;
		this.parameters = new double[0];
	}

	@Override
	public int getNumParameters() {
		return this.function.lengthInput();
	}

	@Override
	public void getParameters(double[] buffer) {
		for (int i = 0; i < this.parameters.length; i++) {
			buffer[i] = this.parameters[i];
		}
	}

	@Override
	public double getParameter(int index) {
		return this.parameters[index];
	}

	@Override
	public void setParameters(double[] params) {
		this.parameters = params;
	}

	@Override
	public void setParameter(int index, double value) {
		this.parameters[index] = value;
	}

	@Override
	public void getValueGradient(double[] buffer) {
		double[] values = this.gradient.g(this.parameters);
		for (int i = 0; i < values.length; i++) {
			buffer[i] = values[i];
		}
	}

	@Override
	public double getValue() {
		return this.function.f(this.parameters);
	}

}
