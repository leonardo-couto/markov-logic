package stat;

import java.util.Set;

/**
 * @author Leonardo Castilho Couto
 *
 */
public interface IndependenceTest<RV extends RandomVariable<RV>> {
	
	/**
	 * @return true if X and Y are independent given Z.
	 */
	public boolean test(RV x, RV y, Set<RV> z);
	
	/**
	 * @return true if pvalue > alpha.
	 */
	public boolean test(double pvalue);
	
	/**
	 * Return the probability of obtaining data at least as extreme 
	 * as the one observed from X and Y assuming that X and Y are
	 * independent.
	 * High pvalues means X and Y are more likely to be independent,
	 * low pvalues, more likely to be dependent.
	 * @return Likelihood of independence [0,1].
	 */
	public double pvalue(RV x, RV y);

}
