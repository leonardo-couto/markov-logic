package stat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiDimensionalHistogram {

	public final int dimension;
	public final double[] nmin;
	public final double[] nmax;
	private final List<double[]> data;

	public MultiDimensionalHistogram(int dimension, double[] nmin, double[] nmax) {
		this.dimension = dimension;
		this.nmax = nmax;
		this.nmin = nmin;
		this.data = new ArrayList<double[]>();
	}


	public MultiDimensionalHistogram(int dimension) {
		this.dimension = dimension;
		this.nmax = new double[dimension];
		this.nmin = new double[dimension];
		Arrays.fill(nmin, 0);
		Arrays.fill(nmax, 1);
		this.data = new ArrayList<double[]>();
	}


	public void addAll(List<double[]> data) {
		this.data.addAll(data);
	}

	public void add(double[] data) {
		this.data.add(data);
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
