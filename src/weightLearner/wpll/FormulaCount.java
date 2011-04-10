package weightLearner.wpll;

import java.util.List;
import java.util.ListIterator;

import fol.Formula;

public class FormulaCount {
	
	public final Formula formula;
	public final Data data;
	
	public FormulaCount(Formula formula, Data data) {
		this.formula = formula;
		this.data = data;
	}
	
	@Override
	public boolean equals(Object o) {
		return this.formula == o;
	}
	
	public static boolean remove(List<FormulaCount> l, Formula f) {
		ListIterator<FormulaCount> it = l.listIterator();
		while (it.hasNext()) {
			if (it.next().formula == f) {
				it.remove();
				return true;
			}
		}
		return false;
	}

}
