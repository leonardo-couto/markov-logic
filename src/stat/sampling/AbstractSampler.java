package stat.sampling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math.util.MathUtils;

public abstract class AbstractSampler<T> implements Sampler<T> {
	
	private final long n;
	private final boolean empty;
	protected final List<List<T>> domains;
	protected final int maxSamples;
	
	public AbstractSampler(List<? extends Collection<T>> domains) {
		this(domains, Integer.MAX_VALUE);
	}
	
	public AbstractSampler(List<? extends Collection<T>> domains, int maxSamples) {
		boolean empty = (domains.isEmpty()) ? true : false;
		this.domains = new ArrayList<List<T>>(domains.size());
		
		long n = 1;
		for (Collection<T> domain : domains) {
			this.domains.add(new ArrayList<T>(domain));
			try {
				n = MathUtils.mulAndCheck(n, domain.size());
			} catch (ArithmeticException e) {
				n = Long.MAX_VALUE;
			}
		}
		
		this.empty = (n == 0) ? true : empty;
		this.n = n;
		this.maxSamples = (int) Math.min(n, (long) maxSamples);
	}
	
	public long getCardinality() {
		return this.n;
	}
	
	public int getMaxSamples() {
		return this.maxSamples;
	}
	
	public boolean isEmpty() {
		return this.empty;
	}
	
	@SuppressWarnings("unchecked")
	public <V> Iterator<V> emptyIterator() {
		return Sampler.EMPTY_ITERATOR;
	}
	
}
