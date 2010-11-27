package stat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import structureLearner.TNodes;
import util.Util;


/**
 * Uses Pearson Chi-Squared test only when conditions are met
 * Otherwise applies Fisher Exact test.
 */
public class DefaultTest<RV extends RandomVariable<RV>> implements IndependenceTest<RV> {

	private final Map<RV, double[]> marginalData;
	private final Map<RV, int[]> marginalDataHist;
	//private final Map<RV, double[]> maginalHistogramProportion; // TODO: TROCAR TUDO PARA UTILIZAR A CLASSE AUXILIAR!!!!!!!!!!!!!!!!!!!!!
	public static int maxPartitions = 10;
	public final double alpha;
	private final FisherExact fe = new FisherExact(100000);
	private final TNodes<RV> tNodes;

	public DefaultTest(double alpha, TNodes<RV> tNodes) {
		this.alpha = alpha;
		this.marginalData = new HashMap<RV, double[]>();
		this.marginalDataHist = new HashMap<RV, int[]>();
		this.tNodes = tNodes;
	}

	/**
	 * Gets the marginal data and generate a histogram
	 * with intervals such that all cells have counts
	 * higher than ten, or it has only two intervals.
	 * @param x the RandomVariable to get the marginal data. 
	 */
	private int[] initMarginalData(RV x) {
		double[] data = x.getData();
		this.marginalData.put(x, data);
		Histogram h = new Histogram();
		h.addAll(data);
		boolean stop = false;
		int n = maxPartitions;
		int[] pdata = null;
		while (!stop && (n >= 2)) {
			pdata = h.getHistogram(n);
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
		this.marginalDataHist.put(x, pdata);
		return pdata;
	}


	/*
	 * Return a matrix representing the outer join of two 
	 * vectors <code>a</code> and <code>b</code>
	 */
	private static double[][] outerJoin(double[] a, double[] b) {
		double[][] matrix = new double[a.length][b.length];
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[i].length; j++) {
				matrix[i][j] = a[i] * b[j];
			}
		}
		return matrix;
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

	@Override
	public double pvalue(RV x, RV y) {

		// gets the marginal data for x and y
		int[] marginalDataX = this.marginalData.containsKey(x) ? 
				this.marginalDataHist.get(x) : this.initMarginalData(x);
				int[] marginalDataY = this.marginalData.containsKey(y) ?
						this.marginalDataHist.get(y) : this.initMarginalData(y);


						double[] marginalProportionX = new double[marginalDataX.length];
						double[] marginalProportionY = new double[marginalDataY.length];
						int sumX = 0, sumY = 0;
						for (int value : marginalDataX) { sumX = sumX + value; };
						for (int value : marginalDataY) { sumY = sumY + value; };
						for (int i = 0; i < marginalDataX.length; i++) { 
							marginalProportionX[i] = ((double) marginalDataX[i])/sumX;
						};
						for (int i = 0; i < marginalDataY.length; i++) { 
							marginalProportionY[i] = ((double) marginalDataY[i])/sumY;
						};


						double[][] proportionMatrix = outerJoin(marginalProportionX, marginalProportionY);

						int cells = marginalDataX.length*marginalDataY.length;
						int increment = 10*cells;
						int sampledElements = 2*increment;

						Iterator<double[]> dataIterator = this.tNodes.getDataIterator(x, y, Collections.<RV>emptyList());
						List<double[]> data = new ArrayList<double[]>(2*increment);
						boolean stopped = !getNextNElements(data, dataIterator, 2*increment);

						if (data.isEmpty()) {
							// If X and Y share no variables, return a high pvalue, meaning
							// they are probably independent.
							return 0.99;
						}








						List<double[]> data = new ArrayList<double[]>();		
						while (dataIterator.hasNext()) {
							data.add(dataIterator.next());
						}

						if (data.isEmpty()) {
							// If X and Y share no variables, return a high pvalue, meaning
							// they are probably independent.
							return 0.99;
						}
						double[] nmin = new double[2];
						double[] nmax = new double[2];
						Arrays.fill(nmin, 0.0);
						Arrays.fill(nmax, 1.0);
						Histogram histogram = new Histogram(2, nmin, nmax, data);
						List<RV> nodes = new ArrayList<RV>(2);
						nodes.add(x);
						nodes.add(y);
						int[][] matrix = this.reduce(histogram, nodes)[0];

						ContingencyTable ct = new ContingencyTable(matrix);
						if (ct.pearson()) {
							return (new PearsonChiSquare(ct)).pvalue();
						}
						return fe.getTwoTailedP(matrix[0][0], matrix[0][1], matrix[1][0], matrix[1][1]);
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

		Iterator<double[]> dataIterator = this.tNodes.getDataIterator(x, y, new ArrayList<RV>(z));
		// TODO: testar a convergencia ao invez de pegar todos os dados? Fazer o teste rodar paralelamente?
		List<double[]> data = new ArrayList<double[]>();
		while (dataIterator.hasNext()) {
			data.add(dataIterator.next());
		}

		if (data.isEmpty()) {
			// Return false (X and Y dependent) to do not
			// break Strong Union axiom.
			return false;
		}

		List<RV> nodes = new ArrayList<RV>(z.size()+2);
		nodes.addAll(z);
		nodes.add(x);
		nodes.add(y);

		for (RV rv : nodes) {
			if (!marginalData.containsKey(rv)) {
				initMarginalData(rv);
			}
		}

		int dimension = nodes.size();
		double[] nmin = new double[dimension];
		double[] nmax = new double[dimension];
		Arrays.fill(nmin, 0.0);
		Arrays.fill(nmax, 1.0);
		Histogram histogram = new Histogram(dimension, nmin, nmax, data);
		int[][][] matrices = this.reduce(histogram, nodes);

		// All Matrices satisfies Pearson conditions, or are 2x2 matrices.
		// Run the tests
		boolean independent = true;
		// TODO: A probabilidade de se atingir um valor menor do que alpha para o pvalue
		// para muitas tentativas eh alta, tentar corrigir isso quando o numero de matrizes
		// eh grande. * pvalue -> eh a probabilidade de encontrar valores pelo menos tao
		// extremos quanto os encontrados, assumindo que X e Y sao independentes. Ou seja
		// mesmo se X e Y forem independentes, para alpha = 0.05, 5 por cento das vezes
		// o teste dira que eles sao dependentes.
		for (int[][] matrix : matrices) {

			ContingencyTable ct = new ContingencyTable(matrix);
			if (ct.pearson()) {
				double pvalue = (new PearsonChiSquare(ct)).pvalue();
				if (Double.compare(pvalue, this.alpha) < 1) {
					independent = false;
					break;
				}
			} else {
				double pvalue = fe.getTwoTailedP(matrix[0][0], matrix[0][1], matrix[1][0], matrix[1][1]);
				if (Double.compare(pvalue, this.alpha) < 1) {
					independent = false;
					break;
				}
			}
		}
		return independent;

	}

	/*
	 * Return a matrix set where each matrix satisfies the Pearson
	 * conditions, or all matrices are reduced to 2x2 matrices.
	 */
	private int[][][] reduce(Histogram histogram, List<RV> nodes) {

		int[] nbins = new int[histogram.dimension];
		Pair[] gmean = new Pair[histogram.dimension];
		for (int i = 0; i < nbins.length; i++) {
			nbins[i] = this.marginalDataHist.get(nodes.get(i)).length;
			gmean[i] = new Pair(Util.geometricMean(marginalDataHist.get(nodes.get(i))),i);
		}

		int[][][] matrices = to2dMatrices(histogram.getHistogram(nbins), histogram.dimension);

		// Check if all matrices satisfies Pearson conditions, or reduces matrix size
		// until reach 2x2 matrices.
		while (true) {
			boolean pearson = true;
			for (int[][] matrix : matrices) {
				ContingencyTable ct = new ContingencyTable(matrix);
				if (!ct.pearson()) {
					pearson = false;
					break;
				}
			}
			if (!pearson) {
				boolean fisher = true;
				for (int d : nbins) {
					if (d != 2) {
						fisher = false;
						break;
					}
				}
				if (fisher) {
					break;
				}
				Arrays.sort(gmean);
				for (int i = 0; i < gmean.length; i++) {
					if (nbins[gmean[i].i] > 2) {
						nbins[gmean[i].i] = (int) Math.ceil(nbins[gmean[i].i]/2.0);
						Histogram h1 = new Histogram(0.0, 1.0, this.marginalData.get(nodes.get(gmean[i].i)));
						gmean[i].d = Util.geometricMean(h1.getHistogram(nbins[gmean[i].i]));
						break;
					}
				}
			} else {
				break;
			}
			matrices = to2dMatrices(histogram.getHistogram(nbins), histogram.dimension);
		}

		return matrices;
	}

	public static void main(String[] args) {
		int dimension = 3;
		Random r = new Random();
		List<double[]> data = new ArrayList<double[]>();
		for (int i = 0; i < 100; i++) {
			double[] d = new double[dimension];
			//for (int j = 0; j < d.length-1; j++) {
			//	d[j] = r.nextDouble();
			//}
			d[0] = r.nextDouble();
			d[1] = 0; //r.nextDouble();
			d[2] = r.nextDouble();
			data.add(d);
		}
		double[] min = new double[dimension];
		double[] max = new double[dimension];
		int[] nbins = new int[dimension];
		for (int i = 0; i < dimension; i++) {
			min[i] = 0;
			max[i] = 1;
			nbins[i] = 3;
		}
		Histogram hist = new Histogram(dimension, min, max, data);
		int[][][] out = to2dMatrices(hist.getHistogram(nbins), dimension);
		System.out.println(Arrays.deepToString(out));

	}

	private class RandomVariableData {

		public final double[] data;
		public final Histogram histogram;
		private final Map<Integer, int[]> binsHistogram;
		private final Map<Integer, Double> binsGeometricMean;
		private final Map<Integer, double[]> binsProportion;
		public final int maxBins;

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
			int[] pdata = this.histogram.getHistogram(bins);
			this.binsHistogram.put(bins, pdata);
			this.init(pdata);
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

		public int[] getHistogram() {
			return this.getHistogram(this.maxBins);
		}

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

		public double getGeometricMean() {
			return this.getGeometricMean(this.maxBins);
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

		public double[] getProportion() {
			return this.getProportion(this.maxBins);
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

class Pair implements Comparable<Pair> {
	public double d;
	public final int i;

	public Pair(double d, int i) {
		this.d = d;
		this.i = i;
	}

	@Override
	public int compareTo(Pair o) {
		return Double.compare(this.d, o.d);
	}

}
