package stat;

import java.util.List;

import util.Convergent;

/**
 * @author Leonardo Castilho Couto
 *
 * @param <T>
 */
public class ConvergenceMethodTester<T> {
	
	private final Convergent<T> convergent;
	private final ConvergenceTester tester;
	
	public ConvergenceMethodTester(Convergent<T> convergent, ConvergenceTester tester) {
		this.convergent = convergent;
		this.tester = tester;		
	}
	
	public boolean hasConverged() {
		return tester.hasConverged();
	}

	public boolean increment(List<T> args) {
	  return tester.increment(convergent.method(args));
	}

	public boolean evaluate(List<List<T>> values) {
	  double[] dValues = new double[values.size()];
	  for (int i = 0; i < dValues.length; i++) {
	    dValues[i] = convergent.method(values.get(i));
	  }
	  return tester.evaluate(dValues);
	}
  
}
