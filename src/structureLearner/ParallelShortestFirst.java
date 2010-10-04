package structureLearner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import math.AutomatedLBFGS;
import math.LBFGS.ExceptionWithIflag;
import math.MaxFinder;
import math.OptimizationException;
import weightLearner.Score;
import weightLearner.WeightedPseudoLogLikelihood;
import fol.Formula;
import fol.Predicate;

/**
 * @author Leonardo Castilho Couto
 *
 */
public class ParallelShortestFirst extends AbstractLearner {
	
	private static final int threads = Runtime.getRuntime().availableProcessors();
	private static final int fpt = 30; // Functions per thread.
	
	private final List<Formula> clauses;
	private final List<List<Formula>> lengthClauses;
	private final FormulaGenerator generator;
	private final int k, m;
	private final double epslon;
	private final Score wscore;
	private final MaxFinder maxFinder;


	public ParallelShortestFirst(Set<Predicate> p) {
		super(p);
		clauses = new ArrayList<Formula>(FormulaGenerator.unitClauses(p));
		generator = new FormulaGenerator(p);
		lengthClauses = new ArrayList<List<Formula>>(generator.getMaxAtoms());
		for (int i = 0; i < generator.getMaxAtoms(); i++) {
			lengthClauses.add(new ArrayList<Formula>());
		}
		for (Formula clause : clauses) {
			lengthClauses.get(clause.length()-1).add(clause);
		}
		this.wscore = new WeightedPseudoLogLikelihood(p);
		wscore.addFormulas(clauses);
		maxFinder = new AutomatedLBFGS();
		m = 50;
		k = 1;
		epslon = 0.5;
	}

	@Override
	public Set<Formula> learn() {
		double[] weights = new double[clauses.size()];
		try {
			weights = maxFinder.max(weights, this.wscore, this.wscore);
		} catch (OptimizationException e) {
			throw new Run
		}
		double score = wscore.getScore(weights);
		
		int i = 0;
		
		while(i < 100) {
			i++;
			System.out.println("**********" + score);
			System.out.println(Arrays.toString(weights)); // TODO: REMOVE!!
			
			Set<Formula> formulas = findBestClauses(score, weights);
			
			if (formulas.isEmpty()) {
				return new HashSet<Formula>(clauses);
			}
			for (Formula f : formulas) {
				System.out.println(f); // TODO: remove!!
				wscore.addFormula(f);
				clauses.add(f);
				lengthClauses.get(f.length()-1).add(f);
			}
			try {
				weights = Arrays.copyOf(weights, clauses.size());
				weights = maxFinder.max(weights, this.wscore, this.wscore);
			} catch (ExceptionWithIflag e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			score = wscore.getScore(weights);
		}
		return new HashSet<Formula>(clauses);
	}
	
	public Set<Formula> findBestClauses(double score, double[] weights) {
		Set<Formula> out = new HashSet<Formula>();
		Vector<WeightedClause> bestClauses = new Vector<WeightedClause>();
		Vector<WeightedClause> candidates = new Vector<WeightedClause>();
		double[] newWeights = Arrays.copyOf(weights, weights.length + 1);
		
		for (int i = 1; i < generator.getMaxAtoms(); i++) {
			// add all clauses of length i;
			Collection<Formula> next = new ArrayList<Formula>(lengthClauses.get(i-1));
			
			// add all candidates to be expanded
			Collections.sort(candidates);
			for (int j = 0; j < Math.min(m, candidates.size()); j++) {
				next.add(candidates.get(j).clause);
			}
			
			candidates = new Vector<WeightedClause>();
			
			FormulaArray formulas = new FormulaArray(generator.generateFormulas(next));
			// next.removeAll(lengthClauses.get(new Integer(i+1)));
			// TODO: Needs to make sure no clause in here is equal clauses already in mln.
			// the above does not work because of Formula.equals.
			
			formulas.setScore(score); // TODO: REMOVE!
			
			CountDownLatch done = new CountDownLatch(threads);
			for (int j = 0; j < threads; j++) {
				new TestFormula(wscore, formulas, candidates, bestClauses, newWeights, score, done);
			}

			try {
				done.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
				// Do nothing
			}
			
			if (!bestClauses.isEmpty()) {
				Collections.sort(bestClauses);
				for (int j = 0; j < Math.min(k, bestClauses.size()); j++) {
					out.add(bestClauses.get(j).clause);
				}
				return out;
			}
			
		}
		return out;
	}
	
	class WeightedClause implements Comparable<WeightedClause> {
		
		public WeightedClause(Formula clause, double score, double weight) {
			this.score = score;
			this.clause = clause;
			this.weight = weight;
		}
		
		double score;
		double weight;
		Formula clause;
		
		@Override
		public int compareTo(WeightedClause o) {
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
		private AutomatedLBFGS lbfgs = new AutomatedLBFGS();
		private Vector<WeightedClause> candidates;
		private Vector<WeightedClause> bestClauses;
		private FormulaArray fArray;
		private double[] weights;
		private final double score;
		public final Thread t;
		private CountDownLatch done;
		
		public TestFormula(Score wscore, FormulaArray fArray, 
				Vector<WeightedClause> candidates, Vector<WeightedClause> bestClauses,
				double[] weights, double score, CountDownLatch done) {
			this.wpll = wscore.copy();
			this.fArray = fArray;
			this.candidates = candidates;
			this.bestClauses = bestClauses;
			this.weights = Arrays.copyOf(weights, weights.length);
			this.score = score;
			this.done = done;
			this.t = new Thread(this);
			t.start();
		}

		@Override
		public void run() {
			try { 
				while (true) {
					formulas = fArray.getElements();
					if (formulas == null || Thread.interrupted()) {
						done.countDown();
						return;
					}
					double bestscore = fArray.getScore(); // TODO: REMOVE!
					for (Formula f : formulas) {

						wpll.addFormula(f);
						double newScore = 0;
						double learnedWeight;
						double[] nweights;
						try {
							nweights = lbfgs.max(weights, this.wpll, this.wpll);
							learnedWeight = nweights[nweights.length -1]; 
							newScore = wpll.f(nweights);
						} catch (ExceptionWithIflag e) {
							wpll.removeFormula(f);
							continue;
						}
						wpll.removeFormula(f);

						if (Double.compare(newScore, score) > 0 && Double.compare(Math.abs(learnedWeight), epslon) > 0) {
							if (Double.compare(newScore, bestscore) > 0) {  // TODO: Remove
								System.out.println(Double.toString(score - newScore) + " " + f + "\n" + Arrays.toString(nweights));
								bestscore = newScore;
							}
							bestClauses.add(new WeightedClause(f, score - newScore, learnedWeight));
						} else {
							candidates.add(new WeightedClause(f, score - newScore, learnedWeight));
						}
					}
					fArray.setScore(bestscore);
				}
			} catch (Exception e) {
				e.printStackTrace();
				done.countDown();
			}
		}
	}

}
