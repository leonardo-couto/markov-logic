package fol.operator;

import fol.Formula;

/**
 * @author Leonardo Castilho Couto
 *
 */
public abstract class Operator {

	protected final int arity;
	protected final String operator;

	protected abstract double _value(double ... values);
	protected abstract Formula _getFormula(Formula ... formulas);

	protected Operator(int arity, String operator) {
		this.arity = arity;
		this.operator = operator;
	}

	/**
	 * Produces a new Formula as a combination of n Formulas and/or Atoms
	 * under this Operator, where n is the Operator's arity. If the number
	 * of Formulas differs from the arity, throws an IllegalArgumentException.
	 * @param formulas Formulas to be combined by this operator
	 * @return a new Formula.
	 */
	public final Formula getFormula(Formula ... formulas) {
		if(formulas.length != arity) {
			throw new IllegalArgumentException("Expected " + arity + 
					"formulas(s), found "+ formulas.length + ".");
		}
		return _getFormula(formulas);
	}

	/**
	 * Gives the value of the combination of n values under this Operator, 
	 * where n is the Operator's arity. If the number of values differs from 
	 * the arity, throws an IllegalArgumentException.
	 * @param values The values to be calculated by this operator
	 * @return double.
	 */
	public final double value(double ... values) {
		if(values.length != arity) {
			throw new IllegalArgumentException("Expected " + arity + 
					"value(s), found "+ values.length + ".");
		}
		return _value(values);
	}

	public final int getArity() {
		return this.arity;
	}

	public final String getOperator() {
		return this.operator;
	}

	public abstract String toString(String ... formulas);

}
