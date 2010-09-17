package stat;

import java.util.Iterator;
import java.util.List;

/**
 * @author Leonardo Castilho Couto
 *
 * @param <T>
 */
public class ConvergenceTesterIterator<T> implements Iterator<List<T>> {
	
	private final Iterator<List<T>> sampler;
	private final ConvergenceMethodTester<T> tester;
	
	public ConvergenceTesterIterator(Iterator<List<T>> sampler, ConvergenceMethodTester<T> tester) {
	  this.tester = tester;
		this.sampler = sampler;
	}
	
	public boolean hasConverged() {
	  return tester.hasConverged();
	}
	
	@Override
	public boolean hasNext() {
		return sampler.hasNext();
	}

	@Override
	public List<T> next() {
		List<T> next = sampler.next();
		tester.increment(next);
		return next;
	}

	@Override
	public void remove() {
		// Do nothing.
	}

}
