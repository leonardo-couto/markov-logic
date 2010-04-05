package stat;

import java.util.Arrays;

public class Histogram {
	
	public final int dimensions;
	public final double[] nmin;
	public final double[] nmax;
	private double[][] data;
	
	public Histogram(int dimensions, double[] nmin, double[] nmax, double[][] data) {
		this.dimensions = dimensions;
		this.nmax = nmax;
		this.nmin = nmin;
		this.data = data;
		checkDataBounds();
	}
	
	
	/**
	 * One dimensional histogram
	 * @param nmax
	 * @param nmin
	 * @param data
	 */
	public Histogram(double nmin, double nmax, double[] data) {
		this.dimensions = 1;
		this.nmax = new double[1];
		this.nmin = new double[1];
		this.data = new double[data.length][1];
		this.nmax[0] = nmax;
		this.nmin[0] = nmin;
		for (int i = 0; i < data.length; i++) {
			this.data[i][0] = data[i];
		}
		checkDataBounds();
	}
	
	private void checkDataBounds() throws IllegalArgumentException {
		for (double[] point : data) {
			for (int i = 0; i < point.length; i++) {
				if ((Double.compare(point[i], nmin[i]) < 0) || (Double.compare(point[i], nmax[i]) > 0)) {
					throw new IllegalArgumentException("Data outside specified range [" + nmin[i] + 
							", " + nmax[i] + "]. d = " + point[i]);
				}
			}
		}
	}

	/**
	 * For one dimensional histogram
	 * @param nbins
	 */
	public int[] getHistogram(int nbins) {
		if (dimensions != 1) {
			throw new IllegalArgumentException("Data has more than one dimension.");
		}
		int[] out = new int[nbins];
		Arrays.fill(out, 0);
		double dbin = (nmax[0] - nmin[0])/((double) nbins);
		
		for (double[] point : data) {
			int idx = Math.min((int) Math.floor(((point[0] - nmin[0])/dbin)), nbins-1);
			out[idx]++;
		}
		
		return out;
	}
	
	/**
	 * @param nbins
	 */
	public Object[] getHistogram(int[] nbins) {
		if (nbins.length != dimensions) {
			throw new IllegalArgumentException("Dimensions do not match");
		}
		Object[] out;
		if (nbins.length > 1) {
			out = new Object[nbins[0]];
		} else {
			out = new Object[1];
		}
		Object[][] aux = new Object[1][];
		aux[0] = out;
		for (int i = 1; i < nbins.length -1; i++) {
			aux = deepInitialize(aux, nbins[i]);
		}
		deepInitializeInt(aux, nbins[nbins.length-1]);
		
		double[] dbin = new double[nbins.length];
		for (int i = 0; i < nbins.length; i++) {
			dbin[i] = (nmax[i] - nmin[i])/((double) nbins[i]);
		}
		
		for (double[] point : data) {
			Object[] counts = out;
			int[] data = null;
			for (int i = 0; i < point.length; i++) {
				int idx = Math.min((int) Math.floor(((point[i] - nmin[i])/dbin[i])), nbins[i]-1);
				if (i < point.length -2) {
					counts = (Object[]) counts[idx];
				} else if (i == point.length -2) {
					data = (int[]) counts[idx];
				} else if (i == point.length -1) {
					data[idx]++;
				}
			}
		}
		
		return out;
	}
	
	@SuppressWarnings("unused")
	private void recursiveDeepInitialize(Object[] oArray, int n) {
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
	
	private Object[][] deepInitialize(Object[][] oArray, int n) {
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
	
	@SuppressWarnings("unused")
	private void recursiveDeepInitializeInt(Object[] oArray, int n) {
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
	
	private void deepInitializeInt(Object[][] oArray, int n) {
		for (int i = 0; i < oArray.length; i++) {
			for (int j = 0; j < oArray[0].length; j++) {
				int[] iArray = new int[n];
				Arrays.fill(iArray, 0);
				oArray[i][j] = iArray;
			}
		}
	}
	
}
