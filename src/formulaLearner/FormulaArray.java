package formulaLearner;

import java.util.ArrayList;
import java.util.Collection;

import fol.Formula;

public class FormulaArray extends ArrayList<Formula> {
	
	private static final long serialVersionUID = 4281216655319484386L;
	private int idx = 0;
	private final int fpt;
	
	public synchronized Formula[] getElements() {
		int size = this.size();
		if (idx <= size) {
			System.out.println("(" + idx + " ate " + Math.min(idx + fpt, size) + ")/" + size); // TODO: REMOVER!!
			Formula[] out = this.subList(idx, Math.min(idx + fpt, size)).toArray(new Formula[0]);
			idx = idx + fpt;
			return out;
		}
		return null;
	}
	
	// TODO: temp, remover
	double score = Double.NEGATIVE_INFINITY;
	public synchronized double getScore() {
		return score;
	}
	public synchronized void setScore(double d) {
		if (Double.compare(d, score) > 0) {
			score = d;
		}
	} // TODO: fim do remove

	public FormulaArray(int limit) {
		super();
		this.fpt = limit;
	}

	public FormulaArray(int limit, Collection<? extends Formula> c) {
		super(c);
		this.fpt = limit;
	}
	
}
