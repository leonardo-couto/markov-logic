package fol;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Leonardo Castilho Couto
 *
 */
public final class Negation extends Operator {
	
	private static final String negationOP = "!";
	private static final int negationArity = 1;
	public static final Operator operator = new Negation();
	
	/**
	 * @param formula
	 */
	private Negation() {
		super(negationArity, negationOP);
	}
	
	@Override
	protected double _value(double ... values) {
		return not(values[0]);
	}

	@Override
	protected Formula _getFormula(Formula... formulas) {
		List<Operator> operators = formulas[0].getOperators();
		if (operators != null && operators.get(operators.size()).equals(this)) {
			// Double negation
			operators = new ArrayList<Operator>(operators);
			operators.remove(operators.size()-1);
			List<Boolean> stack = new ArrayList<Boolean>(formulas[0].getStack());
			stack.remove(stack.size()-1);
			return new Formula(new ArrayList<Atom>(formulas[0].getAtoms()), 
					operators, stack);
		}
		return Formula.oneArityOp(this, formulas[0]);
	}

	@Override
	public String toString(String ... formulas) {
		return negationOP + "( " + formulas[0] + " )";
	}

	@Override
	public String toString() {
		return negationOP;
	}
	
	public static double not(double d1) {
		return 1.0d - d1;
	}

}
