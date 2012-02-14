package structureLearner.pdl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
	private static final int BEAM_SIZE = 20;
	private static final double EPSLON = 0.001; // min absolute weight
	private static final double MIN_IMPROVEMENT = 0.01; // percent value of min score improvement
	
	private final Set<Predicate> predicates;
	private final Database db;
	private final FormulaFactory factory;
	private final List<ConjunctiveNormalForm> atoms;
	private final WeightLearner weighLearner;
	private final Comparator<WeightedFormula> comparator;
	
	private final WeightLearner preciseLearner;
	
	private final Score score;
	private final Score l1Score;
	
	public PDL(Set<Predicate> predicates, Database db) {
		this.predicates = predicates;
		this.db = db;
		this.factory = new FormulaFactory(predicates, MAX_VARS);
		this.atoms = this.factory.getUnitClauses();
		
		// Instantiate the weightLearner
		// TODO: fazer pegar pelo menos 10 ground atomos verdadeiros
		this.score = new WeightedPseudoLogLikelihood(this.predicates, this.db, 50);
		L1RegularizedScore l1Score = new L1RegularizedScore(this.score).setConstantWeight(-0.1);
//		l1Score.setStart(6);
		this.l1Score = l1Score;
			
		this.weighLearner = new WeightLearner(this.l1Score, new AutomatedLBFGS(0.005));
		this.preciseLearner = new WeightLearner(
				new WeightedPseudoLogLikelihood(this.predicates, this.db, 5000), 
				new AutomatedLBFGS(0.001));
		this.comparator = new WeightedFormula.AbsoluteWeightComparator(true);
	}

	@Override
	public MarkovLogicNetwork learn() {
	
		// add unit clauses, learn weights and gets the score
		this.weighLearner.addFormulas(this.atoms);
		this.preciseLearner.addFormulas(this.atoms);
		try {
			this.weighLearner.learn(new double[this.atoms.size()]);
			this.preciseLearner.learn(this.weighLearner.weights());
		} catch (Exception e) {
			System.err.println("Could not learn atoms weight.");
			e.printStackTrace();
			System.exit(1);			
		}
		double score = this.preciseLearner.score();
		
		List<Formula> candidates = this.factory.generateClauses(this.atoms);		
		while (true) {
			List<WeightedFormula> wFormulas = this.batchLearn(candidates);
			
			boolean changed = false;
			for (WeightedFormula f : wFormulas) {
				if (Double.compare(f.getWeight(), EPSLON) > 0) {
					if (this.addClause(f, score)) {
						score = this.preciseLearner.score();
						System.out.println(String.format("%s;%s; %s", score, f.getWeight(), f.getFormula()));
						changed = true;
					} else {
						break;
					}					
				} else {
					break;
				}
			}
			
			if (!changed) {
				break;
			}
			
			List<Formula> clauses = WeightedFormula.toFormulasAndWeights(wFormulas).formulas;
			candidates = this.factory.generateClauses(clauses);
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
			double[] initialWeights = this.weighLearner.weights();
			int initialSize = initialWeights.length;
			
			this.weighLearner.addFormulas(candidates);
			this.weighLearner.learn(initialWeights);
			double[] weights = this.weighLearner.weights();
			ListIterator<Formula> iterator = candidates.listIterator(candidates.size());
			while(iterator.hasPrevious()) {
				this.weighLearner.removeFormula(iterator.previous());
			}
			this.weighLearner.learn(initialWeights);
			
			weights = Arrays.copyOfRange(weights, initialSize, weights.length);
			List<WeightedFormula> wFormulas = WeightedFormula.toWeightedFormulas(candidates, weights);
			Collections.sort(wFormulas, this.comparator);
			wFormulas = wFormulas.subList(0, Math.min(BEAM_SIZE, wFormulas.size()));

			return wFormulas;
		} catch (OptimizationException e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
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
				double[] initialWeights = this.weighLearner.weights();
				initialWeights = Arrays.copyOf(initialWeights, length+1);
				initialWeights[length] = f.getWeight();
				this.weighLearner.addFormula(f.getFormula());
				this.weighLearner.learn(initialWeights);
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
