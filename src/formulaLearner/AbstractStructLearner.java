package formulaLearner;

import java.util.HashSet;
import java.util.Set;

import markovLogic.structureLearner.StructureLearner;


import fol.Atom;
import fol.Predicate;

public abstract class AbstractStructLearner implements StructureLearner {
	
	protected final Set<Predicate> predicates;
	protected final Set<Atom> atoms;
	
	public AbstractStructLearner(Set<Atom> atoms) {
		this.atoms = atoms;
		this.predicates = new HashSet<Predicate>(atoms.size()*2);
		for (Atom a : atoms) {
			if (a.predicate != Predicate.EMPTY) {
			  this.predicates.add(a.predicate);
			}
		}
	}
	
}
