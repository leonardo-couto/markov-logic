package stat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Histogram {

	public final int dimension;
	public final double[] nmin;
	public final double[] nmax;
	private final List<double[]> data;

	public Histogram(int dimension, double[] nmin, double[] nmax, List<double[]> data) {
		this.dimension = dimension;
		this.nmax = nmax;
		this.nmin = nmin;
		this.data = data;
	}


	/**
	 * One dimensional histogram
	 * @param nmax
	 * @param nmin
	 * @param data
	 */
	public Histogram(double nmin, double nmax, double[] data) {
		this.dimension = 1;
		this.nmax = new double[1];
		this.nmin = new double[1];
		this.data = new ArrayList<double[]>(data.length);
		this.nmax[0] = nmax;
		this.nmin[0] = nmin;
		for (double d : data) {
			this.data.add(new double[] { d });
		}
	}

	//	private void checkDataBounds(double[] point) throws IllegalArgumentException {
	//		for (int i = 0; i < point.length; i++) {
	//			if ((Double.compare(point[i], this.nmin[i]) < 0) || 
	//					(Double.compare(point[i], this.nmax[i]) > 0)) {
	//				throw new IllegalArgumentException("Data outside specified range [" + 
	//						this.nmin[i] + ", " + this.nmax[i] + "]. d = " + point[i]);
	//			}
	//		}
	//	}

	/**
	 * For one dimensional histogram
	 * @param nbins
	 */
	public int[] getHistogram(int nbins) {
		if (this.dimension != 1) {
			throw new IllegalArgumentException("Data has more than one dimension.");
		}
		int[] out = new int[nbins];
		Arrays.fill(out, 0);
		double dbin = (this.nmax[0] - this.nmin[0])/((double) nbins);

		for (double[] point : this.data) {
			int idx = Math.min((int) Math.floor(((point[0] - this.nmin[0])/dbin)), nbins-1);
			out[idx]++;
		}

		return out;
	}

	/**
	 * @param nbins
	 */
	public Object[] getHistogram(int[] nbins) {
		if (nbins.length != this.dimension) {
			throw new IllegalArgumentException("Dimensions do not match");
		}
		Object[] out = (nbins.length > 1) ? new Object[nbins[0]] : new Object[1];
		Object[][] aux = new Object[1][];
		aux[0] = out;
		for (int i = 1; i < nbins.length -1; i++) {
			aux = deepInitialize(aux, nbins[i]);
		}
		deepInitializeInt(aux, nbins[nbins.length-1]);

		double[] dbin = new double[nbins.length];
		for (int i = 0; i < nbins.length; i++) {
			dbin[i] = (this.nmax[i] - this.nmin[i])/((double) nbins[i]);
		}

		for (double[] point : this.data) {
			Object[] counts = out;
			int[] data = null;
			for (int i = 0; i < point.length; i++) {
				// find this value interval index
				int idx = Math.min((int) Math.floor(((point[i] - this.nmin[i])/dbin[i])), nbins[i]-1);
				if (i < point.length -2) { // Z
					counts = (Object[]) counts[idx];
				} else if (i == point.length -2) { // X
					data = (int[]) counts[idx];
				} else if (i == point.length -1) { // Y
					data[idx]++;
				}
			}
		}

		return out;
	}

	@SuppressWarnings("unused")
	private static void recursiveDeepInitialize(Object[] oArray, int n) {
		if (oArray[0] == null) {
			for (int i = 0; i < oArray.length; i++) {
				oArray[i] = new Object[n];
			}
		} else {
			for (Object o : oArray) {
				recursiveDeepInitialize((Object[]) o, n); 
			}
		}
	}

	private static Object[][] deepInitialize(Object[][] oArray, int n) {
		Object[][] out = new Object[oArray.length*oArray[0].length][];
		int idx = 0;
		for (int i = 0; i < oArray.length; i++) {
			for (int j = 0; j < oArray[0].length; j++) {
				Object[] o = new Object[n];
				oArray[i][j] = o;
				out[idx] = o;
				idx++;
			}
		}
		return out;
	}

	//@SuppressWarnings("unused")
	private static void recursiveDeepInitializeInt(Object[] oArray, int n) {
		if (oArray[0] == null) {
			for (int i = 0; i < oArray.length; i++) {
				int[] iArray = new int[n];
				Arrays.fill(iArray, 0);
				oArray[i] = iArray;
			}
		} else {
			for (Object o : oArray) {
				recursiveDeepInitializeInt((Object[]) o, n); 
			}
		}
	}

	private static void deepInitializeInt(Object[][] oArray, int n) {
		for (int i = 0; i < oArray.length; i++) {
			for (int j = 0; j < oArray[0].length; j++) {
				int[] iArray = new int[n];
				Arrays.fill(iArray, 0);
				oArray[i][j] = iArray;
			}
		}
	}

}
