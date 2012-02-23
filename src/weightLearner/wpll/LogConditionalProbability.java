package weightLearner.wpll;

import java.util.Iterator;
import java.util.List;

import fol.Atom;

public class LogConditionalProbability {
	
	private final double[] weights;
	
	public LogConditionalProbability(double[] weights) {
		this.weights = weights;
	}

	/**
	 * <p>Evaluate the pseudo-log-likelihood for a predicate and grounding of this predicate.</p>
	 * 
	 * <code>log[P*(ground)] = log[P(ground | MB(ground))],</code><br>
	 * <code>P(g|MB(g)) = wx / ( a + b )</code><br>
	 * <code>wx = sum_i(w_i * n_i[ground = x    ])</code><br>
	 * <code>a  = sum_i(w_i * n_i[ground = true ])</code><br>
	 * <code>b  = sum_i(w_i * n_i[ground = false])</code>
	 * 
	 * @param counts represents a list of true/false counts for the 
	 * same predicate, and same grounding of this predicate.
	 * @return
	 */
	public DataPll evaluate(List<Count> counts) {

		Atom grounding = null;
		double wx = 0, a = 0, b = 0, pll = 0;
		double[] grad = new double[this.weights.length];
		
		
		Iterator<Count> iterator = counts.iterator();
		for (double weight : this.weights) {
			// check end
			if (!iterator.hasNext()) break;
			Count count = iterator.next();
			if (count == null) continue;
			if (grounding == null) grounding = count.getAtom();
				
			// summation
			wx += weight*count.getCount();
			a  += weight*count.getTrueCount();
			b  += weight*count.getFalseCount();
		}
		
		// exp = e^(abs(a-b)), invexp = exp^-1
		// if invexp ~ 0, ignore it.
		boolean ignoreExp = false;
		double exp = 0, invexp = 0;
		double diff = Math.abs(a - b);
		if (Double.compare(diff, 20) > -1) {
			ignoreExp = true;
		} else {
			exp = Math.exp(diff);
			invexp = Math.exp(-diff);
		}
		
		double groundpll = wx - Math.max(a, b);
		if (!ignoreExp) groundpll += - Math.log(1+invexp);
		pll += groundpll;

		// compute the partial derivative with respect to w_i
		// if a > b then the derivative = 
		// n_i[x] - n_i[true] + (n_i[true] - n_i[false])/(1+exp)
		iterator = counts.iterator();
		for (int i = 0; i < this.weights.length; i++) {
			// check end
			if (!iterator.hasNext()) break;
			Count count = iterator.next();
			if (count == null) continue;
			
			double x  = count.getCount();
			double tc = count.getTrueCount();
			double fc = count.getFalseCount();
			
			if (a > b) {
				grad[i] += x - tc;
				if(!ignoreExp) {
					grad[i] += (tc-fc)/(exp+1.0);
				}

			} else {
				grad[i] += x - fc;			
				if(!ignoreExp) {
					grad[i] += (fc-tc)/(exp+1.0);
				}
			}
			
		}
		
		return new DataPll(grounding, pll, grad);
	}
	
}
