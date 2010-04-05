package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import fol.Atom;
import fol.Predicate;

public class Sampler {
	// TODO: ADICIONAR A PARTE DE SAMPLE DE DADOS COM VERIFICACAO DE CONVERGENCIA
	
	public Sampler(Sampler sampler) {
		this.numberOfSamples = sampler.numberOfSamples;
	}

	private int numberOfSamples;
	
	public Sampler(int numberOfSamples) {
		this.numberOfSamples = numberOfSamples;
	}

	public <E> Set<E> sampleSet(Set<E> set) {
		return sampleSet(set, numberOfSamples);
	}
	
	protected <E> Set<E> sampleSet(Set<E> set, int n) {
		if(set.size() > n) {
			ArrayList<E> list = new ArrayList<E>(set);
			Collections.shuffle(list);
			return new HashSet<E>(list.subList(0, n));
		}
		return set;
	}
	
	public Set<Atom> samplePredicate(Predicate p) {
		// TODO: check if its NaN
		return sampleSet(p.getGroundings().keySet());
	}
	
	/**
	 * @return the numberOfSamples
	 */
	public int getNumberOfSamples() {
		return numberOfSamples;
	}

	/**
	 * @param numberOfSamples the numberOfSamples to set
	 */
	public void setNumberOfSamples(int numberOfSamples) {
		this.numberOfSamples = numberOfSamples;
	}

}
