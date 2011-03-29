package structureLearner;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import markovLogic.MarkovLogicNetwork;

import fol.Atom;
import fol.Predicate;

public abstract class AbstractLearner {
	
	protected final Set<Predicate> predicates;
	protected final Set<Atom> atoms;
	
	public AbstractLearner(Set<Atom> atoms) {
		this.atoms = atoms;
		this.predicates = new HashSet<Predicate>(atoms.size()*2);
		for (
				Iterator<Atom> i = atoms.iterator(); 
				i.hasNext(); 
				this.predicates.add(i.next().predicate)
		);
	}
	
	public abstract MarkovLogicNetwork learn();

}
