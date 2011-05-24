package formulaLearner;

import java.util.List;

import fol.Atom;
import fol.Formula;

public interface FormulaLearner {
	
	
	/**
	 * All formulas learned must contain this atom. Exceptions
	 * are Atomic formulas (it might contain an atomic formula
	 * for a Atom different than the target Atom), formulas
	 * that where added by putFormula(s) method, and formulas
	 * already learned before calling this method.
	 * @param a Atom that will be used in all learned formulas
	 */
	public void setTarget(Atom a);
	public void putFormulas(List<Formula> formulas);
	public void putFormula(Formula formula);
	public List<Formula> learn();

}
