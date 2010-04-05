package util;

import java.util.Arrays;

public class ArrayPointer<T> {
	
	public final T[] a;
	public final int i;
	public final T original;
	
	public ArrayPointer(T[] a, int i) {
		this.a = a;
		this.i = i;
		if ((i < 0) || (a.length < i)) {
			throw new IllegalArgumentException("Index out of bounds");
		}
		this.original = a[i]; // pointer to the original element
	}
	
	public T get() {
		return a[i];
	}
	
	public void set(T t) {
		a[i] = t;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(a);
		result = prime * result + i;
		result = prime * result
				+ ((original == null) ? 0 : original.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ArrayPointer<T> other;
		try { 
			other = (ArrayPointer<T>) obj;
		} catch (ClassCastException e) {
			return false;
		}
		if (!Arrays.equals(a, other.a))
			return false;
		if (i != other.i)
			return false;
		if (original == null) {
			if (other.original != null)
				return false;
		} else if (!original.equals(other.original))
			return false;
		return true;
	}
	

}
