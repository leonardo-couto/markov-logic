package fol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Leonardo Castilho Couto
 *
 */
public class Conjunction extends Formula {

	/**
	 * @param formulas
	 */
	public Conjunction(List<Formula> formulas) {
		super(formulas);
	}
	
	public Conjunction(Formula ... formulas) {
		super(formulas);
	}

	@Override
	protected String operator() {
		return "^";
	}	
	
	@Override
	public double getValue() {
		double[] values = new double[formulas.length];
		int i = 0;
		for (Formula f : formulas) {
			values[i] = f.getValue();
			if (Double.compare(values[i], 0.02d) < 0) {
				return 0.0d;
			}
			i++;
		}
		Arrays.sort(values); // this put all Double.NaN in the end of the array.
		if (Double.isNaN(values[0])) {
			return Double.NaN;
		}
		double out = values[0];
		for(i = 1; i < values.length; i++) {
			if (Double.isNaN(values[i])) {
				if (Double.compare(out, 0.1d) < 0) {
					// Ignore errors smaller than 0.05.
					return (out/2.0d);
				}
				out = and(out, values[i]);
			}
		}
		return out;		
	}
	
	public static double and(double d1, double d2) {
		return d1*d2;
	}
	
	@Override
	protected Formula[] colapse(Formula[] fa) {
		List<Formula> out = new ArrayList<Formula>();
		for (Formula f : fa) {
			if (f instanceof Conjunction) {
				for (Formula f1 : f.formulas) {
					out.add(f1);
				}
			} else {
				out.add(f);
			}
		}
		Collections.sort(out);
		return out.toArray(new Formula[out.size()]);
	}

	@Override
	public Formula copy() {
		Formula[] newFormulas = new Formula[formulas.length];
		for (int i = 0; i < newFormulas.length; i++) {
			newFormulas[i] = formulas[i].copy();
		}
		return new Conjunction(newFormulas);
	}

}
