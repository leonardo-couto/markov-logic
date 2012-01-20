package fol.operator;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import fol.Formula;
import fol.FormulaComponent;
import fol.GeneralFormula;
import fol.database.Database;

public final class Conjunction implements Operator {
	
	private static final String CONJUNCTION_OP = "^";
	private static final String CONJUNCTION_CONNECTOR = " ^ ";
	public static final Conjunction OPERATOR = new Conjunction();

	private Conjunction() {
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
				stack.pop().booleanValue() & stack.pop().booleanValue()));
	}

	@Override
	public void print(Deque<StringBuilder> stack) {
		StringBuilder s1 = stack.pop();
		StringBuilder s2 = stack.pop();
		StringBuilder out = new StringBuilder(s1.length()+s2.length()+6);
		stack.push(out.append(Operator.LEFTP)
				.append(s1)
				.append(CONJUNCTION_CONNECTOR)
				.append(s2)
				.append(Operator.RIGHTP)
				);
	}

	@Override
	public String getSymbol() {
		return CONJUNCTION_OP;
	}
	
}
