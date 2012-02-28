package stat;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * This class is used to uniformly picks a random element from a List or Array.
 */
public class ElementPicker {
	
	private static final Object LOCK = new Object();
	
	private static volatile int maxSize = 1000; // ~ 8 kB
	private static volatile double[] probabilities = extend(new double[0], 100);
	
	private final Random random;
	
	public ElementPicker(Random random) {
		this.random = random;
	}
	
	/**
	 * Randomly pick a element
	 */
	public <T> T pick(List<T> list) {
	    int size = list.size();
	    double[] prob = getProbabilities(size);
	    for (int i = size-1; i > 0; i--) {
	        if (prob[i] > this.random.nextDouble()) {
	            return list.get(i);
	        }
	    }
	    return list.get(0);
	}
	
	/**
	 * Randomly pick a element from array
	 * @param array 
	 * @param random
	 * @return
	 */
	public <T> T pick(T[] array) {
	    int size = array.length;
	    double[] prob = getProbabilities(size);
	    for (int i = size-1; i > 0; i--) {
	        if (prob[i] > this.random.nextDouble()) {
	            return array[i];
	        }
	    }
	    return array[0];
	}
	
	private static double[] extend(double[] original, int size) {
		int initialSize = original.length;
		double[] copy = Arrays.copyOf(original, size);
		for (int i = initialSize; i < size;) {
			copy[i] = 1.0 / ++i;
		}
		return copy;
	}
	
	/**
	 * Returns a cached array of cached probabilities for
	 * uniformly picking a element of a List/Array.
	 * @param size the Array/List size.
	 * @return cached array of probabilities.
	 */
	private static double[] getProbabilities(int size) {
		double[] prob = probabilities;
		if (size > prob.length) {
			if (size > maxSize) {
				if (prob.length < maxSize) {
					prob = getProbabilities(maxSize);
				}			
				return extend(prob, size);
			}
			synchronized (LOCK) {
				if (size > probabilities.length) {
					probabilities = extend(probabilities, size);
				}
				return probabilities;
			}
		} else {
			return prob;
		}
	}
	
	/**
	 * Set the cache size.
	 * @param n max number of doubles stored in cache. Default is 1000.
	 */
	public static void setCache(int n) {
		synchronized (LOCK) {
			if (n < maxSize && n < probabilities.length) {
				probabilities = Arrays.copyOf(probabilities, n);
			}
			maxSize = n;
		}
	}
	
}
