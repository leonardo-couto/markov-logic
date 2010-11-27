package stat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Histogram {

	private final List<Double> data;
	public final double nmin;
	public final double nmax;

	/*
	 * Assumes all data lies in the [nmin,nmax] range
	 */
	public Histogram(double nmin, double nmax) {
		this.nmin = nmin;
		this.nmax = nmax;
		this.data = new ArrayList<Double>();
	}

	/*
	 * Assumes all data lies in the [0,1] range
	 */
	public Histogram() {
		this.nmin = 0;
		this.nmax = 1;
		this.data = new ArrayList<Double>();
	}


	public void addAll(double[] data) {
		List<Double> dataList = new ArrayList<Double>(data.length);
		for (double d : data) {
			dataList.add(d);
		}
		this.data.addAll(dataList);
	}

	public void addAll(List<Double> data) {
		this.data.addAll(data);
	}


	public void add(Double data) {
		this.data.add(data);
	}

	/**
	 * For one dimensional histogram
	 * @param nbins
	 */
	public int[] getHistogram(int nbins) {
		int[] out = new int[nbins];
		Arrays.fill(out, 0);
		double dbin = (this.nmax - this.nmin)/((double) nbins);

		for (double point : this.data) {
			int idx = Math.min((int) Math.floor(((point - this.nmin)/dbin)), nbins-1);
			out[idx]++;
		}

		return out;
	}

}
