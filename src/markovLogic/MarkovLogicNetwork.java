package markovLogic;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fol.Atom;
import fol.Predicate;
import fol.WeightedFormula;

public class MarkovLogicNetwork {
	
	private final ArrayList<WeightedFormula<?>> wFormulas;

	public MarkovLogicNetwork() {
		this.wFormulas = new ArrayList<WeightedFormula<?>>();
	}

	public <T extends WeightedFormula<?>> MarkovLogicNetwork(Collection<T> m) {
		this.wFormulas = new ArrayList<WeightedFormula<?>>(m);
	}
	
	/**
	 * The markovBlanket of an Atom 'a' in a MarkovLogicNetwork is a set of neighbors
	 * Atoms of 'a', such that given those atoms, the probability of 'a' does not
	 * depend on any other Atom
	 * @param a
	 * @return the markovBlanket of a
	 */
	public Set<Atom> markovBlanket(Atom a) {
//		GroundedMarkovNetwork mln = this.ground(a, Collections.<Atom>emptyList());
//		List<WeightedFormula> groundedFormulas = mln.getGroundedFormulas();
//		Set<Atom> markovBlanket = new HashSet<Atom>();
//		formula: for (WeightedFormula wf : groundedFormulas) {
//			Formula f = wf.getFormula();
//			for (Atom b : f.getAtoms()) {
//				if (a == b) {
//					markovBlanket.addAll(f.getAtoms());
//					continue formula;
//				}
//			}
//		}
//		return markovBlanket;
		return null;
	}
	
	public List<WeightedFormula<?>> getFormulas() {
		return new ArrayList<WeightedFormula<?>>(this.wFormulas);
	}
	
	/**
	 * Get all predicates in this MLN
	 * @return a Set with all the predicates in this class
	 */
	public Set<Predicate> getPredicates() {
		  Set<Predicate> predicates = new HashSet<Predicate>();
		  for (WeightedFormula<?> f : this.wFormulas) {
			  predicates.addAll(f.getFormula().getPredicates());
		  }
		  return predicates;
	}
	
	@Override
	public String toString() {
		final String space = " ";
		final String eol = File.separator;
		StringBuilder sb = new StringBuilder();
		for (WeightedFormula<?> wf : this.wFormulas) {
			sb.append(wf.getWeight());
			sb.append(space);
			sb.append(wf.getFormula());
			sb.append(eol);
		}
		return sb.toString();
	}
	
}
