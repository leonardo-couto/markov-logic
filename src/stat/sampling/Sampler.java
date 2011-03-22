package stat.sampling;

import java.util.Iterator;
import java.util.List;

public interface Sampler<T> extends Iterable<List<T>> {
	
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
