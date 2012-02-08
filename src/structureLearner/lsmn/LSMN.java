package structureLearner.lsmn;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import markovLogic.MarkovLogicNetwork;
import markovLogic.WeightedFormula;
import markovLogic.WeightedFormula.FormulasAndWeights;
import math.AutomatedLBFGS;
import math.OptimizationException;
import structureLearner.ScoredFormula;
import structureLearner.StructureLearner;
import util.MyException;
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
public class LSMN implements StructureLearner {
	
	private static final int MAX_VARS = 6;
	private static final int BEAM_SIZE = 20;
	private static final double EPSLON = 0.5; // min absolute weight
	
	private final Set<Predicate> predicates;
	private final Database db;
	private final FormulaFactory factory;
	private final List<ConjunctiveNormalForm> atoms;
	private final WeightLearner weighLearner;
	
	private final Score score;
	private final Score l1Score;
	
	public LSMN(Set<Predicate> predicates, Database db) {
		this.predicates = predicates;
		this.db = db;
		this.factory = new FormulaFactory(predicates, MAX_VARS);
		this.atoms = this.factory.getUnitClauses();
		
		// Instantiate the weightLearner
		this.score = new WeightedPseudoLogLikelihood(this.predicates, this.db, 1000);
		L1RegularizedScore l1Score = new L1RegularizedScore(this.score).setConstantWeight(-0.1);
		l1Score.setStart(6);
		this.l1Score = l1Score;
			
		this.weighLearner = new WeightLearner(this.l1Score, new AutomatedLBFGS(0.001));
	}

	@Override
	public MarkovLogicNetwork learn() {
		MarkovLogicNetwork mln = new MarkovLogicNetwork();
		
		// add unit clauses, learn weights and gets the score
		double score = this.update(mln, this.atoms);
		
//		System.out.println(mln.toString());
//		System.out.println("score: " + score);

//		try {
//			if (Integer.valueOf(1).intValue() == 1) { // TODO REMOVER!!
//				System.out.println("SCORE:" + score);
//
//
//
//				for (Predicate p : this.predicates) {
//					double[] weights = new double[mln.size()+1];
//					double[] mlnWeights = WeightedFormula.toFormulasAndWeights(mln).weights;
//					for (int i = 0; i < mlnWeights.length; i++) {
//						weights[i] = mlnWeights[i];
//					}
//					weights[weights.length-1] = -2;
//					Atom a = FormulaFactory.generateAtom(p);
//					Formula tautology = Conjunction.OPERATOR.apply(a, Negation.OPERATOR.apply(a));
//					this.weighLearner.addFormula(tautology);
//					weights = this.weighLearner.learn(weights);
//					System.out.println(tautology.toString());
//					System.out.println(String.format("weight: %s, score: %s.",weights[weights.length-1], this.weighLearner.score()-score));
//					this.weighLearner.removeFormula(tautology);
//				}
//
//
//				return mln;
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

		ScoredFormula clause = null;
		while (true) {
			clause = findBestClause(mln, score);
			if (clause == null) {
				break;
			}
			System.out.println(clause.getFormula());
			score = this.update(mln, clause);
		}
		
		return mln;
	}
	
	private ScoredFormula findBestClause(MarkovLogicNetwork mln, double score) {
		
		List<Formula> clauses = new ArrayList<Formula>(this.factory.generateClauses(this.atoms));
		ScoredFormula bestClause = new ScoredFormula(null, 0, 0);
		BeamClassifier beam = new BeamClassifier(this.weighLearner, BEAM_SIZE, EPSLON);
		double[] weights = WeightedFormula.toFormulasAndWeights(mln).weights;
		
		boolean improved = true;
		for (int i = 0; i < 50; i++) {
			List<ScoredFormula> scoredClauses = beam.beam(clauses, weights, score);
			if (scoredClauses.isEmpty()) break;
			ScoredFormula best = scoredClauses.get(scoredClauses.size()-1);
			if (best.compareTo(bestClause) > 0) {
				improved = true;
				bestClause = best;
			} else {
				if (!improved) {
					break;
				}
				improved = false;
			}
			clauses.clear();
			List<ConjunctiveNormalForm> bestClauses = new ArrayList<ConjunctiveNormalForm>(scoredClauses.size());
			for (ScoredFormula f : scoredClauses) {
				bestClauses.add((ConjunctiveNormalForm) f.getFormula());
			}
			clauses = new ArrayList<Formula>(this.factory.generateClauses(bestClauses));
		}
		
		return bestClause.getFormula() == null ? null : bestClause;
	}
	
	/**
	 * Add formulas to the mln and updates the MLN weights to 
	 * values that maximize the network score.
	 * @param formulas Formulas to be added to the mln
	 * @param weights added Formulas weights
	 * @return mln's score
	 */
	private double update(MarkovLogicNetwork mln, List<? extends Formula> formulas) {

		// add the formulas to mln and mln's associated weightLearner
		mln.addAll(WeightedFormula.toWeightedFormulas(formulas, new double[formulas.size()]));
		this.weighLearner.addFormulas(formulas);
		
		// learn the optimum weights
		FormulasAndWeights fw = WeightedFormula.toFormulasAndWeights(mln);
		double[] weights;
		try {
			weights = this.weighLearner.learn(fw.weights);
		} catch (OptimizationException e) {
			throw new MyException(
					"Fatal error while learning the MLN weights.", e);
		}
		
		// update the MLN with optimum weights
		mln.clear();
		mln.addAll(WeightedFormula.toWeightedFormulas(fw.formulas, weights));
		
		return this.weighLearner.score();
	}
	
	/**
	 * Add formulas to the mln and updates the MLN weights to 
	 * values that maximize the network score.
	 * @param formulas Formulas to be added to the mln
	 * @param weights added Formulas weights
	 * @return mln's score
	 */
	private double update(MarkovLogicNetwork mln, WeightedFormula clause) {

		// add the formulas to mln and mln's associated weightLearner
		mln.add(clause);
		this.weighLearner.addFormula(clause.getFormula());
		
		// learn the optimum weights
		FormulasAndWeights fw = WeightedFormula.toFormulasAndWeights(mln);
		double[] weights;
		try {
			weights = this.weighLearner.learn(fw.weights);
		} catch (OptimizationException e) {
			throw new MyException(
					"Fatal error while learning the MLN weights.", e);
		}
		
		// update the MLN with optimum weights
		mln.clear();
		mln.addAll(WeightedFormula.toWeightedFormulas(fw.formulas, weights));
		
		return this.weighLearner.score();
	}
	
	
	
	

}
