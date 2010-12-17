package stat;

import main.Settings;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.apache.commons.math.distribution.TDistribution;
import org.apache.commons.math.distribution.TDistributionImpl;
import org.apache.commons.math.stat.descriptive.AbstractStorelessUnivariateStatistic;
import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.Variance;

/**
 * @author Leonardo Castilho Couto
 *
 * @param <T>
 */
public class ConvergenceTester {
	
	private final double precision;
	private final AbstractStorelessUnivariateStatistic mean = new Mean();
	private final AbstractStorelessUnivariateStatistic variance = new Variance();
	private boolean converged = false;
	
  private static final int tLimit = 100;
	private final double[] tStudent;
	private final double normal;
	
	public ConvergenceTester(double confidenceLevel, double precision) {
		this.precision = precision;
		this.tStudent = new double[tLimit];
		
	  /**
	   * Calcula os tlimit primeiros elementos da distribuicao t de student
	   * para i=1 .. tlimit graus de liberdade. (i -> infinito tende a normal)
	   */
		for (int i = 0; i < tLimit; i++) {
		  TDistribution tStudentDist = new TDistributionImpl(i+1);
		  this.tStudent[i] = inverseCumulative(tStudentDist,confidenceLevel);
		}
		NormalDistribution normalDist = new NormalDistributionImpl();
		this.normal = inverseCumulative(normalDist,confidenceLevel);
	}
	
	private ConvergenceTester(double confidenceLevel, double precision, 
	                          double normal, double[] tStudent) {
    this.precision = precision;
    this.normal = normal;
    this.tStudent = tStudent;
	}
	
	public boolean hasConverged() {
		return this.converged;
	}

	public boolean increment(double next) {
    this.testConvergence(next);
    return this.converged;
	}

	private boolean convergedOnce = false;
	
	private void testConvergence(double nextValue) {
		this.mean.increment(nextValue);
		this.variance.increment(nextValue);
		
		// need to converge twice before setting this.converged = true;
		if (this.hasConverged(this.variance.getResult(), 
		                    this.mean.getResult(),
		                    (int) this.mean.getN())) {
      if(this.convergedOnce) {
        this.converged = true;
      } else {
        this.convergedOnce = true;
      }
		} else {
		  this.convergedOnce = false;
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
	// n = numero total de elementos (populacao)
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
	    if (n > 10) {
	      return true;
	    }
	  }
    return false;
	}
	
	public boolean evaluate(double[] values) {
	  this.mean.evaluate(values);
	  this.variance.evaluate(values);
    if (this.hasConverged(this.variance.getResult(), 
            this.mean.getResult(),
            values.length)) {
      this.convergedOnce = true;
      this.converged = true;
    }
    return this.converged;
	}
	
  /**
   * Returns the current value of the mean.
   * @return value of the mean, <code>Double.NaN</code> if it
   * has been just instantiated.
   */
	public double mean() {
	  return this.mean.getResult();
	}
	
  /**
   * Returns the current value of the variance.
   * @return value of the variance, <code>Double.NaN</code> if it
   * has been just instantiated.
   */
	public double variance() {
	  return this.variance.getResult();
	}
	
  public static ConvergenceTester lowPrecisionConvergence() {
    return new ConvergenceTester(Settings.confidenceLevel[0], Settings.precision[0],
            Settings.normal[0], Settings.tStudent[0]);
  }

  public static ConvergenceTester mediumPrecisionConvergence() {
    return new ConvergenceTester(Settings.confidenceLevel[1], Settings.precision[1],
            Settings.normal[1], Settings.tStudent[1]);
  }

  public static ConvergenceTester highPrecisionConvergence() {
    return new ConvergenceTester(Settings.confidenceLevel[2], Settings.precision[2], 
            Settings.normal[2], Settings.tStudent[2]);
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
  
  
}
