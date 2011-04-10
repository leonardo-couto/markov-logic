package structureLearner;

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
import math.LBFGS.ExceptionWithIflag;
import math.MaxFinder;
import math.OptimizationException;
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
public class ParallelShortestFirst extends AbstractLearner {
	**
	// PAREI AQUI!!!!! criar dois WPLLS, um com sampleLimit e convergenceTester ruim, e outro sem limite e sem convergenceTester.
	//
	// colocar limite 500 para o formulaCount e 300 para o predicateWpll
	// antes de adicionar as melhores formulas, calcular os n melhores com o wpll bom.
	
	
	private static final int threads = Runtime.getRuntime().availableProcessors();
	private static final int fpt = 5; // Functions per thread.
	private final FormulaFactory generator;
	
	private final int maxVars = 6; // Max number of distinct variables in a clause.
	private final int maxAtoms = 4; // Max number of Atoms in a clause.	
	private final int k = 1; 
	private final int m = 50;
	private final double epslon = 0.5;
	private final Score wscore;
	private final MaxFinder maxFinder;
	
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
		this.wscore = new WeightedPseudoLogLikelihood(this.predicates);
		this.wscore.addFormulas(this.clauses);
		this.maxFinder = new AutomatedLBFGS();
		this.maxFinder.setPrecision(0.001);
	}
	
	@Override
	public MarkovLogicNetwork learn() {
		double[] weights = new double[clauses.size()];
		try {
			weights = this.maxFinder.max(weights, this.wscore, this.wscore);
		} catch (OptimizationException e) {
			throw new MyException("Not able to optimize weights for initial (atomic) clauses.", e);
		}
		double score = this.wscore.getScore(weights);
		
		int i = 0;
		
		while(i < 100) { // TODO: 100?? PQ?? Limite maximo de iteracao, podia ser true, quando parar de melhorar findBestClauses faz terminar
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
				this.wscore.addFormula(f);
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
				weights = formulas.get(0).weights;
				System.out.println("**********");
				System.out.println(score = this.wscore.getScore(weights));
				System.out.println(Arrays.toString(weights));
				weights = this.maxFinder.max(weights, this.wscore, this.wscore);
			} catch (OptimizationException e) {
				// TODO (LOG) and do nothing
				e.printStackTrace();
			}
			score = this.wscore.getScore(weights);
		}
		return new MarkovLogicNetwork(WeightedFormula.toWeightedFormulas(this.clauses, weights));
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
			
			FormulaArray formulas = new FormulaArray(
					this.generator.generateFormulas(next, this.maxAtoms, this.maxVars));
			// next.removeAll(lengthClauses.get(new Integer(i+1)));
			// TODO: Needs to make sure no clause in here is equal clauses already in mln.
			// the above does not work because of Formula.equals.
			
			formulas.setScore(score); // TODO: REMOVE!
			
			CountDownLatch done = new CountDownLatch(threads);
			for (int j = 0; j < threads; j++) {
				new TestFormula(this.wscore, formulas, candidates, bestClauses, newWeights, score, done);
			}

			try {
				done.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
				// Do nothing
			}
			
			if (!bestClauses.isEmpty()) {
				Collections.sort(bestClauses);
				for (int j = 0; j < Math.min(this.k, bestClauses.size()); j++) {
					out.add(bestClauses.get(j));
				}
				return out;
			}
			
		}
		return out;
	}
	
	class ClauseScore extends WeightedFormula implements Comparable<ClauseScore> {
		
		public ClauseScore(Formula clause, double score, double weight, double[] weights) {
			super(clause, weight);
			this.score = score;
			this.weights = weights;
		}
		
		final double score;
		final double[] weights; // TODO: REMOVER!!!!!!!!!!!!!!!!
		
		@Override
		public int compareTo(ClauseScore o) {
			return Double.compare(this.score, o.score);
		}

		
	}

	class FormulaArray extends ArrayList<Formula> {
		
		private static final long serialVersionUID = 4281216655319484386L;
		private int idx = 0;
		
		public synchronized Formula[] getElements() {
			int size = this.size();
			if (idx <= size) {
				System.out.println("(" + idx + " ate " + Math.min(idx + fpt, size) + ")/" + size); // TODO: REMOVER!!
				Formula[] out = this.subList(idx, Math.min(idx + fpt, size)).toArray(new Formula[0]);
				idx = idx + fpt;
				return out;
			}
			return null;
		}
		
		// TODO: temp, remover
		double score = Double.NEGATIVE_INFINITY;
		public synchronized double getScore() {
			return score;
		}
		public synchronized void setScore(double d) {
			if (Double.compare(d, score) > 0) {
				score = d;
			}
		} // TODO: fim do remove

		public FormulaArray() {
			super();
		}

		public FormulaArray(Collection<? extends Formula> c) {
			super(c);
		}
		
	}


	class TestFormula implements Runnable {
		
		private Score wpll;
		private Formula[] formulas;
		private MaxFinder lbfgs = new AutomatedLBFGS();
		private Vector<ClauseScore> candidates;
		private Vector<ClauseScore> bestClauses;
		private FormulaArray fArray;
		private double[] weights;
		private final double score;
		public final Thread t;
		private CountDownLatch done;
		
		public TestFormula(Score wscore, FormulaArray fArray, 
				Vector<ClauseScore> candidates, Vector<ClauseScore> bestClauses,
				double[] weights, double score, CountDownLatch done) {
			this.wpll = wscore.copy();
			this.fArray = fArray;
			this.candidates = candidates;
			this.bestClauses = bestClauses;
			this.weights = Arrays.copyOf(weights, weights.length);
			this.score = score;
			this.done = done;
			this.t = new Thread(this);
			this.t.start();
			this.lbfgs.setPrecision(0.05);
		}

		@Override
		public void run() {
			try { 
				while (true) {
					this.formulas = this.fArray.getElements();
					if (this.formulas == null || Thread.interrupted()) {
						return;
					}
					double bestscore = this.fArray.getScore(); // TODO: REMOVE!
					for (Formula f : this.formulas) {

						if (!this.wpll.addFormula(f)) { continue; }
						double newScore = 0;
						double learnedWeight;
						double[] nweights;
						try {
							nweights = this.lbfgs.max(this.weights, this.wpll, this.wpll);
							learnedWeight = nweights[nweights.length -1]; 
							newScore = this.wpll.f(nweights);
						} catch (ExceptionWithIflag e) {
							this.wpll.removeFormula(f);
							continue;
						} catch (Exception e) {
							e.printStackTrace();
							System.out.println(f + "\n" + Arrays.toString(this.weights) + "\n" + this.wpll.getFormulas());
							System.exit(1);
							continue;
						}
						this.wpll.removeFormula(f);

						if (Double.compare(newScore, this.score+0.01) > 0 && Double.compare(Math.abs(learnedWeight), epslon) > 0) {
							if (Double.compare(newScore, bestscore) > 0) {  // TODO: Remove
								System.out.println(Double.toString(newScore) + " " + f + "\n" + Arrays.toString(nweights));
								bestscore = newScore;
							}
							this.bestClauses.add(new ClauseScore(f, this.score - newScore, learnedWeight, nweights));
						} else {
							this.candidates.add(new ClauseScore(f, this.score - newScore, learnedWeight, nweights));
						}
					}
					this.fArray.setScore(bestscore);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				this.done.countDown();
			}
		}
	}

}
