package structureLearner;

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
import fol.Atom;
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
	
	private final Set<Predicate> predicates;
	private final Database db;
	private final List<Atom> atoms;
	private WeightLearner weighLearner;
	
	public SimpleLearner(Set<Predicate> predicates, Database db) {
		this.predicates = predicates;
		this.db = db;
		this.atoms = FormulaFactory.getUnitClauses(predicates);
		
		// Instantiate the weightLearner
		WeightedPseudoLogLikelihood score = new WeightedPseudoLogLikelihood(this.predicates, this.db, 500);
		this.weighLearner = new WeightLearner(score, new AutomatedLBFGS(0.001));
	}

	@Override
	public MarkovLogicNetwork learn() {
		MarkovLogicNetwork mln = new MarkovLogicNetwork();
		
		// add unit clauses, learn weights and gets the score
		double score = this.update(mln, this.atoms, new double[this.atoms.size()]);
		
		System.out.println(mln.toString());
		System.out.println("score: " + score);
		
		return mln;
	}
	
	/**
	 * Add formulas to the mln and updates the MLN weights to 
	 * values that maximize the network score.
	 * @param formulas Formulas to be added to the mln
	 * @param weights added Formulas weights
	 * @return mln's score
	 */
	private double update(MarkovLogicNetwork mln, List<? extends Formula> formulas, double[] weights) {

		// add the formulas to mln and mln's associated weightLearner
		mln.addAll(WeightedFormula.toWeightedFormulas(formulas, weights));
		this.weighLearner.addFormulas(formulas);
		
		// learn the optimum weights
		FormulasAndWeights fw = WeightedFormula.toFormulasAndWeights(mln);
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
