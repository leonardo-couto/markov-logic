package fol.operator;

import fol.FormulaComponent;

public interface Operator extends FormulaComponent {
	
	public static final Character LEFTP = '(';
	public static final Character RIGHTP = ')';
	
	public String getSymbol();

}
