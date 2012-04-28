package math;

import java.util.Arrays;

public class AutomatedLBFGS extends LBFGS implements Optimizer {
	
	private final double precision;
	private double[] lastArgs;
	private double lastValue;
	
	public boolean condition = true;
	
	private boolean stop(int i) {
		return condition && i > 50;
	}
	
	public AutomatedLBFGS(double precision) {
		super(4, precision, new int[] {-1, 3}, false);
		this.precision = precision;
		this.lastArgs = new double[0];
		this.lastValue = Double.NaN;
	}

	public double[] min(double[] x, ScalarFunction function, VectorFunction gradient) throws ExceptionWithIflag {
		int n = function.lengthInput();
		double[] out = Arrays.copyOf(x, n);
		double[] diag = new double[n];
		int[] iflag = {0};
		double f = function.f(out);
		double[] g = gradient.g(out);
		
		super.lbfgs(n, out, f, g, diag, iflag);
		
		while (iflag[0] == 1) {
			f = function.f(out);
			g = gradient.g(out);
			super.lbfgs(n, out, f, g, diag, iflag);			
		}

		this.lastValue = function.f(super.solution_cache);
		this.lastArgs = super.solution_cache;

		return out;
	}
	
	public double[] min(double[] x, DifferentiableFunction function) throws ExceptionWithIflag {
		return this.min(x, function, function);
	}
	
	public double[] max(double[] x, ScalarFunction function, VectorFunction gradient) throws ExceptionWithIflag {
		int n = function.lengthInput();
		double[] out = Arrays.copyOf(x, n);
		double[] diag = new double[n];
		int[] iflag = {0};
		double f = function.f(out);
		double[] g = gradient.g(out);
		
//		super.lbfgs(n, m, out, -1.0*f, changeSign(g), diagco, diag, iprint, eps, xtol, iflag);
		
		int i = 0;
		do {
			super.lbfgs(n, out, -1.0*f, changeSign(g), diag, iflag);		
			i++;
			f = function.f(out);
			g = gradient.g(out);
		} while (iflag[0] == 1 && !stop(i));
		
		this.lastValue = function.f(super.solution_cache);
		this.lastArgs = super.solution_cache;

		return out;
	}
	
	public double[] max(double[] x, DifferentiableFunction function) throws ExceptionWithIflag {
		return this.max(x, function, function);
	}
	
	private static double[] changeSign(double[] d) {
		for (int i = 0; i < d.length; i++) {
			d[i] = -1.0d * d[i];
		}
		return d;
	}
	
	public AutomatedLBFGS copy() {
		return new AutomatedLBFGS(this.precision);
	}

	@Override
	public double[] getArgs() {
		return this.lastArgs;
	}

	@Override
	public double getValue() {
		return this.lastValue;
	}
	
}
