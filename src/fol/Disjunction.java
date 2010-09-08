package fol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Leonardo Castilho Couto
 *
 */
public class Disjunction extends Formula {

	/**
	 * @param formulas
	 */
	public Disjunction(List<Formula> formulas) {
		super(formulas);
		
	}
	
	public Disjunction(Formula ... formulas) {
		super(formulas);
	}
	
	@Override
	protected String operator() {
		return "v";
	}	

	@Override
	public double getValue() {
		double[] values = new double[formulas.size()];
		int i = 0;
		for (Formula f : formulas) {
			values[i] = f.getValue();
			if (Double.compare(values[i], 0.98d) > 0) {
				return 1.0d;
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
				if (Double.compare(out, 0.9d) > 0) {
					// Ignore errors smaller than 0.05.
					return ((out + 1.0d)/2.0d);
				}
				out = or(out, values[i]);
			}
		}
		return out;
	}
	
	public static double or(double d1, double d2) {
		return d1 + d2 - d1*d2;
	}

	@Override
	protected List<Formula> colapse(List<Formula> fa) {
		List<Formula> out = new ArrayList<Formula>();
		for (Formula f : fa) {
			if (f instanceof Disjunction) {
				for (Formula f1 : f.formulas) {
					out.add(f1);
				}
			} else {
				out.add(f);
			}
		}
		Collections.sort(out);
		return out;
	}

	@Override
	public Formula copy() {
		List<Formula> newFormulas = new ArrayList<Formula>(formulas.size());
		for (Formula f : formulas) {
			newFormulas.add(f.copy());
		}
		return new Disjunction(newFormulas);
	}

}
