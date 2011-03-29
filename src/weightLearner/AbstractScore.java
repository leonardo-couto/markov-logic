package weightLearner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fol.Formula;
import fol.Predicate;

/**
 * @author Leonardo Castilho Couto
 *
 */
public abstract class AbstractScore implements Score {
	
	private final List<Formula> formulas;
	private final Set<Predicate> predicates;
	protected final Map<Predicate, Set<Formula>> predicateFormulas;
	protected final Map<Formula, Set<Predicate>> formulaPredicates;
	
	public AbstractScore(Set<Predicate> predicates) {
		int defaultSize = (int) Math.ceil(predicates.size()*1.4);
		this.predicates = predicates;
		this.formulas = new LinkedList<Formula>();
		this.predicateFormulas = new HashMap<Predicate, Set<Formula>>(defaultSize);
		this.formulaPredicates = new HashMap<Formula, Set<Predicate>>(defaultSize);
		for (Predicate p : predicates) {
			this.predicateFormulas.put(p, new HashSet<Formula>(defaultSize));
		}
	}

	protected AbstractScore(List<Formula> formulas, Set<Predicate> predicates,
			Map<Predicate, Set<Formula>> predicateFormulas,
			Map<Formula, Set<Predicate>> formulaPredicates) {
		this.formulas = formulas;
		this.predicates = predicates;
		this.predicateFormulas = predicateFormulas;
		this.formulaPredicates = formulaPredicates;
	}
	
	/* (non-Javadoc)
	 * @see weightLearner.Score#addFormula(fol.Formula)
	 */
	@Override
	public boolean addFormula(Formula f) {
		if (this.formulas.contains(f)) { return false; }
		this.formulas.add(0, f);
		Set<Predicate> predicates = f.getPredicates();
		predicates.remove(Predicate.empty);
		predicates.remove(Predicate.equals);
		this.formulaPredicates.put(f, predicates);
		for (Predicate p : predicates) {
			this.predicateFormulas.get(p).add(f);
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see weightLearner.Score#addFormulas(java.util.List)
	 */
	@Override
	public boolean addFormulas(List<Formula> formulas) {
		boolean b = true;
		for (Formula f : formulas) {
			b = b && this.addFormula(f);
		}
		return b;
	}

	/* (non-Javadoc)
	 * @see weightLearner.Score#removeFormula(fol.Formula)
	 */
	@Override
	public boolean removeFormula(Formula f) {
		if (this.formulas.remove(f)) {
			for (Predicate p : f.getPredicates()) {
				if (p != Predicate.equals && p != Predicate.empty) {
					predicateFormulas.get(p).remove(f);
				}
			}
			formulaPredicates.remove(f);
			return true;
		}
		return false;
	}
	
	public List<Formula> getFormulas() {
		return new ArrayList<Formula>(formulas);
	}

	/* (non-Javadoc)
	 * @see math.RnToRFunction#f(double[])
	 */
	@Override
	public double f(double[] x) {
		return this.getScore(x);
	}
	
	protected Set<Predicate> getPredicates() {
		return new HashSet<Predicate>(this.predicates);
	}


}
