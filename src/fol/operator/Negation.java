package fol.operator;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import fol.Formula;
import fol.FormulaComponent;
import fol.GeneralFormula;
import fol.database.Database;


public final class Negation implements Operator {
	
	private static final Character NEGATION_OP = '!';
	public static final Negation OPERATOR = new Negation();
	
	private Negation() {
		super();
	}
	
	public GeneralFormula apply(Formula f0) {
		List<FormulaComponent> l0 = f0.getComponents();
		List<FormulaComponent> formula = new ArrayList<FormulaComponent>(l0.size()+1);
		if (l0.get(l0.size()-1) == this) {
			return new GeneralFormula(l0.subList(0, l0.size()-1));
		}
		formula.addAll(l0);
		formula.add(this);
		return new GeneralFormula(formula);
	}

	@Override
	public void evaluate(Deque<Boolean> stack, Database db) {
		stack.push(Boolean.valueOf(!stack.pop().booleanValue()));		
	}

	@Override
	public String getSymbol() {
		return NEGATION_OP.toString();
	}
	
	@Override
	public void print(Deque<StringBuilder> stack) {
		stack.peek().insert(0, NEGATION_OP);	
	}
	
}
