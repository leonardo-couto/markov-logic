package formulaLearner;

import java.util.HashSet;
import java.util.Set;

import structureLearner.StructureLearner;

import fol.Atom;
import fol.Predicate;

public abstract class AbstractLearner implements StructureLearner {
	
	protected final Set<Predicate> predicates;
	protected final Set<Atom> atoms;
	
	public AbstractLearner(Set<Atom> atoms) {
		this.atoms = atoms;
		this.predicates = new HashSet<Predicate>(atoms.size()*2);
		for (Atom a : atoms) {
			if (a.predicate != Predicate.empty) {
			  this.predicates.add(a.predicate);
			}
		}
	}
	
}
