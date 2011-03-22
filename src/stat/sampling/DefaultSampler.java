package stat.sampling;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;


/**
 * Chooses a Sampler based on the total distinct samples
 * and the maxSamples limit.
 */
public class DefaultSampler<T> extends AbstractSampler<T> {
	
	private AbstractSampler<T> sampler;
	private int crossJoinLimit = 100; // default
	private int noReplacementLimit = 500;
	private int treeLimit = 10000;
	
	public DefaultSampler(List<? extends Collection<T>> domains) {
		this(domains, Integer.MAX_VALUE);
	}
	
	public DefaultSampler(List<? extends Collection<T>> domains, int maxSamples) {
		super(domains);
		this.setMaxSamples(maxSamples);
	}

	public Iterator<List<T>> iterator() {
		return sampler.iterator();
	}

	/**
	 * Sets the max number of samples and chooses the sampler
	 * @param n
	 */
	public void setMaxSamples(int n) {
		long card = sampler.getCardinality();
		if (card < this.crossJoinLimit) {
			if (n >= card) {
				if (!(this.sampler instanceof CrossJoinSampler<?>)) {
					this.sampler = new CrossJoinSampler<T>(this.domains);
				}
			} else {
				this.sampler = new TreeSampler<T>(this.domains, n); 
			}
		} else if (n > this.noReplacementLimit) {
			if ( (n > this.treeLimit) || (Double.compare((n/card), 0.3) > 1)) {
				this.sampler = new RandomSampler<T>(domains, n);
			} else {
				this.sampler = new TreeSampler<T>(this.domains, n); 
			}
		} else {
			this.sampler = new TreeSampler<T>(this.domains, n); 
		}
	}
	
	@Override
	public int getMaxSamples() {
		return this.sampler.maxSamples; 
	}
}