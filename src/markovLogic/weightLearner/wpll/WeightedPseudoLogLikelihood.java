package markovLogic.weightLearner.wpll;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import markovLogic.weightLearner.Score;
import fol.Atom;
import fol.Formula;
import fol.Predicate;

public class WeightedPseudoLogLikelihood implements Score {
	
	private final List<Formula> formulas;
	private final List<Predicate> predicates;
	private final Map<Atom, List<Count>> countsMap;
	private final CountCache cache;
	private final int sampleLimit;
	
	private final int[] samples;
	private double[] grad = new double[0];
	
	public WeightedPseudoLogLikelihood(Collection<Predicate> predicates, CountCache cache, int sampleLimit) {
		this.countsMap = new HashMap<Atom, List<Count>>();
		this.predicates = new ArrayList<Predicate>(predicates);
		this.formulas = new LinkedList<Formula>();
		this.cache = cache;
		this.sampleLimit = sampleLimit;
		this.samples = new int[this.predicates.size()];
		this.computeSamples();
	}

	@Override
	public boolean addFormula(Formula formula) {
		int index = this.formulas.size();
		List<Count> counts = this.cache.getCounts(formula, this.sampleLimit);
		for (Count count : counts) {
			this.store(count, index);
			
		}
		return this.formulas.add(formula);
	}

	@Override
	public boolean addFormulas(List<? extends Formula> formulas) {
		boolean b = true;
		for (Formula formula : formulas) {
			b = b && this.addFormula(formula);
		}
		return b;
	}
	
	private void checkInput(double[] weights) {
		if (weights.length != this.lengthInput()) {
			String error = String.format("Wrong number of arguments. Expected %s, got %s", 
					this.lengthInput(), 
					weights.length
				);
			throw new RuntimeException(error);
		}
	}
	
	private void computeSamples() {
		for (int i = 0; i < this.samples.length; i++) {
			Predicate p = this.predicates.get(i);
			this.samples[i] = Math.min(this.sampleLimit, p.totalGroundings());			
		}
	}

	@Override
	public WeightedPseudoLogLikelihood copy() {
		WeightedPseudoLogLikelihood copy = new WeightedPseudoLogLikelihood(this.predicates, this.cache, this.sampleLimit);
		copy.formulas.addAll(this.formulas);
		for (Atom key : this.countsMap.keySet()) {
			List<Count> value = this.countsMap.get(key);
			copy.countsMap.put(key, new ArrayList<Count>(value));			
		}
		return copy;
	}
	
	@Override
	public double f(double[] x) {
		return this.getScore(x);
	}

	@Override
	public double[] g(double[] x) {
		return this.grad;
	}

	@Override
	public List<Formula> getFormulas() {
		return new ArrayList<Formula>(this.formulas);
	}

	// Soma_{r=predicate} (
	//     Soma_{gr=ground_r} ( 
	//         x -max(a,b) -log(1+e^min[a-b,b-a]) -> o log varia entre 2 e 1+e
	//     )
	// )
	//           --> fator mais importante eh a soma de de a e b, parar de usar o somatorio
	//               quando tiver convertido para a soma de x - max(a,b).
	//
	// onde x = Soma_{i=function}( w_i * n_i[gr = valor(gr)] = w_i * n_i[x] )
	//      a = Soma_{i=function}( w_i * n_i[gr = true     ] = w_i * n_i[a] )
	//      b = Soma_{i=function}( w_i * n_i[gr = false    ] = w_i * n_i[b] )
	//      n_i = numero de counts da formula i com o valor de gr especificado
	//      gr = atom do predicado r, com constantes (grounded)
	@Override
	public double getScore(double[] weights) {
		this.checkInput(weights);
		
		double[] predicatePll = new double[this.predicates.size()];
		double[][] predicateGrad = new double[this.predicates.size()][weights.length];
		Predicate previous = null;
		int index = -1;
		
		// all the work is done here
		LogConditionalProbability prob = new LogConditionalProbability(weights);
		Collection<List<Count>> counts = this.countsMap.values();
		for (List<Count> count : counts) {
			// for each grounding, update pll and its gradient
			DataPll data = prob.evaluate(count);
			Predicate predicate = data.grounding.predicate;
			
			if (predicate != previous) {
				previous = predicate;
				index = this.predicates.indexOf(predicate);
			}
			
			predicatePll[index] += data.pll;
			for (int i = 0; i < weights.length; i++) {
				predicateGrad[index][i] += data.grad[i];
			}
		}
		
		// applies the weight for each predicate 
		double[] grad = new double[weights.length];
		double pll = 0;
		for (int i = 0; i < this.samples.length; i++) {
			double samples = this.samples[i];
			pll += predicatePll[i] / samples;
			for (int j = 0; j < grad.length; j++) {
				grad[j] += predicateGrad[i][j] / samples;
			}
		}

		this.grad = grad;
		return pll;
	}

	@Override
	public int lengthInput() {
		return this.formulas.size();
	}
	
	@Override
	public int lengthOutput() {
		return this.formulas.size();
	}
	
	private void remove(int index) {
		for (Atom key : this.countsMap.keySet()) {
			List<Count> value = this.countsMap.get(key);
			if (index < value.size()) {
				value.remove(index);
			}
		}
	}
	
	@Override
	public boolean removeFormula(Formula f) {
		int i = this.formulas.size();
		ListIterator<Formula> iterator = this.formulas.listIterator(i);
		while (iterator.hasPrevious()) {
			i--;
			if (iterator.previous().equals(f)) {
				iterator.remove();
				this.remove(i);
				return true;
			}
		}
		return false;
	}
	
	private void store(Count count, int index) {
		Atom key = count.getAtom();
		List<Count> counts = this.countsMap.get(key);
		if (counts == null) {
			counts = new ArrayList<Count>();
			this.countsMap.put(key, counts);
		}
		for (int i = counts.size(); i < index+1; i++) {
			counts.add(null);
		}
		
		counts.set(index, count);
	}
	
}
