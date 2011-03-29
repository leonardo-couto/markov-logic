package stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.MyException;
import util.Util;


/**
 * Uses Pearson Chi-Squared test only when conditions are met
 * Otherwise applies Fisher Exact test.
 */
public class DefaultTest<RV extends RandomVariable<RV>> implements IndependenceTest<RV> {

	private final Map<RV, RandomVariableData> marginalData;
	private static int maxPartitions = 10;
	public final double alpha;
	private final FisherExact fe = new FisherExact(1000);
	private final Distribution<RV> distribution;

	public DefaultTest(double alpha, Set<RV> domain) {
		this.alpha = alpha;
		this.marginalData = new HashMap<RV, RandomVariableData>();
		try {
			this.distribution = domain.isEmpty() ? null : 
				domain.iterator().next().getDistributionClass().newInstance();
		} catch (Exception e) {
			throw new MyException("RandomVariable Distribution class do " + 
					"not support empty constructor.", e);
		}
		this.distribution.addAll(domain);
	}
	
	public boolean addRandomVariable(RV r) {
		return this.distribution.add(r);
	}
	
	public boolean removeRandomVariable(RV r) {
		return this.distribution.remove(r);		
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
		int sampledElements = increment;

		Iterator<double[]> dataIterator = this.distribution.getDataIterator(x, y, z);
		if (dataIterator == null) {
			return 0.99; // not connected, independent
		}
		List<double[]> data = new ArrayList<double[]>(sampledElements+1);
		if (!dataIterator.hasNext()) {
			// If X and Y share no variables, return a low pvalue, meaning
			// they are probably dependent.
			return 0.01;
		} else {
			double[] d = dataIterator.next();
			data.add(d);
			boolean removed = false;
			for (int i = z.size(); i > 0; i--) {
				if (d[i-1] == -1.0) {
					removed = true;
					nodes.remove(i-1);
				}
			}
			if (removed) {
				data.clear();
				dataIterator = this.distribution.getDataIterator(x, y, nodes.subList(0, nodes.size() -2));
			}
		}
		
		getNextNElements(data, dataIterator, sampledElements);

		int[] nbins = new int[nodes.size()];
		for (int i = 0; i < nodes.size(); i++) {
			nbins[i] = this.marginalData.get(nodes.get(i)).maxBins;
		}
		int nMatrices = cells / (nbins[nbins.length-1] * nbins[nbins.length-2]);

		MultiDimensionalHistogram histogram = new MultiDimensionalHistogram(nodes.size());
		histogram.addAll(data);
		data.clear();
		
		boolean[] stopped = new boolean[nMatrices];
		double pvalue = 1.0;

		while (true) {

			int[][][] matrices = to2dMatrices(histogram.getHistogram(nbins), nodes.size());

			for (int i = 0; i < matrices.length; i++) { 
				if (!stopped[i]) {
					int[][] matrix = matrices[i]; 
					ContingencyTable ct = new ContingencyTable(matrix);
					if (ct.pearson()) {
						stopped[i] = true;
						PearsonChiSquare pearson = new PearsonChiSquare(ct);
						pvalue = Math.min(pearson.pvalue(), pvalue);
						if (Double.compare(pvalue, 0.01) < 0) {
							return pvalue;
						}
					} else if (ct.fisher() && ct.getSum() > 40 && ct.getSum() < 1000) {
						boolean fisher = true;
						for (double row : ct.getRowSum()) {
							if (row < 10) {
								fisher = false;
							}
						}
						for (double col : ct.getColSum()) {
							if (col < 10) {
								fisher = false;
							}
						}
						if (fisher) {
						  stopped[i] = true;
						  pvalue = Math.min(fe.getTwoTailedP(matrix[0][0], matrix[0][1], matrix[1][0], matrix[1][1]), pvalue);
						}
					}
				}
			}

			boolean stop = true;
			for (boolean b : stopped) {
				stop = stop && b;
			}
			if (stop || sampledElements > 10000 * cells) {
				return pvalue;
			}

			if (!getNextNElements(data, dataIterator, increment)) {
				if (data.isEmpty()) {
					return pvalue;
				}
			}
			sampledElements = sampledElements + increment;
			histogram.addAll(data);
			data.clear();
		}
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
	
	
	private List<List<RV>> getPermutations(List<RV> variables) {
		List<List<RV>> permutations = new ArrayList<List<RV>>();
		for (RV var : variables) {
			permutations.add(Collections.singletonList(var));
		}
		List<RV> rvList = new ArrayList<RV>(3);
		rvList.add(null);
		rvList.add(null);
		for (int i = 0; i < variables.size(); i++) {
			rvList.set(0, variables.get(i));
			for (int j = i+1; j < variables.size(); j++) {
				rvList.set(1, variables.get(j));
				permutations.add(new ArrayList<RV>(rvList));				
			}
		}
		rvList.add(null);
		for (int i = 0; i < variables.size(); i++) {
			rvList.set(0, variables.get(i));
			for (int j = i+1; j < variables.size(); j++) {
				rvList.set(1, variables.get(j));
				for (int k = j+1; k < variables.size(); k++) {
					rvList.set(2, variables.get(k));
					permutations.add(new ArrayList<RV>(rvList));
				}
			}
		}
		return permutations;
	}


	@Override
	public boolean test(RV x, RV y,	Set<RV> z) {
		ArrayList<RV> zList = new ArrayList<RV>(z);
		for (List<RV> p : this.getPermutations(zList)) {
			if (Double.compare(this.getPvalue(x, y, p), this.alpha) > 0) {
				return true;
			}
		}
		boolean independent = Double.compare(this.getPvalue(x, y, new ArrayList<RV>(z)), this.alpha) > 0;
		return independent;
	}
	
	@Override
	public boolean test(double pvalue) {
		return Double.compare(pvalue, this.alpha) > 0;
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

		public final List<Double> data;
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
			
			this.data = new ArrayList<Double>();
			for (Iterator<Double> it = distribution.getDataIterator(var); it.hasNext(); this.data.add(it.next()));
			
			this.histogram = new Histogram();
			this.histogram.addAll(this.data);
			this.binsHistogram = new HashMap<Integer, int[]>();
			this.binsGeometricMean = new HashMap<Integer, Double>();
			this.binsProportion = new HashMap<Integer, double[]>();
			
			int total = this.data.size();
			boolean stop = false;
			int n = DefaultTest.maxPartitions;
			int[] pdata = null;
			while (!stop && (n >= 2)) {
				pdata = this.histogram.getHistogram(n);
				stop = true;
				for (int count : pdata) {
					if ((count < 10) || Double.compare((count/total), 0.01) < 0) {
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

	}


}