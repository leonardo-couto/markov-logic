package stat.benchmark;

import java.util.Comparator;

public class InputPoint {
	
	public final double observed;
	public final boolean expected;

	public InputPoint(double observed, boolean expected) {
		this.observed = observed;
		this.expected = expected;
	}
	
	public static Comparator<InputPoint> observedComparator(final boolean descending) {
		return new Comparator<InputPoint>() {

			@Override
			public int compare(InputPoint o1, InputPoint o2) {
				int comp = Double.compare(o2.observed, o1.observed);
				return descending ? comp : -comp;
			}
			
		};
	}

}
