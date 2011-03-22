package markovLogic;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fol.Atom;
import fol.Formula;

public class MarkovLogicNetwork implements Map<Formula, Double> {
	
	private final HashMap<Formula, Double> wFormulas; // weightedFormulas
	public static final long serialVersionUID = -7177305257098768154L;

	public MarkovLogicNetwork() {
		this.wFormulas = new HashMap<Formula, Double>();
	}

	public <T extends Map<? extends Formula, Double>> MarkovLogicNetwork(T m) {
		this.wFormulas = new HashMap<Formula, Double>(m);
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
		Map<Formula, Double> groundedFormulas = mln.getGroundedFormulas();
		Set<Atom> markovBlanket = new HashSet<Atom>();
		formula: for (Formula f : groundedFormulas.keySet()) {
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
	
	@Override
	public int size() {
		return wFormulas.size();
	}

	@Override
	public boolean isEmpty() {
		return wFormulas.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return wFormulas.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return wFormulas.containsValue(value);
	}

	@Override
	public Double get(Object key) {
		return wFormulas.get(key);
	}

	@Override
	public Double put(Formula key, Double value) {
		return wFormulas.put(key, value);
	}

	@Override
	public Double remove(Object key) {
		return wFormulas.remove(key);
	}

	@Override
	public void putAll(Map<? extends Formula, ? extends Double> m) {
		wFormulas.putAll(m);
	}

	@Override
	public void clear() {
		wFormulas.clear();
	}

	@Override
	public Set<Formula> keySet() {
		return wFormulas.keySet();
	}

	@Override
	public Collection<Double> values() {
		return wFormulas.values();
	}

	@Override
	public Set<java.util.Map.Entry<Formula, Double>> entrySet() {
		return wFormulas.entrySet();
	}
	
}
