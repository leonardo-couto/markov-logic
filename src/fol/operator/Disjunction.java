package fol.operator;

import fol.Formula;


/**
 * @author Leonardo Castilho Couto
 *
 */
public final class Disjunction extends Operator {
	
	private static final String disjunctionOP = "v";
	private static final int disjunctionArity = 2;
	public static final Operator operator = new Disjunction();

	/**
	 * @param formulas
	 */
	public Disjunction() {
		super(disjunctionArity, disjunctionOP);
	}
	
	@Override
	protected double _value(double ... values) {
		return or(values[0], values[1]);
	}

	@Override
	protected Formula _getFormula(Formula... formulas) {
		return Formula.twoArityOp(this, formulas[0], formulas[1]);
	}

	@Override
	public String toString(String ... formulas) {
		return "( " + formulas[0] + " ) " + disjunctionOP + " ( " + formulas[1] + " )";
	}

	@Override
	public String toString() {
		return disjunctionOP;
	}

	public static double or(double d1, double d2) {
		if ( Double.isNaN(d1) ) {
			if (Double.isNaN(d2)) {
				return Double.NaN;
			}
			if (Double.compare(d2, 0.9d) > 0) {
				// Ignore errors smaller than 0.05.
				return ((d2+1.0d)/2.0d);
			}
			return Double.NaN;
		} else {
			if (Double.isNaN(d2)) {
				if (Double.compare(d1, 0.9d) > 0) {
					// Ignore errors smaller than 0.05.
					return ((d1+1.0d)/2.0d);
				}
				return Double.NaN;
			}
		}
		return d1 + d2 - d1*d2;
	}

}
