package fol;

import java.util.Deque;

/**
 * Represent the components of {@link GeneralFormula} 
 * ({@link Atom}s and {@link Operator}s).
 */
public interface FormulaComponent {
	
	/**
	 * Evaluate a {@link GeneralFormula}.
	 * 
	 * Formulas in the <code>GeneralFormula</code> class are in Reverse 
	 * Polish Notation (Posfix). When evaluating it each formula component 
	 * handle and modify the stack of values thru this method.
	 * 
	 * We use the Deque interface because of performance issues in the
	 * Stack class.
	 * 
	 * @param stack boolean values evaluated so far
	 * @param db database of atom values
	 * @throws NullPointerException if stack or Database are null
	 */
	public void evaluate(Deque<Boolean> stack, Database db);
	
	/**
	 * Prints a {@link GeneralFormula}.
	 * 
	 * Formulas in the <code>GeneralFormula</code> class are in Reverse 
	 * Polish Notation (Posfix). When printing it each formula component 
	 * handle and modify the stack of printed components thru this method.
	 * 
	 * We use the Deque interface because of performance issues in the
	 * Stack class.
	 * 
	 * @param stack printed components evaluated so far
	 * @throws NullPointerException if stack is null
	 */
	public void print(Deque<StringBuilder> stack);

}
