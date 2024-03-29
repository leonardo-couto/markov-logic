package stat.convergence;

import java.util.Arrays;


public class ShortMemoryConvergenceTester extends SequentialConvergenceTester {
	
	private final int memory;
	private final double[] data;
	private double mean;
	private double variance;
	private int index;

	public ShortMemoryConvergenceTester(double confidenceLevel, double precision, int memory) {
		super(confidenceLevel, precision);
		this.memory = memory;
		this.data = new double[memory];
		this.mean = 0;
		this.variance = 0;
		this.index = 0;
	}
	
	@Override
	public boolean increment(double next) {
		if (this.index < this.memory) {
			super.mean.increment(next);
			super.variance.increment(next);
			this.mean = super.mean();
			this.variance = super.variance();
			this.data[this.index] = next;
		} else {
			int idx = this.index % this.memory;
			double removed = data[idx];
			data[idx] = next;
			this.mean = this.mean + (next-removed)/this.memory;
			this.variance = super.variance.evaluate(this.data, this.mean);
		}
		this.index++;
		return super.hasConverged();
	}

	@Override
	public boolean evaluate(double[] values) {
		if (values.length > this.memory) {
			values = Arrays.copyOfRange(values, values.length-memory, values.length);
			this.index = this.memory;
		} else {
			this.index = values.length;
		}
		boolean b = super.evaluate(values);
		this.mean = super.mean();
		this.variance = super.variance();
		return b;
	}

	@Override
	public void clear() {
		super.clear();
		this.mean = 0;
		this.variance = 0;
		this.index = 0;
	}

	@Override
	public double mean() {
		return this.mean;
	}

	@Override
	public double variance() {
		return this.variance;
	}
	
	@Override
	public int getCount() {
		return Math.min(index, memory);
	}
}