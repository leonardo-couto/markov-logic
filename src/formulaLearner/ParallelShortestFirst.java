package formulaLearner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import markovLogic.MarkovLogicNetwork;
import markovLogic.WeightedFormula;
import math.AutomatedLBFGS;
import math.OptimizationException;
import math.Optimizer;
import stat.convergence.SequentialConvergenceTester;
import stat.convergence.SequentialTester;
import util.MyException;
import weightLearner.Score;
import weightLearner.wpll.WeightedPseudoLogLikelihood;
import fol.Atom;
import fol.Formula;
import fol.FormulaFactory;

/**
 * @author Leonardo Castilho Couto
 *
 */
public class ParallelShortestFirst extends AbstractStructLearner {

	private final Score exactScore;
	private final Score fastScore;
	private final Optimizer fastOptimizer;
	private final Optimizer preciseOptimizer;
	
	
	private static final int threads = Runtime.getRuntime().availableProcessors();
	private static final int fpt = 4; // Functions per thread.
	private final FormulaFactory generator;
	
	private final int maxVars = 6; // Max number of distinct variables in a clause.
	private final int maxAtoms = 4; // Max number of Atoms in a clause.	
	private final int k = 1; 
	private final int m = 50;
	
	private final List<Formula> clauses;
	private final List<List<Formula>> lengthClauses;



	public ParallelShortestFirst(Set<Atom> atoms) {
		super(atoms);

		this.clauses = new ArrayList<Formula>(atoms);
		this.generator = new FormulaFactory(atoms);
		this.lengthClauses = new ArrayList<List<Formula>>(this.maxAtoms);
		for (int i = 0; i < this.maxAtoms; i++) {
			this.lengthClauses.add(new ArrayList<Formula>());
		}
		for (Formula clause : this.clauses) {
			this.lengthClauses.get(clause.length()-1).add(clause);
		}
		
		{	// init scores
			WeightedPseudoLogLikelihood exactScore = new WeightedPseudoLogLikelihood(this.predicates);
			WeightedPseudoLogLikelihood fastScore = new WeightedPseudoLogLikelihood(this.predicates);
			SequentialTester tester = new SequentialConvergenceTester(0.95, 0.05);
			tester.setSampleLimit(500);
			fastScore.setSampleLimit(300);
			fastScore.setTester(tester);
			exactScore.addFormulas(this.clauses);
			fastScore.addFormulas(this.clauses);
			this.exactScore = exactScore;
			this.fastScore = fastScore;
		}
		
		{ 	// maxFinders
			this.preciseOptimizer = new AutomatedLBFGS(0.001);
			this.fastOptimizer = new AutomatedLBFGS(0.02);
		}
	}
	
	@Override
	public MarkovLogicNetwork learn() {
		double[] weights = new double[this.clauses.size()];
		try {
			weights = this.preciseOptimizer.max(weights, this.exactScore);
		} catch (OptimizationException e) {
			throw new MyException("Not able to optimize weights for initial (atomic) clauses.", e);
		}
		double score = this.preciseOptimizer.getValue();		
		int i = 0;
		
		while(true) {
			i++;
			System.out.println("**********" + score);
			System.out.println(Arrays.toString(weights)); // TODO: REMOVE!! (LOG)
			
			List<ClauseScore> formulas = this.findBestClauses(score, weights);
			
			if (formulas.isEmpty()) {
				return new MarkovLogicNetwork(WeightedFormula.toWeightedFormulas(this.clauses, weights));
			}
			for (ClauseScore cs : formulas) {
				Formula f = cs.getFormula();
				System.out.println(f); // TODO: remove!! (LOG)
				this.exactScore.addFormula(f);
				this.fastScore.addFormula(f);
				this.clauses.add(f);
				this.lengthClauses.get(f.length()-1).add(f);
			}
			try {
				int length = weights.length;
				weights = Arrays.copyOf(weights, this.clauses.size());
				for (ClauseScore cs : formulas) {
					weights[length] = cs.getWeight();
					length++;
				}
				weights = this.preciseOptimizer.max(weights, this.exactScore);
			} catch (OptimizationException e) {
				// TODO (LOG) and do nothing
				e.printStackTrace();
			}
			score = this.preciseOptimizer.getValue();
		}
	}
	
	public List<ClauseScore> findBestClauses(double score, double[] weights) {
		List<ClauseScore> out = new LinkedList<ClauseScore>();
		Vector<ClauseScore> bestClauses = new Vector<ClauseScore>();
		Vector<ClauseScore> candidates = new Vector<ClauseScore>();
		double[] newWeights = Arrays.copyOf(weights, weights.length + 1);
		
		for (int i = 1; i < this.maxAtoms; i++) {
			// add all clauses of length i;
			Collection<Formula> next = new ArrayList<Formula>(this.lengthClauses.get(i-1));
			
			// add all candidates to be expanded
			Collections.sort(candidates);
			for (int j = 0; j < Math.min(this.m, candidates.size()); j++) {
				next.add(candidates.get(j).getFormula());
			}
			
			candidates = new Vector<ClauseScore>();
			
			FormulaArray formulas = new FormulaArray(fpt,
					this.generator.generateFormulas(next, this.maxAtoms, this.maxVars));
			// next.removeAll(lengthClauses.get(new Integer(i+1)));
			// TODO: Needs to make sure no clause in here is equal clauses already in mln.
			// the above does not work because of Formula.equals.
			
			formulas.setScore(score); // TODO: REMOVE!
			
			CountDownLatch done = new CountDownLatch(threads);
			for (int j = 0; j < threads; j++) {
				new OldTestFormula(this.fastScore, formulas, candidates, 
						bestClauses, newWeights, score, this.fastOptimizer, done);
			}

			try {
				done.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
				// Do nothing
			}
			
			if (!bestClauses.isEmpty()) {
				Collections.sort(bestClauses);
				int j = 0;
				for (ClauseScore c : bestClauses) {
					Formula f = c.getFormula();
					this.exactScore.addFormula(f);
					newWeights[newWeights.length-1] = c.getWeight();
					try {
						double[] temp = this.preciseOptimizer.max(newWeights, this.exactScore);
						double v = this.preciseOptimizer.getValue();
						if (Double.compare(v - score, 0.05d) > 0) {
							System.out.println("AQUI!!!!!!!!!!!!! " + Arrays.toString(temp) + " | " + v);
							out.add(c);
							j++;
						}
					} catch (OptimizationException e) {
						e.printStackTrace();
					}
					this.exactScore.removeFormula(f);
					if (j == this.k) {
						break;
					}
				}
				newWeights[newWeights.length-1] = 0;
				if (j > 0) { return out; }
			}
			
		}
		return out;
	}
	
}
