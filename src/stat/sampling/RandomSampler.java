package stat.sampling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Picks a random next element, does not care about repetition.
 * Very efficient, but the repetitions may be an issue if the
 * number of distinct samples is small.
 */
public class RandomSampler<T> extends AbstractSampler<T> {
	
	private final List<Random> random;

	public RandomSampler(List<? extends Collection<T>> domains, int maxSamples) {
		super(domains, maxSamples);
		this.random = new ArrayList<Random>();
		for (int i = 0; i < domains.size(); i++) {
			this.random.add(new Random());
		}
	}

	public RandomSampler(List<? extends Collection<T>> domains) {
		this(domains, Integer.MAX_VALUE);
	}

	@Override
	public Iterator<List<T>> iterator() {
		if (this.isEmpty()) {
			return this.emptyIterator();
		}
		
		return new Iterator<List<T>>() {
			
			private int i = 0;

			@Override
			public boolean hasNext() {
				if (i > maxSamples) {
					return false;
				}
				return true;
			}

			@Override
			public List<T> next() {
				i++;
				List<T> out = new ArrayList<T>(domains.size());
				for(int i = 0; i < domains.size(); i++) {
					List<T> domain = domains.get(i);
					Random r = random.get(i);
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

}
