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

	/* (non-Javadoc)
	 * @see weightLearner.Score#addFormula(fol.Formula)
	 */
	@Override
	public void addFormula(Formula f) {
		this.formulas.add(0, f);
		Set<Predicate> predicates = f.getPredicates();
		this.formulaPredicates.put(f, predicates);
		for (Predicate p : predicates) {
			predicateFormulas.get(p).add(f);
		}
	}

	/* (non-Javadoc)
	 * @see weightLearner.Score#addFormulas(java.util.List)
	 */
	@Override
	public void addFormulas(List<Formula> formulas) {
		this.formulas.addAll(0, formulas);
		for (Formula f : formulas) {
			Set<Predicate> predicates = f.getPredicates();
			this.formulaPredicates.put(f, predicates);
			for (Predicate p : predicates) {
				predicateFormulas.get(p).add(f);
			}
		}
	}

	/* (non-Javadoc)
	 * @see weightLearner.Score#removeFormula(fol.Formula)
	 */
	@Override
	public boolean removeFormula(Formula f) {
		if (this.formulas.remove(f)) {
			for (Predicate p : f.getPredicates()) {
				predicateFormulas.get(p).remove(f);
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
