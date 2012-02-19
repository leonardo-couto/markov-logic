package structureLearner.pdl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import markovLogic.MarkovLogicNetwork;
import markovLogic.WeightedFormula;
import math.AutomatedLBFGS;
import math.OptimizationException;
import structureLearner.StructureLearner;
import weightLearner.L1RegularizedScore;
import weightLearner.Score;
import weightLearner.WeightLearner;
import weightLearner.wpll.WeightedPseudoLogLikelihood;
import fol.ConjunctiveNormalForm;
import fol.Formula;
import fol.FormulaFactory;
import fol.Predicate;
import fol.database.Database;

/**
 * Based on Kok and Domingos paper Learning the Structure of Markov Logic Networks,
 * published in the International Conference on Machine Learning 2005.
 * 
 * The algorithm perform a beam search to find the best clause and add it to the network.
 * Clauses are scored through Weighted Pseudo-log-likelihood.
 */
public class PDL implements StructureLearner {
	
	private static final int MAX_VARS = 6;
//	private static final int BEAM_SIZE = 20;
	private static final double EPSLON = 0.0001; // min absolute weight
	private static final double MIN_IMPROVEMENT = 0.001; // percent value of min score improvement
	private static final int MAX_LITERALS = 5;
	
	private final Set<Predicate> predicates;
	private final Database db;
	private final FormulaFactory factory;
	private final List<ConjunctiveNormalForm> atoms;
	private final WeightLearner weightLearner;
	private final Comparator<WeightedFormula> comparator;
	
	private final WeightLearner preciseLearner;
	
	private final Score score;
	private final L1RegularizedScore l1Score;
	private final AutomatedLBFGS l1Optimizer;
	
	public PDL(Set<Predicate> predicates, Database db) {
		this.predicates = predicates;
		this.db = db;
		this.factory = new FormulaFactory(predicates, MAX_VARS);
		this.atoms = this.factory.getUnitClauses();
		
		// Instantiate the weightLearner
		// TODO: fazer pegar pelo menos 10 ground atomos verdadeiros
		this.score = new WeightedPseudoLogLikelihood(this.predicates, this.db, 500); // 50
		L1RegularizedScore l1Score = new L1RegularizedScore(this.score).setConstantWeight(-0.1);
//		l1Score.setStart(6);
		this.l1Score = l1Score;
		this.l1Optimizer = new AutomatedLBFGS(0.01);
			
		this.weightLearner = new WeightLearner(this.l1Score, this.l1Optimizer);
		this.preciseLearner = new WeightLearner(
				new WeightedPseudoLogLikelihood(this.predicates, this.db, 5000), 
				new AutomatedLBFGS(0.001));
		this.comparator = new WeightedFormula.AbsoluteWeightComparator(true);
	}

	@Override
	public MarkovLogicNetwork learn() {
	
		// add unit clauses, learn weights and gets the score
		this.weightLearner.addFormulas(this.atoms);
		this.preciseLearner.addFormulas(this.atoms);
		try {
			this.weightLearner.learn(new double[this.atoms.size()]);
			this.preciseLearner.learn(this.weightLearner.weights());
		} catch (Exception e) {
			System.err.println("Could not learn atoms weight.");
			e.printStackTrace();
			System.exit(1);			
		}
		double score = this.preciseLearner.score();
		
		List<Formula> candidates = this.factory.generateClauses(this.atoms);	
		
		for (int i = 1; i < MAX_LITERALS; i++) {
			List<WeightedFormula> wFormulas = this.batchLearn(candidates);
			
			boolean changed = false;
			List<Formula> addedClauses = new ArrayList<Formula>();
			for (WeightedFormula f : wFormulas) {
				if (Double.compare(Math.abs(f.getWeight()), EPSLON) > 0) {
					if (this.addClause(f, score)) {
						score = this.preciseLearner.score();
						addedClauses.add(f.getFormula());
						System.out.println(String.format("%s;%s; %s", score, f.getWeight(), f.getFormula()));
						changed = true;
					}					
				} else {
					break;
				}
			}
			
			if (!changed) {
				break;
			}
			
			HashSet<ConjunctiveNormalForm> flippedClauses = new HashSet<ConjunctiveNormalForm>((2 << i)*addedClauses.size());
			for (Formula f : addedClauses) {
				flippedClauses.addAll(this.factory.flipSigns((ConjunctiveNormalForm) f));
			}
//			List<Formula> clauses = WeightedFormula.toFormulasAndWeights(wFormulas).formulas;
			candidates = this.factory.generateClauses(flippedClauses);
		}
		
		// TODO: TIRAR AS QUE TEM PESO ZERO (no L1 score) ANTES DE ADICIONAR NA MLN
		// menos os atomos
		
		double[] weights = this.preciseLearner.weights();
		List<Formula> formulas = this.preciseLearner.getFormulas();
		MarkovLogicNetwork mln = new MarkovLogicNetwork();
		mln.addAll(WeightedFormula.toWeightedFormulas(formulas, weights));
		
		return mln;
	}
	
	private List<WeightedFormula> batchLearn(List<Formula> candidates) {
		try {
			double min_weight = 10e-6;
			double[] initialWeights = this.weightLearner.weights();
			int initialSize = initialWeights.length;
			this.l1Score.setStart(initialSize);
			double[] weights = initialWeights;
			int counter = 0;
			
			this.weightLearner.addFormulas(candidates);
			{
				boolean improved = true;
				double score, min, lastScore;
				lastScore = this.weightLearner.score();

				while (improved) {
					counter++;
					weights = this.weightLearner.learn(weights);
					score = this.weightLearner.score();
					min = lastScore*(1 -1*MIN_IMPROVEMENT/2);
					lastScore = score;
					improved = score > min;
					System.out.println(counter + ": " + score);
				}
			}
			
			
//			for (Formula f : candidates) {
//				this.weightLearner.addFormula(f);
//				weights = this.weightLearner.learn(weights);
//				//this.removeClauses(this.weightLearner, min_weight);
//			}
			counter = 0;
			int quantity = weights.length;
			System.out.println("TOTAL FORMULAS = " + quantity);
			while (this.removeClauses(this.weightLearner, min_weight, initialSize) > 0) {
				weights = this.weightLearner.weights();
				counter++;
				System.out.println(counter + ": " + (quantity - weights.length));
			}
			
			System.out.println(String.format("SOBRARAM %s FORMULAS!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!", weights.length));
		
			
			int finalLength = weights.length;
			weights = Arrays.copyOfRange(weights, initialSize, finalLength);
			List<Formula> formulas = this.weightLearner.getFormulas().subList(initialSize, finalLength);
			List<WeightedFormula> wFormulas = WeightedFormula.toWeightedFormulas(formulas, weights);
			Collections.sort(wFormulas, this.comparator);
			this.printBatchLearner(wFormulas);
			//wFormulas = wFormulas.subList(0, Math.min(BEAM_SIZE, wFormulas.size()));
			
			// clean weightLearner
			ListIterator<Formula> iterator = candidates.listIterator(candidates.size());
			while(iterator.hasPrevious()) {
				this.weightLearner.removeFormula(iterator.previous());
			}
			this.weightLearner.learn(initialWeights);

			return wFormulas;
		} catch (OptimizationException e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}
	
	private void printBatchLearner(List<WeightedFormula> wFormulas) {
		System.out.println("************* IMPRIMINDO FORMULAS BATHLEARNER **************");
		for (WeightedFormula wf : wFormulas) {
			System.out.println(wf);
		}
		System.out.println("************* END IMPRIMINDO FORMULAS BATHLEARNER **************");			
	}
	
	private int removeClauses(WeightLearner learner, double weightThreeshold, int from) throws OptimizationException {
		List<Formula> formulas = new ArrayList<Formula>(learner.getFormulas());
		double[] weights = learner.weights();
		double[] newWeights = new double[weights.length];
		int remove = 0;
		int j = 0;
		for (int i = 0; i < from; i++) {
			newWeights[i] = weights[i];
			j++;
		}
		for (int i = from; i < weights.length; i++) {
			if (Math.abs(weights[i]) < weightThreeshold) {
				remove++;
				learner.removeFormula(formulas.get(i));
			} else {
				newWeights[j] = weights[i];
				j++;
			}
		}
		
		if (remove > 0) {
			weights = Arrays.copyOf(newWeights, weights.length-remove);
			weights = learner.learn(weights);
		}
		
		return remove;
	}
	
	private boolean addClause(WeightedFormula f, double lastScore) {
		try {
			double[] weights = this.preciseLearner.weights();
			int length = weights.length;
			weights = Arrays.copyOf(weights, length+1);
			weights[length] = f.getWeight();
				
			this.preciseLearner.addFormula(f.getFormula());
			this.preciseLearner.learn(weights);
			double score = this.preciseLearner.score();
			double min = -1*MIN_IMPROVEMENT*lastScore;
			if (Double.compare(score-lastScore, min) > -1) {
				double[] initialWeights = this.weightLearner.weights();
				initialWeights = Arrays.copyOf(initialWeights, length+1);
				initialWeights[length] = f.getWeight();
				this.weightLearner.addFormula(f.getFormula());
				this.weightLearner.learn(initialWeights);
				return true;				
			} else {
				this.preciseLearner.removeFormula(f.getFormula());
				weights = Arrays.copyOf(weights, length);
				this.preciseLearner.learn(weights);
				return false;
			}
			
		} catch (OptimizationException e) {
			e.printStackTrace();
		}
		return false;
	}

}
