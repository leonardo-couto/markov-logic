package structureLearner;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import markovLogic.MarkovLogicNetwork;
import markovLogic.WeightedFormula;
import markovLogic.WeightedFormula.FormulasAndWeights;
import math.AutomatedLBFGS;
import math.OptimizationException;
import util.MyException;
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
public class SimpleLearner implements StructureLearner {
	
	private final int MAX_VARS = 6;
	
	private final Set<Predicate> predicates;
	private final Database db;
	private final FormulaFactory factory;
	private final List<ConjunctiveNormalForm> atoms;
	private WeightLearner weighLearner;
	
	public SimpleLearner(Set<Predicate> predicates, Database db) {
		this.predicates = predicates;
		this.db = db;
		this.factory = new FormulaFactory(predicates, MAX_VARS);
		this.atoms = this.factory.getUnitClauses();
		
		// Instantiate the weightLearner
		WeightedPseudoLogLikelihood score = new WeightedPseudoLogLikelihood(this.predicates, this.db, 500);
		this.weighLearner = new WeightLearner(score, new AutomatedLBFGS(0.001));
	}

	@Override
	public MarkovLogicNetwork learn() {
		MarkovLogicNetwork mln = new MarkovLogicNetwork();
		
		// add unit clauses, learn weights and gets the score
		double score = this.update(mln, this.atoms);
		
//		System.out.println(mln.toString());
//		System.out.println("score: " + score);
		
		List<ConjunctiveNormalForm> clauses = null;
		while (true) {
			clauses = findBestClauses(mln, score);
			clauses = Collections.emptyList();
			if (clauses.isEmpty()) {
				break;
			}
			score = this.update(mln, clauses);
		}
		
		return mln;
	}
	
	private List<ConjunctiveNormalForm> findBestClauses(MarkovLogicNetwork mln, double score) {
		
		this.factory.printCandidates(this.atoms);

	
		// - nunca repetir uma variável em um mesmo predicado
		// - fazer uma lista de todos os atomos que compartilham uma variável com a fórmula de todas
		// as possiveis maneiras.
		
		// - colocar os iguais
		// - adicionar o atomo e sua negaçao
		
		
		return Collections.emptyList();
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
	
	
	
	

}
