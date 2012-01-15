package fol.operator;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import fol.Database;
import fol.Formula;
import fol.FormulaComponent;
import fol.GeneralFormula;

public final class Disjunction implements Operator {
	
	private static final String DISJUNCTION_OP = "v";
	private static final String DISJUNCTION_CONNECTOR = " v ";
	public static final Disjunction OPERATOR = new Disjunction();

	public Disjunction() {
		super();
	}
	
	public GeneralFormula apply(Formula f0, Formula f1) {
		List<FormulaComponent> l0 = f0.getComponents();
		List<FormulaComponent> l1 = f1.getComponents();
		List<FormulaComponent> formula = new ArrayList<FormulaComponent>(l0.size()+l1.size()+1);
		formula.addAll(l0);
		formula.addAll(l1);
		formula.add(this);
		return new GeneralFormula(formula);
	}

	@Override
	public void evaluate(Deque<Boolean> stack, Database db) {
		stack.push(Boolean.valueOf(
				stack.pop().booleanValue() | stack.pop().booleanValue()));
	}

	@Override
	public void print(Deque<StringBuilder> stack) {
		StringBuilder s1 = stack.pop();
		StringBuilder s2 = stack.pop();
		StringBuilder out = new StringBuilder(s1.length()+s2.length()+6);
		stack.push(out.append(Operator.LEFTP)
				.append(s1)
				.append(DISJUNCTION_CONNECTOR)
				.append(s2)
				.append(Operator.RIGHTP)
				);
	}

	@Override
	public String getSymbol() {
		return DISJUNCTION_OP;
	}
	
	public static void main(String[] args) {
		Deque<StringBuilder> stack = new LinkedList<StringBuilder>();
		StringBuilder sb = new StringBuilder().append("abc");
		StringBuilder sb1 = new StringBuilder().append("def");
		StringBuilder sb2 = new StringBuilder().append("ghi");
		stack.push(sb1);
		Negation.OPERATOR.print(stack);
		stack.push(sb);
		Negation.OPERATOR.print(stack);
		OPERATOR.print(stack);
		Negation.OPERATOR.print(stack);
		stack.push(sb2);
		OPERATOR.print(stack);
		System.out.println(stack.pop());
		//System.out.println(stack.pop());
	}
	
//	@Override
//	protected boolean _value(boolean ... values) {
//		return or(values[0], values[1]);
//	}
//
//	@Override
//	protected OldFormula _getFormula(OldFormula... formulas) {
//		return OldFormula.twoArityOp(this, formulas[0], formulas[1]);
//	}
//
//	@Override
//	public String toString(String ... formulas) {
//		return "( " + formulas[0] + " ) " + disjunctionOP + " ( " + formulas[1] + " )";
//	}
//
//	@Override
//	public String toString() {
//		return disjunctionOP;
//	}
//
//	public static boolean or(boolean b1, boolean b2) {
//		return b1 || b2;
//	}

}
