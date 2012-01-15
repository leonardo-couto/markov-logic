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
public interface Distribution<RV extends RandomVariable<?>> {
	
	public boolean add(RV e);
	public boolean addAll(Collection<? extends RV> c);
	public boolean remove(RV e);

	/**
	 * @return This variable marginal data.
	 */
	public Iterator<Boolean> getDataIterator(RV x);
	public Iterator<boolean[]> getDataIterator(RV x, RV y);
	public Iterator<boolean[]> getDataIterator(RV x, RV y, List<RV> z);
	
	public Set<RV> getRandomVariables();
}
