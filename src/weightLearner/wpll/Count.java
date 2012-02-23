package weightLearner.wpll;

import fol.Atom;
import fol.Formula;

/**
 * <p>Store the counts for <code>formula</code>. Counts are later used to compute 
 * the MLN likelihood or pseudo-likelihood.</p>
 * 
 * <p><code>atom</code> is a grounded atom of predicate '<code>atom.predicate</code>' 
 * and <code>value</code> correspond to its value in the database used to generate 
 * this count.</p>
 *  
 * <p>The true counts are the number of formulas that has a grounding equals
 * to <code>atom</code> and are true when we set <code>atom</code> to true.
 * False counts are the same thing, but setting <code>atom</atom> to false.</p>
 * 
 */
public class Count {
	
	private final Atom atom;
	private final Formula formula;
	private final double count;
	private final double falseCount;
	private final double trueCount;
	
	public Count(Atom atom, Formula formula, double falseCount, double trueCount, double count) {
		this.atom = atom;
		this.formula = formula;
		this.falseCount = falseCount;
		this.trueCount = trueCount;
		this.count = count;
	}
	
	public Atom getAtom() {
		return atom;
	}

	public Formula getFormula() {
		return formula;
	}

	public double getFalseCount() {
		return falseCount;
	}

	public double getTrueCount() {
		return trueCount;
	}

	public double getCount() {
		return count;
	}
	
}
