package stat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.commons.math.util.MathUtils;

public class Sampler<T> implements Iterable<List<T>> {
	
	private final List<List<T>> domains;
	private final boolean shuffle;
	private final boolean noReplacement;
	public final long n;
	private static final int SHUFFLE_LIMIT = 50; //50;
	private static final int NO_REPLACEMENT_LIMIT = 500; //500;
	private int maxSamples = -1;
	private boolean hasSampleLimit = false;
	private boolean empty = false;
	
	public Sampler(Collection<T> domain) {
		this(Collections.singletonList(domain));
	}

	public Sampler(List<? extends Collection<T>> domains) {
		
		boolean shuffle = true;
		boolean noReplacement = true;
		if(domains.isEmpty()) {
			this.empty = true;
		}
		final List<List<T>> objDomains = new ArrayList<List<T>>(domains.size());
		long n = 1;
		for (int i=0; i < domains.size(); i++ ) {
			objDomains.add(new ArrayList<T>(domains.get(i)));
			n = MathUtils.mulAndCheck(n, domains.get(i).size());

			// test for population size
			if (noReplacement && n < NO_REPLACEMENT_LIMIT) {

				if (shuffle && n > SHUFFLE_LIMIT) {
					shuffle = false;
				}
				if (n > NO_REPLACEMENT_LIMIT) {
					noReplacement = false;
				}
			}
		}
		if (n == 0) {
			this.empty = true;
		}
		
		this.shuffle = shuffle;
		this.noReplacement = noReplacement;
		this.n = n;
		this.domains = objDomains;
	}

	// criar e manter uma arvore de escolhas jah feitas, ex:
	// a {a0, a1}, b {b0, b1}, c {c0, c1}
	//      b0 - c0
	//    /
	// a0 - b1 - c0
	//
	// escolher randomicamente cada ramo da arvore, se já estiver lotado, escolher outro ramo
	// backtraking, manter uma flag se jah esta cheio.
	@SuppressWarnings("unchecked")
	public Iterator<List<T>> iterator() {
		
		if (this.empty) {
			return (Iterator<List<T>>) EMPTY_ITERATOR;
		}
		
		if (shuffle) {
			List<List<T>> out = crossJoin(this.domains, (int) this.n);
			if (hasSampleLimit) {
				if (out.size() > this.maxSamples) {
					out = out.subList(0, maxSamples);
				}
			}
			return out.iterator();
		}

		if (noReplacement) {
			if (hasSampleLimit) {
				return (new MultiVarRandomIterator<T>(this.domains)).setMaxSamples(this.maxSamples);
			}
			return new MultiVarRandomIterator<T>(this.domains);
		}

		return new Iterator<List<T>>() {
			
			private int i = 0;
			private final Random r = new Random();

			@Override
			public boolean hasNext() {
				if (hasSampleLimit) {
					if (i > maxSamples) {
						return false;
					}
				}
				return true;
			}

			@Override
			public List<T> next() {
				i++;
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
	
	public void setMaxSamples(int n) {
		this.maxSamples = n;
		this.hasSampleLimit = true;
	}
	
	public int getMaxSamples() {
		return this.maxSamples; 
	}
	
	@SuppressWarnings("rawtypes")
	public static final Iterator EMPTY_ITERATOR = new Iterator() {

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public Object next() {
			return null;
		}

		@Override
		public void remove() {			
		}
	};
	
}