package stat.sampling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Given k List<T> <code>domains</code>, each with m_k elements 
 * generates <code>n</code> k-tuples of elements (x1_d1, x2_d2, ... , xk_dk).
 * Not efficient for large values of <code>n</code>.
 */
public class CrossJoinSampler<T> extends AbstractSampler<T> {
	
	public CrossJoinSampler(List<? extends Collection<T>> domains) {
		super(domains);
	}
	
	@Override
	public Iterator<List<T>> iterator() {
		if (this.isEmpty()) {
			return this.emptyIterator();
		}
		
		int n = (int) Math.min(this.getCardinality(), (long) this.maxSamples);
		return this.crossJoin(n).iterator();
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
	private List<List<T>> crossJoin(int n) {
		int[] counter = new int[this.domains.size()+1];
		Arrays.fill(counter, 0);
		List<List<T>> out = new ArrayList<List<T>>(n);
		for (int i = 0; i < n; i++) {
			List<T> el = new ArrayList<T>(this.domains.size());
			for (int j = 0; j < this.domains.size(); j++) {
				if (counter[j] == this.domains.get(j).size()) {
					counter[j] = 0;
					counter[j+1]++;
				}
				el.add((T) this.domains.get(j).get(counter[j]));
			}
			counter[0]++;
			out.add(el);
		}
		Collections.shuffle(out);
		return out;
	}


}
