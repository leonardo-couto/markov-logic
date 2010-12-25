package stat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import structureLearner.TNodes;
import util.Util;


/**
 * Uses Pearson Chi-Squared test only when conditions are met
 * Otherwise applies Fisher Exact test.
 */
public class DefaultTest<RV extends RandomVariable<RV>> implements IndependenceTest<RV> {

	private final Map<RV, RandomVariableData> marginalData;
	public static int maxPartitions = 10;
	public final double alpha;
	private final FisherExact fe = new FisherExact(100);
	private final TNodes<RV> tNodes;

	public DefaultTest(double alpha, TNodes<RV> tNodes) {
		this.alpha = alpha;
		this.marginalData = new HashMap<RV, RandomVariableData>();
		this.tNodes = tNodes;
	}

	/*
	 * Gets the next <code>n</code> elements from iterator 
	 * <code>iterator</code> and puts into the <code>holder</code> List.
	 * If the Iterator has less than n elements, return false. Otherwise true.
	 */
	private static <T> boolean getNextNElements(List<T> holder, 
			Iterator<? extends T> iterator, int n) {
		for (int i = 0; i < n; i++) {
			if (iterator.hasNext()) {
				holder.add(iterator.next());
			} else {
				return false;
			}
		}
		return true;
	}

	private RandomVariableData initMarginalData(RV x) {
		RandomVariableData data = new RandomVariableData(x);
		this.marginalData.put(x, data);
		return data;
	}

	private static double min(double[] values) {
		double min = Double.MAX_VALUE;
		for(double value : values) {
			if(value < min)
				min = value;
		}
		return min;
	}

	private double getPvalue(RV x, RV y, List<RV> z) {
		// initialize the marginal data
		List<RV> nodes = new ArrayList<RV>(z.size()+2);
		nodes.addAll(z);
		nodes.add(x);
		nodes.add(y);
		int cells = 1;
		for (RV r : nodes) {
			if (!this.marginalData.containsKey(r)) { this.initMarginalData(r); }
			cells = cells * this.marginalData.get(r).maxBins;
		}

		int increment = 100*cells;
		int sampledElements = 3*increment;

		Iterator<double[]> dataIterator = this.tNodes.getDataIterator(x, y, z);
		List<double[]> data = new ArrayList<double[]>(sampledElements);
		getNextNElements(data, dataIterator, sampledElements);

		if (data.isEmpty()) {
			// If X and Y share no variables, return a high pvalue, meaning
			// they are probably independent.
			return 0.99;
		}

		int[] nbins = new int[nodes.size()];
		for (int i = 0; i < nodes.size(); i++) {
			nbins[i] = this.marginalData.get(nodes.get(i)).maxBins;
		}
		int[] originalNbins = Arrays.copyOf(nbins, nbins.length);

		//int xyMatrices = cells / (nbins[nbins.length-2] + nbins[nbins.length-1]);

		MultiDimensionalHistogram histogram = new MultiDimensionalHistogram(nodes.size());
		SequentialConvergenceTester tester = new ShortMemoryConvergenceTester(.95, .01, 10);
		histogram.addAll(data);
		data.clear();

		while (!tester.hasConverged()) {

			boolean moreThan2Bins = false;
			for (int nbin : nbins) {
				if (nbin > 2) {
					moreThan2Bins = true;
					break;
				}
			}

			// The idea is to have the approximate cell counts, based on each variable's 
			// marginal data frequency, to determine the number of bins for each variable 
			// according to the number of sample points taken.
			// The heuristic approach is: if any cell has a count smaller then 10
			// and any variable has more than 2 bins, reduce the number of bins of some 
			// variable. We need only the cell with smaller proportion to that.
			if (moreThan2Bins) {
				double min = 1.0;
				for (int i = 0; i < nodes.size(); i++) {
					min = min * min(this.marginalData.get(nodes.get(i)).getProportion(nbins[i]));
				}

				if (Double.compare(10.0, min*sampledElements) < 0) {
					this.reduce(nbins, nodes);
					break;
				}

			}


			int[][][] matrices = to2dMatrices(histogram.getHistogram(nbins), nodes.size());
			double[] pvalues = new double[matrices.length];
			{
				int i = 0;
				for (int[][] matrix : matrices) {
					ContingencyTable ct = new ContingencyTable(matrix);
					if (ct.fisher() && ct.getSum() < 100.0) {
						pvalues[i] = fe.getTwoTailedP(matrix[0][0], matrix[0][1], matrix[1][0], matrix[1][1]);
					} else {
						if (ct.pearson()) {
							PearsonChiSquare pearson = new PearsonChiSquare(ct);
							pvalues[i] = pearson.pvalue();
						} else {
							// TODO: APLICAR CORRECAO DE YATES!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
							// problemas: com muitas dimensoes podem ter zeros, ver o que fazer
							//pvalues[i] = 0.993; // por enquanto devolve um alto p-value, mas esta errado!
							ct.applyYates();
							PearsonChiSquare pearson = new PearsonChiSquare(ct);
							pvalues[i] = pearson.pvalue();
						}
					}
					i++;
				}
			}

			// TODO: SE A ARRAY FOR MUITO GRANDE, PEGAR A MEDIA DOS 10% MENORES!!
			double pvalue = min(pvalues);
			System.out.println("pvalue: " + pvalue);
			tester.increment(pvalue);
			if (!getNextNElements(data, dataIterator, increment)) {
				if (data.isEmpty()) {
					System.out.println("Pegou todos os elementos");
					return pvalue;
				}
			}
			sampledElements = sampledElements + increment;
			histogram.addAll(data);
			data.clear();
			nbins = Arrays.copyOf(originalNbins, originalNbins.length);

		}
		System.out.println("CONVERGIU");

		return tester.mean();
	}

	@Override
	public double pvalue(RV x, RV y) {
		return this.getPvalue(x, y, Collections.<RV>emptyList());
	}


	/**
	 * Transform a multidimensional matrix in a series of two dimensional matrices.
	 * @param matrix
	 * @param dimensions
	 * @return
	 */
	private static int[][][] to2dMatrices(Object[] matrix, int dimensions) {
		int d = 1;
		Object[] o =  matrix;
		for (int i = 0; i < dimensions -2; i++) {
			d = d * o.length;
			o = (Object[]) o[0];
		}
		int[][][] out = new int[d][][];
		for (int i = 0; i < d; i++) {
			o = matrix;
			int p = i;
			for (int j = 0; j < dimensions -2; j++) {
				int q = p % o.length;
				p = (int) Math.floor(p / o.length);
				o = (Object[]) o[q];
			}
			// "o" points to Object[int[]]
			int[][] m = new int[o.length][];
			for (int j = 0; j < o.length; j++) {
				m[j] = (int[]) o[j];
			}
			out[i] = m;
		}
		return out;
	}	


	@Override
	public boolean test(RV x, RV y,	Set<RV> z) {
		boolean independent = Double.compare(this.getPvalue(x, y, new ArrayList<RV>(z)), this.alpha) > 0;
		return independent;
	}

	/*
	 * Find and reduces the number of bins for the variable
	 * that has more than two bins and the lowest marginal 
	 * data geometric mean
	 */
	private void reduce(int[] nbins, List<RV> nodes) {
		// find the bin which has the lowest marginal data geometric mean
		int index = -1;
		double min = Double.MAX_VALUE;
		for (int i = 0; i < nbins.length; i++) {
			if (nbins[i] > 2) {
				double gmean = this.marginalData.get(nodes.get(i)).getGeometricMean(nbins[i]);
				if (Double.compare(gmean, min) < 0) {
					min = gmean;
					index = i;
				}
			}
		}

		// reduce the chosen bin
		if (index > -1) {
			nbins[index] = (int) Math.ceil(nbins[index]/2.0);
		}
	}


	public static void main(String[] args) { // TODO: REMOVER!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		//		int dimension = 3;
		//		Random r = new Random();
		//		List<double[]> data = new ArrayList<double[]>();
		//		for (int i = 0; i < 100; i++) {
		//			double[] d = new double[dimension];
		//			//for (int j = 0; j < d.length-1; j++) {
		//			//	d[j] = r.nextDouble();
		//			//}
		//			d[0] = r.nextDouble();
		//			d[1] = 0; //r.nextDouble();
		//			d[2] = r.nextDouble();
		//			data.add(d);
		//		}
		//		double[] min = new double[dimension];
		//		double[] max = new double[dimension];
		//		int[] nbins = new int[dimension];
		//		for (int i = 0; i < dimension; i++) {
		//			min[i] = 0;
		//			max[i] = 1;
		//			nbins[i] = 3;
		//		}
		//		Histogram hist = new Histogram(dimension, min, max, data);
		//		int[][][] out = to2dMatrices(hist.getHistogram(nbins), dimension);
		//		System.out.println(Arrays.deepToString(out));

	}

	private class RandomVariableData {

		public final double[] data;
		public final Histogram histogram;
		private final Map<Integer, int[]> binsHistogram;
		private final Map<Integer, Double> binsGeometricMean;
		private final Map<Integer, double[]> binsProportion;
		public final int maxBins;

		/**
		 * Gets the marginal data and generate a histogram
		 * with intervals such that all cells have counts
		 * higher than ten, or it has only two intervals.
		 * @param x the RandomVariable to get the marginal data. 
		 */
		public RandomVariableData(RV var) {
			this.data = var.getData();
			this.histogram = new Histogram();
			this.histogram.addAll(this.data);
			this.binsHistogram = new HashMap<Integer, int[]>();
			this.binsGeometricMean = new HashMap<Integer, Double>();
			this.binsProportion = new HashMap<Integer, double[]>();


			boolean stop = false;
			int n = DefaultTest.maxPartitions;
			int[] pdata = null;
			while (!stop && (n >= 2)) {
				pdata = this.histogram.getHistogram(n);
				stop = true;
				for (int count : pdata) {
					if (count < 10) {
						// Each cell has at least 10 counts.
						stop = false;
						break;
					}
				}
				n = (int) Math.ceil(n/2.0);
			}
			this.maxBins = pdata.length;
			this.binsHistogram.put(this.maxBins, pdata);
			this.init(pdata);
		}

		private void init(int bins) {
			if (this.binsHistogram.get(bins) == null) {
				int[] pdata = this.histogram.getHistogram(bins);
				this.binsHistogram.put(bins, pdata);
				this.init(pdata);
			}
		}

		private void init(int[] pdata) {
			double[] proportion = new double[pdata.length];
			int sum = 0;
			for (int e : pdata) {
				sum = sum + e;
			}
			for (int i = 0; i < pdata.length; i++) {
				proportion[i] = ((double) pdata[i])/sum;
			}
			this.binsProportion.put(pdata.length, proportion);
			this.binsGeometricMean.put(pdata.length, Util.geometricMean(pdata));
		}		

		@SuppressWarnings("unused")
		public int[] getHistogram(int n) {
			if (this.binsHistogram.containsKey(n)) {
				return this.binsHistogram.get(n);
			}
			if (n > maxBins) {
				throw new IllegalArgumentException("");
			}
			this.init(n);
			return this.binsHistogram.get(n);
		}

		public double getGeometricMean(int n) {
			if (this.binsGeometricMean.containsKey(n)) {
				return this.binsGeometricMean.get(n);
			}
			if (n > maxBins) {
				throw new IllegalArgumentException("");
			}
			this.init(n);
			return this.binsGeometricMean.get(n);
		}

		public double[] getProportion(int n) {
			if (this.binsProportion.containsKey(n)) {
				return this.binsProportion.get(n);
			}
			if (n > maxBins) {
				throw new IllegalArgumentException("");
			}
			this.init(n);
			return this.binsProportion.get(n);
		}


	}


}