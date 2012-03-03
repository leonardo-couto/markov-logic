package stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * Uses Pearson Chi-Squared test only when conditions are met
 * Otherwise applies Fisher Exact test.
 */
public class DefaultTest<T> implements IndependenceTest<T> {

	public final double alpha;
	private final FisherExact fe = new FisherExact(1000);
	private final Distribution<T> distribution;

	public DefaultTest(double alpha, Distribution<T> distribution) {
		this.alpha = alpha;
		this.distribution = distribution;
	}
	
	/*
	 * Gets the next <code>n</code> elements from iterator 
	 * <code>iterator</code> and puts into the <code>holder</code> List.
	 * If the Iterator has less than n elements, return false. Otherwise true.
	 */
	private static <U> boolean getNextNElements(List<U> holder, 
			Iterator<? extends U> iterator, int n) {
		for (int i = 0; i < n; i++) {
			if (iterator.hasNext()) {
				holder.add(iterator.next());
			} else {
				return false;
			}
		}
		return true;
	}

	private double getPvalue(T x, T y, List<T> z) {
		// initialize the marginal data
		List<T> nodes = new ArrayList<T>(z.size()+2);
		nodes.addAll(z);
		nodes.add(x);
		nodes.add(y);
		int cells = 1 << nodes.size();

		int increment = 100*cells;
		int sampledElements = increment;

		Iterator<boolean[]> dataIterator = this.distribution.getDataIterator(x, y, z);
		if (dataIterator == null) {
			return 0.99; // not connected, independent
		}
		List<boolean[]> data = new ArrayList<boolean[]>(sampledElements+1);
		if (!dataIterator.hasNext()) {
			// If X and Y share no variables, return a low pValue, meaning
			// they are probably dependent.
			return 0.01;
		} else {
			boolean[] d = dataIterator.next();
			data.add(d);
		}
		
		getNextNElements(data, dataIterator, sampledElements);

		int[] nbins = new int[nodes.size()];
		for (int i = 0; i < nodes.size(); i++) {
			nbins[i] = 2;
		}
		int nMatrices = cells / 4;

		MultiDimensionalHistogram histogram = new MultiDimensionalHistogram(nodes.size());
		histogram.addAll(convert(data));
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
			histogram.addAll(convert(data));
			data.clear();
		}
	}

	@Override
	public double pvalue(T x, T y) {
		return this.getPvalue(x, y, Collections.<T>emptyList());
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
	
	
	private List<List<T>> getPermutations(List<T> variables) {
		List<List<T>> permutations = new ArrayList<List<T>>();
		for (T var : variables) {
			permutations.add(Collections.singletonList(var));
		}
		List<T> TList = new ArrayList<T>(3);
		TList.add(null);
		TList.add(null);
		for (int i = 0; i < variables.size(); i++) {
			TList.set(0, variables.get(i));
			for (int j = i+1; j < variables.size(); j++) {
				TList.set(1, variables.get(j));
				permutations.add(new ArrayList<T>(TList));				
			}
		}
		TList.add(null);
		for (int i = 0; i < variables.size(); i++) {
			TList.set(0, variables.get(i));
			for (int j = i+1; j < variables.size(); j++) {
				TList.set(1, variables.get(j));
				for (int k = j+1; k < variables.size(); k++) {
					TList.set(2, variables.get(k));
					permutations.add(new ArrayList<T>(TList));
				}
			}
		}
		return permutations;
	}


	@Override
	public boolean test(T x, T y,	Set<T> z) {
		ArrayList<T> zList = new ArrayList<T>(z);
		for (List<T> p : this.getPermutations(zList)) {
			if (Double.compare(this.getPvalue(x, y, p), this.alpha) > 0) {
				return true;
			}
		}
		boolean independent = Double.compare(this.getPvalue(x, y, new ArrayList<T>(z)), this.alpha) > 0;
		return independent;
	}
	
	@Override
	public boolean test(double pvalue) {
		return Double.compare(pvalue, this.alpha) > 0;
	}
	
	private static List<double[]> convert(List<boolean[]> original) {
		List<double[]> list = new ArrayList<double[]>(original.size());
		for (boolean[] values : original) {
			double[] converted = new double[values.length];
			int i = 0;
			for (boolean value : values) {
				converted[i++] = value ? 1.0 : 0.0;
			}
			list.add(converted);
		}
		
		return list;
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


}