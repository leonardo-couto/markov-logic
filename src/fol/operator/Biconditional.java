package fol.operator;

import fol.Formula;


/**
 * @author Leonardo Castilho Couto
 *
 */
public final class Biconditional extends Operator {
	
	private static final String biconditionalOP = "<->";
	private static final int biconditionalArity = 2;
	public static final Operator operator = new Biconditional();

	/**
	 * @param formulas
	 */
	private Biconditional() {
		super(biconditionalArity, biconditionalOP);
	}
	
	@Override
	protected double _value(double ... values) {
		return bic(values[0], values[1]);
	}

	@Override
	protected Formula _getFormula(Formula... formulas) {
		return Formula.twoArityOp(this, formulas[0], formulas[1]);
	}

	@Override
	public String toString(String ... formulas) {
		return "( " + formulas[0] + " ) " + biconditionalOP + " ( " + formulas[1] + " )";
	}

	@Override
	public String toString() {
		return biconditionalOP;
	}

	public static double bic(double d1, double d2) {
		return Disjunction.or(
				Conjunction.and(d1, d2), 
				Conjunction.and(1.0-d1,1.0-d2)
			   );
	}

}
