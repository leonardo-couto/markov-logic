package stat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.apache.commons.math.distribution.TDistribution;
import org.apache.commons.math.distribution.TDistributionImpl;

/**
 * @author Leonardo Castilho Couto
 *
 */
public class ConvergenceTester {

	private final double precision;
	private final double confidenceLevel;
	private final double[] tStudent;
	private final double normal;
	private static final double epslon = 0.005;
	private static final Map<Double, Map<Double, ConvergenceTester>> staticTester = new HashMap<Double, Map<Double,ConvergenceTester>>();
	private static final Object lock = new Object();

	/*
	 * Instead of creating many instances of this class, 
	 * call the static method getTester instead.
	 */
	public ConvergenceTester(double confidenceLevel, double precision) {
		this.precision = precision;
		this.confidenceLevel = confidenceLevel;
		NormalDistribution normalDist = new NormalDistributionImpl();
		double normal = inverseCumulative(normalDist,confidenceLevel);
		ArrayList<Double> tStudentList = new ArrayList<Double>();

		/**
		 * Calcula os primeiros elementos da distribuicao t de student
		 * para i=1 .. n. (i -> infinito tende a normal)
		 * onde n é tal que |tStudent[n] - normal| < epslon
		 */
		int i = 0;
		boolean b = true;
		while (b) {
			TDistribution tStudentDist = new TDistributionImpl(i+1);
			double tStudent = inverseCumulative(tStudentDist,confidenceLevel);
			tStudentList.add(new Double(tStudent));
			if (Double.compare(Math.abs(tStudent-normal), epslon) < 1) {
				b = false;
			}
			i++;
		}
		this.normal = normal;
		this.tStudent = new double[tStudentList.size()];
		i = 0;
		for (Double ts : tStudentList) {
			this.tStudent[i] = ts.doubleValue();
			i++;
		}
	}

	// see Hass, Peter J. and Swami, Arun N. - Sequential Sampling
	// Procedures for Query Size Estimation
	// e*mean*n > z_{p,n}*(variance*n)^1/2
	//
	// s = somatorio de f(x, y, ...). (valor desconhecido, a ser estimado)
	// Y = estimativa de s com base nas amostras coletadas
	// epsilon = precisao de Y
	// p = grau de confianca da estimativa
	// n = numero de elementos amostrados (populacao)
	// u = media estimada
	//
	// Queremos:
	//
	// P(|Y-s| <= epsilon*n*u) = p
	public boolean hasConverged(double variance, double mean, int n) {
		if (Double.compare(variance,0) > 0) {
			double Z;
			if (n < this.tStudent.length) {
				Z = this.tStudent[n];
			} else {
				Z = this.normal;
			}
			double a = this.precision*mean*n;
			double b = Z*Math.sqrt(variance*n);
			return (Double.compare(a, b) > 0);
		} else {
			// TODO: Caso w = 0 varias vezes, calcular o numero de vezes para determinado
			// grau de confianca e erro. Assumir uma distribuicao discreta de 0 e 1.
			if (n >= 10) {
				return true;
			}
		}
		return false;
	}

	public double getPrecision() {
		return this.precision;
	}

	public double getConfidenceLevel() {
		return this.confidenceLevel;
	}	

	/**
	 * This method returns the critical point x, for the normal standard 
	 * and tStudent distribution, such that P(-x &lt X &lt; x) = <code>p</code>.
	 * <p>
	 * Returns <code>Double.POSITIVE_INFINITY</code> for p=1.</p>
	 *
	 * @param p the desired probability
	 * @return x, such that P(-x &lt X &lt; x) = <code>p</code>
	 * @throws IllegalArgumentException if <code>p</code> is not a valid
	 *         probability, or if the inverse cumulative probability can not
	 *         be computed due to convergence or other numerical errors.
	 */
	private static double inverseCumulative(ContinuousDistribution dist, 
			double p) {
		try {
			return dist.inverseCumulativeProbability((p+1.0)/2.0);
		} catch (MathException e) {
			throw new IllegalArgumentException("Error calculating the inverse " +
					"Cumulative probability for p = " + p, e);
		}
	}

	/*
	 * Store this class instances in a Map, and reuse then. 
	 * All data in this class are final, so there is no need to 
	 * replicate instances that has the same constructor arguments.
	 */
	public static ConvergenceTester getTester(double confidenceLevel, double precision) {
		Map<Double, ConvergenceTester> map;
		Double confidenceLevelD = new Double(confidenceLevel);
		Double precisionD = new Double(precision);
		synchronized (lock) {
			if (staticTester.containsKey(confidenceLevelD)) {
				map = staticTester.get(confidenceLevelD);
				if (map.containsKey(precisionD)) {
					return map.get(precisionD);
				} else { 
					ConvergenceTester ct = new ConvergenceTester(confidenceLevel, precision);
					map.put(precisionD, ct);
					return ct;
				}
			}
			map = new HashMap<Double, ConvergenceTester>();
			ConvergenceTester ct = new ConvergenceTester(confidenceLevel, precision);
			map.put(precisionD, ct);
			staticTester.put(confidenceLevelD, map);
			return ct;		
		}
	}

}
