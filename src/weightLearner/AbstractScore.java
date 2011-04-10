package weightLearner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
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
	protected final Map<Predicate, List<Formula>> predicateFormulas;
	
	public AbstractScore(Set<Predicate> predicates) {
		int defaultSize = (int) Math.ceil(predicates.size()*1.4);
		this.predicates = predicates;
		this.formulas = new LinkedList<Formula>();
		this.predicateFormulas = new HashMap<Predicate, List<Formula>>(defaultSize);
		for (Predicate p : predicates) {
			this.predicateFormulas.put(p, new LinkedList<Formula>());
		}
	}

	protected AbstractScore(List<Formula> formulas, Set<Predicate> predicates,
			Map<Predicate, List<Formula>> predicateFormulas) {
		this.formulas = formulas;
		this.predicates = predicates;
		this.predicateFormulas = predicateFormulas;
	}
	
	/* (non-Javadoc)
	 * @see weightLearner.Score#addFormula(fol.Formula)
	 */
	@Override
	public boolean addFormula(Formula f) {
		this.formulas.add(f);
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
	
	// A way to remove an Object from a list without
	// using the equals method.
	protected static boolean remove(List<?> l, Object f) {
		ListIterator<?> it = l.listIterator(l.size());
		while (it.hasPrevious()) {
			Object g = it.previous();
			if (f == g) {
				it.remove();
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see weightLearner.Score#removeFormula(fol.Formula)
	 */
	@Override
	public boolean removeFormula(Formula f) {
		if (remove(this.formulas, f)) {
			for (Predicate p : f.getPredicates()) {
				if (p != Predicate.equals && p != Predicate.empty) {
					remove(this.predicateFormulas.get(p), f);
				}
			}
			return true;
		}
		return false;
	}
	
	@Override
	public List<Formula> getFormulas() {
		return this.formulas;
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
