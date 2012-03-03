package stat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeightedRV<T> {
	
	public final T rv;
	public final double value;
	
	public static final Comparator<WeightedRV<?>> valueComparator = new Comparator<WeightedRV<?>>() {

		@Override
		public int compare(WeightedRV<?> o1, WeightedRV<?> o2) {
			return Double.compare(o1.value, o2.value);
		}
		
	};
	
	public WeightedRV(T rv, double value) {
		this.rv = rv;
		this.value = value;
	}

	/**
	 * Convert a List of WeightedRV, into a List of RandomVariable.
	 * @param list
	 * @return
	 */
	public static <E> List<E> getRvList(List<WeightedRV<E>> list) {
		List<E> out = new ArrayList<E>(list.size());
		for (WeightedRV<E> wrv : list) {
			out.add(wrv.rv);
		}
		return out;
	}
	
	/**
	 * Convert a List of WeightedRV, into a Map<RandomVariable,Double>.
	 * @param list
	 * @return
	 */
	public static <E> Map<E,Double> getMap(List<WeightedRV<E>> list) {
		Map<E, Double> out = new HashMap<E, Double>(2*list.size());
		for (WeightedRV<E> wrv : list) {
			out.put(wrv.rv, wrv.value);
		}
		return out;
	}

}
