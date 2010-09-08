package fol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Leonardo Castilho Couto
 *
 */
public class Biconditional extends Formula {

	/**
	 * @param formulas
	 */
	public Biconditional(List<Formula> formulas) {
		super(formulas);
		
	}
	
	public Biconditional(Formula ... formulas) {
		super(formulas);
	}
	
	@Override
	protected String operator() {
		return "<->";
	}	

	@Override
	public double getValue() {
		double[] values = new double[formulas.size()];
		int i = 0;
		for (Formula f : formulas) {
			values[i] = f.getValue();
			if (Double.isNaN(values[i])) {
				return Double.NaN;
			}
			i++;
		}
		double out = values[0];
		for(i = 1; i < values.length; i++) {
			out = bic(out, values[i]);
		}
		return out;
	}
	
	public static double bic(double d1, double d2) {
		return Disjunction.or(Conjunction.and(d1, d2), Conjunction.and(1.0-d1,1.0-d2));
	}

	@Override
	protected List<Formula> colapse(List<Formula> fa) {
		List<Formula> out = new ArrayList<Formula>();
		for (Formula f : fa) {
			if (f instanceof Biconditional) {
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
		return new Biconditional(newFormulas);
	}

}
