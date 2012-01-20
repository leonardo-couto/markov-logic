package stat;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Represents a Set of RandomVariable's distribution.
 * Each class that implements RandomVariable should have an
 * class that implements this interface.
 */
public interface Distribution<T> {
	
	public boolean add(T e);
	public boolean addAll(Collection<? extends T> c);
	public boolean remove(T e);

	/**
	 * @return This variable marginal data.
	 */
	public Iterator<Boolean> getDataIterator(T x);
	public Iterator<boolean[]> getDataIterator(T x, T y);
	public Iterator<boolean[]> getDataIterator(T x, T y, List<T> z);
	
	public Set<T> getRandomVariables();
}
