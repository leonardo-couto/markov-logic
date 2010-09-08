package fol;

import java.util.List;
import java.util.Set;


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
		if(Double.isNaN(formulas.get(0).getValue())) {
			return Double.NaN;
		} else {
			return 1.0d - formulas.get(0).getValue();
		}
	}
	
	@Override
	protected String print() {
		return toString();
	}	

	@Override
	public String toString() {
		return "!" + formulas.get(0).print();
	}
	
	@Override
	protected String operator() {
		return null;
	}	

	@Override
	protected List<Formula> colapse(List<Formula> fa) {
		return formulas;
	}
	
	public static Formula negatedFormula(Formula f) {
		if (f instanceof NegatedFormula) {
			return ((NegatedFormula) f).formulas.get(0);
		}
		return new NegatedFormula(f);
	}

	@Override
	public Formula copy() {
		return new NegatedFormula(formulas.get(0).copy());
	}
	
	/* (non-Javadoc)
	 * @see fol.Formula#getPredicates()
	 */
	@Override
	public Set<Predicate> getPredicates() {
		return formulas.get(0).getPredicates();
	}

}
