package markovLogic.structureLearner.pdl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import markovLogic.weightLearner.WeightLearner;
import markovLogic.weightLearner.wpll.CountsGenerator;
import math.OptimizationException;
import fol.Clause;
import fol.FormulaFactory;
import fol.WeightedFormula;
import fol.WeightedFormula.AbsoluteWeightComparator;

public class ClauseFilter {
	
	private final WeightLearner wlearner;
	private final FormulaFactory factory;
	private final CountsGenerator counter;
	
	public ClauseFilter(WeightLearner learner, FormulaFactory factory, CountsGenerator counter) {
		this.wlearner = learner;
		this.factory = factory;
		this.counter = counter;
	}
	
	public List<Clause> filter(List<Clause> candidates) {
		
		List<Clause> all = new ArrayList<Clause>();
		List<List<Clause>> flipList = new ArrayList<List<Clause>>(candidates.size());
		
		for (Clause candidate : candidates) {
			List<Clause> flips = this.factory.flipSigns(candidate);
			flipList.add(flips);
			all.addAll(flips);			
		}
		
		// generate counts
		this.counter.count(all);
		
		double initialScore = this.wlearner.score();
		double[] weights = this.wlearner.weights();
//		List<ConjunctiveNormalForm> selection = new ArrayList<ConjunctiveNormalForm>(candidates.size());
		List<WeightedFormula<Clause>> selection = new ArrayList<WeightedFormula<Clause>>(candidates.size());
		
		for (List<Clause> flips : flipList) {
			double max = 0;
			int index  = -1;
			for (int i = 0; i < flips.size(); i++) {
				Clause clause = flips.get(i);
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
				WeightedFormula<Clause> cnf = new WeightedFormula<Clause>(flips.get(index), max);
				selection.add(cnf);
//				selection.add(flips.get(index));
			}
		}
		
		Comparator<WeightedFormula<?>> comparator = new AbsoluteWeightComparator(true);
		Collections.sort(selection, comparator);
		return WeightedFormula.toFormulasAndWeights(selection).formulas;
		
//		return selection;
	}

}
