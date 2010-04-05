package util;

import java.util.Comparator;

public class NameComparator implements Comparator<NameID> {

	@Override
	public int compare(NameID o1, NameID o2) {
		return o1.getName().compareTo(o2.getName());
	}

}
