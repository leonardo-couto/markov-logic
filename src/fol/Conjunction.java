package fol;


/**
 * @author Leonardo Castilho Couto
 *
 */
public class Conjunction extends Operator {
	
	private static final String conjunctionOP = "^";
	private static final int conjunctionArity = 2;
	public static final Operator operator = new Conjunction();

	/**
	 * @param formulas
	 */
	private Conjunction() {
		super(conjunctionArity, conjunctionOP);
	}
	
	@Override
	public double _value(double ... values) {
		return and(values[0], values[1]);
	}

	@Override
	protected Formula _getFormula(Formula... formulas) {
		return Formula.twoArityOp(this, formulas[0], formulas[1]);
	}

	@Override
	public String toString(String ... formulas) {
		return "( " + formulas[0] + " ) " + conjunctionOP + " ( " + formulas[1] + " )";
	}

	@Override
	public String toString() {
		return conjunctionOP;
	}
	
	public static double and(double d1, double d2) {
		if ( Double.isNaN(d1) ) {
			if (Double.compare(d2, 0.1d) < 0) {
				// Ignore errors smaller than 0.05.
				return (d2/2.0d);
			}
			return Double.NaN;
		} else {
			if (Double.isNaN(d2)) {
				if (Double.compare(d1, 0.1d) < 0) {
					// Ignore errors smaller than 0.05.
					return (d1/2.0d);
				}
				return Double.NaN;
			}
		}
		return d1*d2;
	}
	
}
