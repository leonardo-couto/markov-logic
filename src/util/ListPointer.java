package util;

import java.util.List;

public class ListPointer<T> {
	
	public final List<T> a;
	public final int i;
	public final T original;
	
	public ListPointer(List<T> a, int i) {
		this.a = a;
		this.i = i;
		this.original = a.get(i); // pointer to the original element
	}
	
	public T get() {
		return a.get(i);
	}
	
	public void set(T t) {
		a.set(i, t);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((a == null) ? 0 : a.hashCode());
		result = prime * result + i;
		result = prime * result
				+ ((original == null) ? 0 : original.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ListPointer<?>))
			return false;
		ListPointer<?> other = (ListPointer<?>) obj;
		if (a == null) {
			if (other.a != null)
				return false;
		} else if (!a.equals(other.a))
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
