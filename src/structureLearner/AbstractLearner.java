package structureLearner;

import java.util.Set;

import fol.Formula;
import fol.Predicate;

public abstract class AbstractLearner {
	
	protected Set<Predicate> predicates;
	
	public AbstractLearner(Set<Predicate> p) {
		this.predicates = p;
	}
	
	public abstract Set<Formula> learn();

}
