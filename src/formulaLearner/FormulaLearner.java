package formulaLearner;

import java.util.List;

import fol.Atom;
import fol.Formula;

public interface FormulaLearner {
	
	
	/**
	 * All formulas learned must contain this atom
	 * @param a Atom that will be used in all learned formulas
	 */
	public void setTarget(Atom a);
	public void putFormulas(List<Formula> formulas);
	public void putFormula(Formula formula);
	//public void setMaxVariables(int n);
	//public void setMaxAtoms(int n);
	public List<Formula> learn();
	//public double[] finalArgs();
	//public double finalScore();

}
