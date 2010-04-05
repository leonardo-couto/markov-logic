package fol;


/**
 * @author Leonardo Castilho Couto
 *
 */
public class NegatedFormula extends Formula {
	
	/**
	 * @param formula
	 */
	private NegatedFormula(Formula formula) {
		super(formula);
	}
	
	public double getValue() {
		if(Double.isNaN(formulas[0].getValue())) {
			return Double.NaN;
		} else {
			return 1.0d - formulas[0].getValue();
		}
	}
	
	@Override
	protected String print() {
		return toString();
	}	

	@Override
	public String toString() {
		return "!" + formulas[0].print();
	}
	
	@Override
	protected String operator() {
		return null;
	}	

	@Override
	protected Formula[] colapse(Formula[] fa) {
		return null;
	}
	
	public static Formula negatedFormula(Formula f) {
		if (f instanceof NegatedFormula) {
			return ((NegatedFormula) f).formulas[0];
		}
		return new NegatedFormula(f);
	}

	@Override
	public Formula copy() {
		return new NegatedFormula(formulas[0].copy());
	}

}
