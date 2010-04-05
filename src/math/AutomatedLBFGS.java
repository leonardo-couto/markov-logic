package math;

import java.util.Arrays;


import util.Util;

public class AutomatedLBFGS extends LBFGS {
	
	public int m;
	public int[] iprint;
	public double eps;	
	public static final double xtol = Util.machinePrecision();
	
	public AutomatedLBFGS() {
		super();
		this.m = 4; // 4 <= m <= 7
		this.iprint = new int[2];
		iprint[0] = -1;
		iprint[1] = 3;
		eps = 0.01; // Precision of solution.
	}

	public double[] lbfgs(double[] x, RnToRFunction function, RnToRnFunction gradient) throws ExceptionWithIflag {
		int n = x.length;
		double[] out = Arrays.copyOf(x, n);
		boolean diagco = false;
		double[] diag = new double[n];
		int[] iflag = {0};
		
		super.lbfgs(n, m, out, function.f(out), gradient.f(out), diagco, diag, iprint, eps, xtol, iflag);
		
		while (iflag[0] == 1) {
			super.lbfgs(n, m, out, function.f(out), gradient.f(out), diagco, diag, iprint, eps, xtol, iflag);			
		}

		return out;
	}
	
	public double[] maxLbfgs(double[] x, RnToRFunction function, RnToRnFunction gradient) throws ExceptionWithIflag {
		int n = x.length;
		double[] out = Arrays.copyOf(x, n);
		boolean diagco = false;
		double[] diag = new double[n];
		int[] iflag = {0};
		
		super.lbfgs(n, m, out, -1.0*function.f(out), changeSign(gradient.f(out)), diagco, diag, iprint, eps, xtol, iflag);
		
		while (iflag[0] == 1) {
			super.lbfgs(n, m, out, -1.0*function.f(out), changeSign(gradient.f(out)), diagco, diag, iprint, eps, xtol, iflag);			
		}

		return out;
	}
	
	private double[] changeSign(double[] d) {
		for (int i = 0; i < d.length; i++) {
			d[i] = -1.0d * d[i];
		}
		return d;
	}
}
