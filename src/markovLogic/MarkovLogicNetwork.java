package markovLogic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import fol.Atom;
import fol.Formula;
import fol.Predicate;
import fol.WeightedFormula;

public class MarkovLogicNetwork implements List<WeightedFormula> {
	
	private final ArrayList<WeightedFormula> wFormulas;
	public static final long serialVersionUID = -7177305257098768154L;

	public MarkovLogicNetwork() {
		this.wFormulas = new ArrayList<WeightedFormula>();
	}

	public <T extends List<? extends WeightedFormula>> MarkovLogicNetwork(T m) {
		this.wFormulas = new ArrayList<WeightedFormula>(m);
	}
	
	/**
	 * The markovBlanket of an Atom 'a' in a MarkovLogicNetwork is a set of neighbors
	 * Atoms of 'a', such that given those atoms, the probability of 'a' does not
	 * depend on any other Atom
	 * @param a
	 * @return the markovBlanket of a
	 */
	public Set<Atom> markovBlanket(Atom a) {
		GroundedMarkovNetwork mln = this.ground(a, Collections.<Atom>emptyList());
		List<WeightedFormula> groundedFormulas = mln.getGroundedFormulas();
		Set<Atom> markovBlanket = new HashSet<Atom>();
		formula: for (WeightedFormula wf : groundedFormulas) {
			Formula f = wf.getFormula();
			for (Atom b : f.getAtoms()) {
				if (a == b) {
					markovBlanket.addAll(f.getAtoms());
					continue formula;
				}
			}
		}
		return markovBlanket;
	}
	
	public GroundedMarkovNetwork ground(Atom query, Collection<Atom> given) {
		return GroundedMarkovNetwork.ground(this, query, given);
	}

	public GroundedMarkovNetwork ground(Atom query) {
		return GroundedMarkovNetwork.ground(this, query, Collections.<Atom>emptyList());
	}
	
	/**
	 * Get all predicates in this MLN
	 * @return a Set with all the predicates in this class
	 */
	public Set<Predicate> getPredicates() {
		  Set<Predicate> predicates = new HashSet<Predicate>();
		  for (WeightedFormula f : this) {
			  predicates.addAll(f.getFormula().getPredicates());
		  }
		  return predicates;
	}
	
	@Override
	public String toString() {
		final String comma = " : ";
		final String eol = "\n";
		StringBuilder sb = new StringBuilder(this.size()*150);
		for (WeightedFormula wf : this) {
			sb.append(wf.getWeight());
			sb.append(comma);
			sb.append(wf.getFormula());
			sb.append(eol);
		}
		return sb.toString();
	}
	
	@Override
	public int size() {
		return this.wFormulas.size();
	}

	@Override
	public boolean isEmpty() {
		return this.wFormulas.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return this.wFormulas.contains(o);
	}

	@Override
	public Iterator<WeightedFormula> iterator() {
		return this.wFormulas.iterator();
	}

	@Override
	public Object[] toArray() {
		return this.wFormulas.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return this.wFormulas.toArray(a);
	}

	@Override
	public boolean add(WeightedFormula e) {
		return this.wFormulas.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return this.wFormulas.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return this.wFormulas.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends WeightedFormula> c) {
		return this.wFormulas.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends WeightedFormula> c) {
		return this.wFormulas.addAll(index, c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return this.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return this.retainAll(c);
	}

	@Override
	public void clear() {
		this.wFormulas.clear();
	}

	@Override
	public WeightedFormula get(int index) {
		return this.wFormulas.get(index);
	}

	@Override
	public WeightedFormula set(int index, WeightedFormula element) {
		return this.wFormulas.set(index, element);
	}

	@Override
	public void add(int index, WeightedFormula element) {
		this.wFormulas.add(index, element);
	}

	@Override
	public WeightedFormula remove(int index) {
		return this.remove(index);
	}

	@Override
	public int indexOf(Object o) {
		return this.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return this.lastIndexOf(o);
	}

	@Override
	public ListIterator<WeightedFormula> listIterator() {
		return this.listIterator();
	}

	@Override
	public ListIterator<WeightedFormula> listIterator(int index) {
		return this.listIterator();
	}

	@Override
	public List<WeightedFormula> subList(int fromIndex, int toIndex) {
		return this.subList(fromIndex, toIndex);
	}
	
}
