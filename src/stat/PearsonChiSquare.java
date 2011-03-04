package stat;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ChiSquaredDistribution;
import org.apache.commons.math.distribution.ChiSquaredDistributionImpl;
import org.apache.commons.math.stat.inference.ChiSquareTest;
import org.apache.commons.math.stat.inference.ChiSquareTestImpl;

/**
 * @author Leonardo Castilho Couto
 *
 */
public class PearsonChiSquare {
	
	private final ContingencyTable observed;
	private final ContingencyTable expected;
	private final ChiSquaredDistribution csd;
	
	public PearsonChiSquare(ContingencyTable observed, ContingencyTable expected) {
		if (observed.m != expected.m || observed.n != expected.n) {
			throw new IllegalArgumentException("Observed and expected table dimensions do not match.");
		}
		this.observed = observed;
		this.expected = expected;
		this.csd = new ChiSquaredDistributionImpl(degreesOfFreedom());
	}
	
	public PearsonChiSquare(ContingencyTable observed) {
		this.observed = observed;
		this.expected = observed.expected();
		this.csd = new ChiSquaredDistributionImpl(degreesOfFreedom());
	}
	
	public int degreesOfFreedom() {
		return (observed.m -1)*(observed.n -1);
	}
	
    /**
     * Returns the p-value (observed significance level) associates with
     * a ChiSquare two sample teste comparing bin frequency counts in
     * <code>observed</code> and <code>expected</code>.
     * 
     * The number returned is the smallest significance level at which one
     * can reject the null hypothesis that the observed and expected counts 
     * conform to the same distribution.
     * 
     * @param observed array of observed frequency counts
     * @param expected array of expected frequency counts
     * @return p-value
     */
	public double pvalue() {
		double x2 = 0;
		for (int i = 0; i < this.observed.m; i++) {
			for (int j = 0; j < this.observed.n; j++) {
				x2 = x2 + (Math.pow(this.observed.table[i][j] - this.expected.table[i][j], 2)/this.expected.table[i][j]);
			}
		}
		try {
			// If x2 is too big the cumulativeProbability does not converge, and after 
			// a long time returns an exception, if x2 cumulative probability is higher
			// than 0.99 do not run the test.
			if (Double.compare(this.csd.inverseCumulativeProbability(0.999),x2) < 0) {
				return 0.0005;
			}
			return 1.0 - this.csd.cumulativeProbability(x2);
		} catch (MathException e) {
			e.printStackTrace();
			return 0.999;
		}
	}
	
	public static void main(String[] args) {
		
		// TODO: REMOVER!!
		int[][] table = {{62, 16}, {94, 10}};
		long[][] ltable = {{62, 16}, {94, 10}};
		ContingencyTable ct = new ContingencyTable(table);
		System.out.println(ct.expected());
		PearsonChiSquare test = new PearsonChiSquare(ct, ct.expected());
		ChiSquareTest cst = new ChiSquareTestImpl();
		System.out.println("EU: " + test.pvalue());
		System.out.println("APACHE: " + cst.chiSquare(ltable));
		ChiSquaredDistribution csd = new ChiSquaredDistributionImpl(test.degreesOfFreedom());
		//System.out.println(Math.exp(Gamma.logGamma(1)));
		try { 
			System.out.println(1.0 - csd.cumulativeProbability(test.pvalue()));
			System.out.println(csd.inverseCumulativeProbability(0.95));
		} catch (Exception e) {
			// TODO: handle exception
		}
		FisherExact fe = new FisherExact(250);
		System.out.println("cutOff: " + fe.getP(62, 16, 94, 10));
		System.out.println("OneTail: " + fe.getCumlativeP(62, 16, 94, 10));
		System.out.println("TwoTail: " + fe.getTwoTailedP(62, 16, 94, 10));
		
		
	}


}
