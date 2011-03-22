package stat.sampling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public class RandomIterator<T> implements Iterable<T> {
	
	private final Collection<T> data;
	private final int maxSamples;
	
	public RandomIterator(Collection<T> c) {
		this(c,Integer.MAX_VALUE);
	}
	
	public RandomIterator(Collection<T> c, int maxSamples) {
		this.data = c;
		this.maxSamples = Math.min(c.size(), maxSamples);
	}

	@Override
	public Iterator<T> iterator() {
		
		final ArrayList<T> copy = new ArrayList<T>(data);
		Collections.shuffle(copy);
		
		return new Iterator<T>() {
			
			int max = maxSamples -1;
			int i = -1;

			@Override
			public boolean hasNext() {
				return (i < max);
			}

			@Override
			public T next() {
				i = i + 1;
				return (i < maxSamples) ? copy.get(i) : null;
			}

			@Override
			public void remove() {
				// do nothing				
			}
		};
	}

}
