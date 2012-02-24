package structureLearner.pdl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import markovLogic.WeightedFormula;
import math.OptimizationException;
import structureLearner.CountsGenerator;
import weightLearner.WeightLearner;
import fol.ConjunctiveNormalForm;
import fol.FormulaFactory;

public class ClauseFilter {
	
	private final WeightLearner wlearner;
	private final FormulaFactory factory;
	private final CountsGenerator counter;
	
	public ClauseFilter(WeightLearner learner, FormulaFactory factory, CountsGenerator counter) {
		this.wlearner = learner;
		this.factory = factory;
		this.counter = counter;
	}
	
	public List<ConjunctiveNormalForm> filter(List<ConjunctiveNormalForm> candidates) {
		
		List<ConjunctiveNormalForm> all = new ArrayList<ConjunctiveNormalForm>();
		List<List<ConjunctiveNormalForm>> flipList = new ArrayList<List<ConjunctiveNormalForm>>(candidates.size());
		
		for (ConjunctiveNormalForm candidate : candidates) {
			List<ConjunctiveNormalForm> flips = this.factory.flipSigns(candidate);
			flipList.add(flips);
			all.addAll(flips);			
		}
		
		// generate counts
		this.counter.count(all);
		
		double initialScore = this.wlearner.score();
		double[] weights = this.wlearner.weights();
//		List<ConjunctiveNormalForm> selection = new ArrayList<ConjunctiveNormalForm>(candidates.size());
		List<WeightedFormula<ConjunctiveNormalForm>> selection = new ArrayList<WeightedFormula<ConjunctiveNormalForm>>(candidates.size());
		
		for (List<ConjunctiveNormalForm> flips : flipList) {
			double max = 0;
			int index  = -1;
			for (int i = 0; i < flips.size(); i++) {
				ConjunctiveNormalForm clause = flips.get(i);
				try {
					this.wlearner.addFormula(clause);
					double w = this.wlearner.learn(weights)[weights.length];
					double score = (this.wlearner.score()-initialScore) * Math.abs(w);
					if (score > max) {
						max = score;
						index = i;
					}					
				} catch (OptimizationException e) {
					e.printStackTrace();
				} finally {
					this.wlearner.removeFormula(clause);
				}
			}
			if (index > -1) {
				WeightedFormula<ConjunctiveNormalForm> cnf = new WeightedFormula<ConjunctiveNormalForm>(flips.get(index), max);
				selection.add(cnf);
//				selection.add(flips.get(index));
			}
		}
		
		Comparator<WeightedFormula<ConjunctiveNormalForm>> comparator = new WeightedFormula.AbsoluteWeightComparator<ConjunctiveNormalForm>(true);
		Collections.sort(selection, comparator);
		return WeightedFormula.toFormulasAndWeights(selection).formulas;
		
//		return selection;
	}

}
