package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.apache.commons.math.stat.descriptive.AbstractStorelessUnivariateStatistic;
import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.Variance;

import stat.MultiVarRandomIterator;

public class Sampler<T> implements Iterable<List<T>> {
	// TODO: talvez dividir em duas classes, one dimensional sampler
	//       e multidimensional sampler
	
	
	
	// s = somatorio de f(x, y, ...). (valor desconhecido, a ser estimado)
	// Y = estimativa de s com base nas amostras coletadas
	// epsilon = precisao de Y
	// p = grau de confianca da estimativa
	// n = numero total de elementos (populacao)
	// u = media estimada
	//
	// Queremos:
	//
	// P(|Y-s| <= epsilon*n*u) = p
	
	private final List<List<T>> domains;
	private final boolean shuffle;
	private final boolean noReplacement;
	private final int n;
	private static final int SHUFFLE_LIMIT = 50; //50;
	private static final int NO_REPLACEMENT_LIMIT = 500; //500;
	
	
	private final AbstractStorelessUnivariateStatistic mean = new Mean();
	private final AbstractStorelessUnivariateStatistic variance = new Variance();	
	//private final double precision;
	//private final double confidenceLevel;
//	private double Z; // Area under Normal Curve for given confidence level.

	public Sampler(List<? extends Collection<T>> domains) {
		
		boolean shuffle = true;
		boolean noReplacement = true;
		final List<List<T>> objDomains = new ArrayList<List<T>>(domains.size());
		int n = 1;
		for (int i=0; i < domains.size(); i++ ) {
			objDomains.add(new ArrayList<T>(domains.get(i)));

			// test for population size
			if (noReplacement && n < NO_REPLACEMENT_LIMIT) {
				n = n * domains.get(i).size();
				if (shuffle && n > SHUFFLE_LIMIT) {
					shuffle = false;
				}
				if (n > NO_REPLACEMENT_LIMIT) {
					noReplacement = false;
				}
			}
		}
		
		this.shuffle = shuffle;
		this.noReplacement = noReplacement;
		this.n = n;
		this.domains = objDomains;
		
		
//		this.precision = precision;
//		this.confidenceLevel = confidenceLevel;
//		this.Z = Sampler.normalSDInverseCumulative(confidenceLevel);
	}

	/**
	 * This method returns the critical point x, for the normal standard 
	 * distribution, such that P(-x &lt X &lt; x) = <code>p</code>.
	 * <p>
	 * Returns <code>Double.POSITIVE_INFINITY</code> for p=1.</p>
	 *
	 * @param p the desired probability
	 * @return x, such that P(-x &lt X &lt; x) = <code>p</code>
	 * @throws IllegalArgumentException if <code>p</code> is not a valid
	 *         probability, or if the inverse cumulative probability can not
	 *         be computed due to convergence or other numerical errors.
	 */
	private static double normalSDInverseCumulative(double p) {
		try {
			return new NormalDistributionImpl().inverseCumulativeProbability((p+1.0)/2.0);
		} catch (MathException e) {
			throw new IllegalArgumentException("Error calculating the inverse " +
					"Cumulative probability for p = " + p, e);
		}
	}




	// criar e manter uma arvore de escolhas jah feitas, ex:
	// a {a0, a1}, b {b0, b1}, c {c0, c1}
	//      b0 - c0
	//    /
	// a0 - b1 - c0
	//
	// escolher randomicamente cada ramo da arvore, se já estiver lotado, escolher outro ramo
	// backtraking, manter uma flag se jah esta cheio.

	public Iterator<List<T>> iterator() {

		if (shuffle) {
			return crossJoin(this.domains, this.n).iterator();
		}

		if (noReplacement) {
			return new MultiVarRandomIterator<T>(this.domains);
		}

		return new Iterator<List<T>>() {

			private final Random r = new Random();

			@Override
			public boolean hasNext() {
				return true;
			}

			@Override
			public List<T> next() {
				List<T> out = new ArrayList<T>(domains.size());
				for(List<T> domain : domains) {
					out.add(domain.get(r.nextInt(domain.size())));
				}
				return out;
			}

			@Override
			public void remove() {
				// do nothing
			}
		};
	}

	/**
	 * Given k List<T> <code>domains</code>, each with m_k elements 
	 * generates <code>n</code> k-tuples of elements (x1_d1, x2_d2, ... , xk_dk).
	 * If <code>n</code> is the number of possible permutations between the 
	 * arrays of elements in <code>domains</code>, 
	 * returns all possible permutations.
	 * 
	 * <code>n</code> has to be less or equal m_1 * m_2 * ... * m_k.
	 * 
	 * @param <T>
	 * @param domains 
	 * @param n
	 * @return List<List<T>> representing n permutations.
	 */
	private static <T> List<List<T>> crossJoin(List<List<T>> domains, int n) {
		int[] counter = new int[domains.size()+1];
		Arrays.fill(counter, 0);
		List<List<T>> out = new ArrayList<List<T>>(n);
		for (int i = 0; i < n; i++) {
			List<T> el = new ArrayList<T>(domains.size());
			for (int j = 0; j < domains.size(); j++) {
				if (counter[j] == domains.get(j).size()) {
					counter[j] = 0;
					counter[j+1]++;
				}
				el.add((T) domains.get(j).get(counter[j]));
			}
			counter[0]++;
			out.add(el);
		}
		Collections.shuffle(out);
		return out;
	}

	public static void main(String[] args) {
		String[] a = {"a0", "a1", "a2", "a3", "a4", "a5"};
		String[] b = {"b0", "b1"};
		String[] c = {"c0", "c1"};
		List<List<String>> domains = new ArrayList<List<String>>();
		domains.add(Arrays.asList(a));
		domains.add(Arrays.asList(b));
		domains.add(Arrays.asList(c));
		Sampler<String> sampler = new Sampler<String>(domains);
		//Iterator<List<String>> iterator = new MultiVarRandomIterator<String>(domains);
		int i = 0;
		for (List<String> parameters : sampler) {
			i++;
			System.out.println(Arrays.toString(parameters.toArray()));  
			if (i>15) {
				return;
			}
		}
	}
	
	public static void mainA(String[] args) {
		//		for(int n = 1000; n < 100000000; n = n*10) {
		//			int k = n/100;
		//			double kd = (double) k;
		//			double summation = 0;
		//			for (int i = n-k+1; i <= n; i++) {
		//				summation = summation + Math.log(i);
		//			}
		//			double logResult = summation - kd*Math.log(n);
		//			System.out.println(n + ": " + Math.exp(logResult));
		//		}
		//		try { 
		//			NormalDistribution normal = new NormalDistributionImpl();
		//			System.out.println(normal.inverseCumulativeProbability(0.995));
		//			//System.out.println(normal.inverseCumulativeProbability(0.95));
		//			//System.out.println(normal.cumulativeProbability(-1.96, 1.96));
		//			//System.out.println(normal.cumulativeProbability(-1.6448536283610324, 1.6448536283610324));
		//
		//			for(int i = 1; i <= 100; i++) {
		//				TDistribution tStudent = new TDistributionImpl(i);
		//				System.out.println("*" + tStudent.inverseCumulativeProbability(0.995));
		//			}
		//
		//
		//		} catch (Exception e) {
		//			// TODO: handle exception
		//		}

		String s0 = "d0_a";
		String s1 = "d0_b";
		String s2 = "d0_c";
		String s3 = "d0_d";
		String s4 = "d0_e";
		String r0 = "d1_a";
		String r1 = "d1_b";
		String t0 = "d2_a";
		String t1 = "d2_b";
		String t2 = "d2_c";
		Set<String> d0 = new HashSet<String>();
		Set<String> d1 = new HashSet<String>();
		Set<String> d2 = new HashSet<String>();
		d0.add(s0);
		d0.add(s1);
		d0.add(s2);
		d0.add(s3);
		d0.add(s4);
		d1.add(r0);
		d1.add(r1);
		d2.add(t0);
		d2.add(t1);
		d2.add(t2);
		List<Set<String>> doms = new ArrayList<Set<String>>();
		doms.add(d0);
		doms.add(d1);
		doms.add(d2);
		//getRandomParameters(new String(), doms);


	}

	
}