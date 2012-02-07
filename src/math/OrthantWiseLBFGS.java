package math;

import java.util.Arrays;
import java.util.logging.Level;

import cc.mallet.optimize.OrthantWiseLimitedMemoryBFGS;
import cc.mallet.util.MalletLogger;

public class OrthantWiseLBFGS implements Optimizer {
	
	private final double l1;
	
	private double[] lastArgs;
	private double lastValue;
	private final boolean which;
	private final OWLQNMinimizer owqn;
	
	public OrthantWiseLBFGS(double l1, boolean which) {
		this.l1 = l1;
		this.lastArgs = new double[0];
		this.lastValue = Double.NaN;
		this.which = which;
		if (which) {
			this.owqn = new OWLQNMinimizer(l1);
		} else {
			this.owqn = null;
		}
//		MalletLogger.getLogger(OrthantWiseLimitedMemoryBFGS.class.getName()).setLevel(Level.OFF);
		MalletLogger.getLogger("edu.umass.cs.mallet.base.ml.maximize.LimitedMemoryBFGS").setLevel(Level.OFF);
		MalletLogger.getLogger(OrthantWiseLimitedMemoryBFGS.class.getName()).setLevel(Level.FINEST);
		
		
		
	}

	@Override
	public double[] max(double[] x, RnToRFunction function,
			RnToRnFunction gradient) throws OptimizationException {
		int n = function.lengthInput();
		this.lastArgs = Arrays.copyOf(x, n);
		if (this.which) {
			OptimizableWrapperB wrapper = new OptimizableWrapperB(function, gradient, false);
			this.lastArgs = this.owqn.minimize(wrapper, 0.01, this.lastArgs);
			this.lastValue = wrapper.valueAt(this.lastArgs);
			return this.lastArgs;
		} else {
			OptimizableWrapper functionWrapper = new OptimizableWrapper(function, gradient);

			this.lastArgs = Arrays.copyOf(x, n);
			functionWrapper.setParameters(this.lastArgs);
			OrthantWiseLimitedMemoryBFGS owqn = new OrthantWiseLimitedMemoryBFGS(functionWrapper, l1);
			owqn.optimize();
//			LimitedMemoryBFGS lbfgs = new LimitedMemoryBFGS(functionWrapper);
//			lbfgs.optimize();
			functionWrapper.getParameters(this.lastArgs);
			this.lastValue = functionWrapper.getValue();
			return this.lastArgs;			
		}

	}

	@Override
	public double[] max(double[] x, FunctionAndGradient function)
			throws OptimizationException {
		return this.max(x, function, function);
	}

	@Override
	public double[] min(double[] x, RnToRFunction function,
			RnToRnFunction gradient) throws OptimizationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double[] min(double[] x, FunctionAndGradient function)
			throws OptimizationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPrecision(double eps) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Optimizer copy() {
		return new OrthantWiseLBFGS(this.l1, this.which);
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
