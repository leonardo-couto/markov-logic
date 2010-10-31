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
	
	private void initMarginals(RV x) {
		double[] data = x.getData();
		this.marginalData.put(x, data);
		Histogram h = new Histogram(0.0, 1.0, data);
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
	}
	
	@Override
	public double pvalue(RV x, RV y) {
		if (!this.marginalData.containsKey(x)) {
			this.initMarginals(x);
		}
		if (!this.marginalData.containsKey(y)) {
			initMarginals(y);
		}
		Iterator<double[]> data = this.tNodes.getDataIterator(x, y, Collections.<RV>emptyList());
		ConvergenceTester tester = ConvergenceTester.lowPrecisionConvergence();
		
		
		double[][] data = x.getData(y, Collections.<RV>emptyList());
		if (data == null) {
			// If X and Y share no variables, return a high pvalue, meaning
			// they are probably independent.
			return 0.99;
		}
		double[] nmin = new double[2];
		double[] nmax = new double[2];
		Arrays.fill(nmin, 0.0);
		Arrays.fill(nmax, 1.0);
		Histogram histogram = new Histogram(2, nmin, nmax, data);
		int[] nbins = {this.marginalDataHist.get(x).length,
				       this.marginalDataHist.get(y).length};
		double[] gmean = {Util.geometricMean(this.marginalDataHist.get(x)), 
				          Util.geometricMean(this.marginalDataHist.get(x))}; 
		int[][] matrix = to2dMatrices(histogram.getHistogram(nbins), 2)[0]; // TODO: PAREI AQUI! pegar o histograma e fazer o teste!

		// Reduces the data count matrix size until satisfies Pearson conditions, 
		// or reach a 2x2 matrix, in this case applies Fisher Exact test.
		while (true) {
			ContingencyTable ct = new ContingencyTable(matrix);
			if (ct.pearson()) {
				return (new PearsonChiSquare2D(ct)).pvalue();
			}
			if (ct.fisher()) {
				return fe.getTwoTailedP(matrix[0][0], matrix[0][1], matrix[1][0], matrix[1][1]);
			}
			if (Double.compare(gmean[0], gmean[1]) < 0) {
				if (nbins[0] > 2) {
					nbins[0] = (int) Math.ceil(nbins[0]/2.0);
					gmean[0] = Util.geometricMean((new Histogram(0.0, 1.0, marginalData.get(x))).getHistogram(nbins[0]));
				} else {
					nbins[1] = (int) Math.ceil(nbins[1]/2.0);
					gmean[1] = Util.geometricMean((new Histogram(0.0, 1.0, marginalData.get(y))).getHistogram(nbins[1]));
				}
			} else {
				if (nbins[1] > 2) {
					nbins[1] = (int) Math.ceil(nbins[1]/2.0);
					gmean[1] = Util.geometricMean((new Histogram(0.0, 1.0, marginalData.get(y))).getHistogram(nbins[1]));
				} else {
					nbins[0] = (int) Math.ceil(nbins[0]/2.0);
					gmean[0] = Util.geometricMean((new Histogram(0.0, 1.0, marginalData.get(x))).getHistogram(nbins[0]));
				}
				matrix = to2dMatrices(histogram.getHistogram(nbins), 2)[0];
			}
			
		}
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
	public boolean test(RV x, RV y,	Set<RV> Z) {
		List<RV> nodes = new ArrayList<RV>(Z.size()+2);
		nodes.addAll(Z);
		nodes.add(x);
		nodes.add(y);
		double[][] data = x.getData(y, zList);
		if (data == null) {
			// Return false (X and Y dependent) to do not
			// break Strong Union axiom.
			return false;
		}
		
		zList.add(x);
		zList.add(y);
		for (RV rv : zList) {
			if (!marginalData.containsKey(rv)) {
				initMarginals(rv);
			}
		}

		int dimension = zList.size();
		double[] nmin = new double[dimension];
		double[] nmax = new double[dimension];
		Arrays.fill(nmin, 0.0);
		Arrays.fill(nmax, 1.0);
		Histogram h = new Histogram(dimension, nmin, nmax, data);
		int[] nbins = new int[dimension];
		Pair[] gmean = new Pair[dimension];
		for (int i = 0; i < nbins.length; i++) {
			nbins[i] = marginalDataHist.get(zList.get(i)).length;
			gmean[i] = new Pair(Util.geometricMean(marginalDataHist.get(zList.get(i))),i);
		}

		int[][][] matrices = to2dMatrices(h.getHistogram(nbins), dimension);
		
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
						Histogram h1 = new Histogram(0.0, 1.0, marginalData.get(zList.get(gmean[i].i)));
						gmean[i].d = Util.geometricMean(h1.getHistogram(nbins[gmean[i].i]));
						break;
					}
				}
			} else {
				break;
			}
			matrices = to2dMatrices(h.getHistogram(nbins), dimension);
		}
		
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
				double pvalue = (new PearsonChiSquare2D(ct)).pvalue();
				if (Double.compare(pvalue, alpha) < 1) {
					independent = false;
					break;
				}
			} else {
				double pvalue = fe.getTwoTailedP(matrix[0][0], matrix[0][1], matrix[1][0], matrix[1][1]);
				if (Double.compare(pvalue, alpha) < 1) {
					independent = false;
					break;
				}
			}
		}
		return independent;
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
		Histogram hist = new Histogram(dimension, min, max, data.toArray(new double[0][]));
		int[][][] out = to2dMatrices(hist.getHistogram(nbins), dimension);
		System.out.println(Arrays.deepToString(out));
		
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
