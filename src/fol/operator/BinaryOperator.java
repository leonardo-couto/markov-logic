package fol.operator;

import fol.Formula;
import fol.GeneralFormula;

public interface BinaryOperator extends Operator {
	
	public GeneralFormula apply(Formula f0, Formula f1);

}
