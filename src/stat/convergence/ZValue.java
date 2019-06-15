package stat.convergence;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.apache.commons.math.distribution.TDistribution;
import org.apache.commons.math.distribution.TDistributionImpl;

/**
 * <p>For a given confidence level, returns the Normal Distribution values
 * for large sample sizes and t-Student for small samples.</p>
 * 
 * <p>The transition between t-Student and Normal occurs when the difference between
 * both distributions is less than <code>EPSLON</code></p>
 * 
 * <p>Minimum precision for confidence level in this class is 10e-3. Minimum value
 * if 0.5 and maximum is 0.999</p>. 
 */
public class ZValue {
	
	private static final double EPSLON = 0.005;
	private static final int INV_PRECISION = 1000;
	private static final double PRECISION = 1e-3;
	private static final Map<Integer, ZValue> CACHE = new ConcurrentHashMap<Integer, ZValue>();
	
	private final double confidence;
	private final double normal;
	private final double[] tStudent;
	
	private ZValue(double confidence) {
		this.confidence = confidence;
		NormalDistribution normalDist = new NormalDistributionImpl();
		this.normal = inverseCumulative(normalDist,confidence);
		this.tStudent = this.tStudent();
	}
	
	public double valueOf(int samples) {
		return samples < this.tStudent.length ? this.tStudent[samples] : this.normal;
	}
	
	/**
	 * Calcula os primeiros elementos da distribuicao t de student
	 * para i=1 .. n. (i -> infinito tende a normal)
	 * onde n é tal que |tStudent[n] - normal| < epslon
	 */
	private double[] tStudent() {
		ArrayList<Double> tStudentList = new ArrayList<Double>();

		int counter = 0;
		boolean b = true;
		while (b) {
			TDistribution tStudentDist = new TDistributionImpl(counter+1);
			double tStudent = inverseCumulative(tStudentDist,this.confidence);
			tStudentList.add(Double.valueOf(tStudent));
			if (Double.compare(Math.abs(tStudent-this.normal), EPSLON) < 1) {
				b = false;
			}
			counter++;
		}
		
		double[] tStudent = new double[counter];
		for (int i = 0; i < counter; i++) tStudent[i] = tStudentList.get(i);
		return  tStudent;
	}
	
	
	public static ZValue getZValue(double confidence) {
		int value = (int) Math.round(confidence * INV_PRECISION);
		if (value > 999) value = 999;
		else if (value < 500) throw new IllegalArgumentException("Confidence level smaller than 50%");
		Integer key = Integer.valueOf(value);
		
		ZValue z = CACHE.get(key);
		if (z == null) {
			confidence = PRECISION * value;
			z = new ZValue(confidence);
			CACHE.put(key, z);
		}
		
		return z;		
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
	private static double inverseCumulative(ContinuousDistribution dist, double p) {
		try {
			return dist.inverseCumulativeProbability((p+1.0)/2.0);
		} catch (MathException e) {
			throw new IllegalArgumentException("Error calculating the inverse " +
					"Cumulative probability for p = " + p, e);
		}
	}
	
}
