package math;

import java.util.Arrays;


import util.Util;

public class AutomatedLBFGS extends LBFGS implements Optimizer {
	
	public final int m;
	public final int[] iprint;
	public double eps;	
	public static final double xtol = Util.machinePrecision();
	
	private double[] lastArgs;
	private double lastValue;
	
	public AutomatedLBFGS(double precision) {
		super();
		this.m = 4; // 4 <= m <= 7
		this.iprint = new int[2];
		iprint[0] = -1;
		iprint[1] = 3;
		this.eps = precision; // Precision of solution.
		this.lastArgs = new double[0];
		this.lastValue = Double.NaN;
	}

	public double[] min(double[] x, RnToRFunction function, RnToRnFunction gradient) throws ExceptionWithIflag {
		int n = function.lengthInput();
		double[] out = Arrays.copyOf(x, n);
		boolean diagco = false;
		double[] diag = new double[n];
		int[] iflag = {0};
		double f = function.f(out);
		double[] g = gradient.g(out);
		
		super.lbfgs(n, m, out, f, g, diagco, diag, this.iprint, eps, xtol, iflag);
		
		while (iflag[0] == 1) {
			f = function.f(out);
			g = gradient.g(out);
			super.lbfgs(n, m, out, f, g, diagco, diag, iprint, eps, xtol, iflag);			
		}

		this.lastValue = f;
		this.lastArgs = out;

		return out;
	}
	
	public double[] min(double[] x, FunctionAndGradient function) throws ExceptionWithIflag {
		return this.min(x, function, function);
	}
	
	public double[] max(double[] x, RnToRFunction function, RnToRnFunction gradient) throws ExceptionWithIflag {
		int n = function.lengthInput();
		double[] out = Arrays.copyOf(x, n);
		boolean diagco = false;
		double[] diag = new double[n];
		int[] iflag = {0};
		double f = function.f(out);
		double[] g = gradient.g(out);
		
		super.lbfgs(n, m, out, -1.0*f, changeSign(g), diagco, diag, iprint, eps, xtol, iflag);
		
		while (iflag[0] == 1) {
			f = function.f(out);
			g = gradient.g(out);
			super.lbfgs(n, m, out, -1.0*f, changeSign(g), diagco, diag, iprint, eps, xtol, iflag);			
		}
		
		this.lastValue = f;
		this.lastArgs = out;

		return out;
	}
	
	public double[] max(double[] x, FunctionAndGradient function) throws ExceptionWithIflag {
		return this.max(x, function, function);
	}
	
	@Override
	public void setPrecision(double eps) {
		this.eps = eps;
	}
	
	private static double[] changeSign(double[] d) {
		for (int i = 0; i < d.length; i++) {
			d[i] = -1.0d * d[i];
		}
		return d;
	}
	
	public AutomatedLBFGS copy() {
		return new AutomatedLBFGS(this.eps);
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
