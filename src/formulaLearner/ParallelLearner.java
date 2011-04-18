package formulaLearner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import math.OptimizationException;
import math.Optimizer;
import util.MyException;
import weightLearner.Score;
import fol.Atom;
import fol.Formula;
import fol.Predicate;

// TODO: doNotReplace flag? (set initial formulas not replaceable)
// TODO: fazer a parte de targetAtom
public class ParallelLearner implements FormulaLearner {
	
	private final Set<Predicate> predicates;
	private final Set<Atom> atoms;
	//private final Map<String, Atom> equals;
	private final List<Formula> formulas;
	
	private final Score exactScore;
	private final Score fastScore;
	private final Optimizer fastOptimizer;
	private final Optimizer preciseOptimizer;
	private final int maxVars; // TODO: USAR O MAXVARS!
	private final int maxAtoms;
	private final int threads;
	private final double epslon;
	private final double[] initialArgs;
	private final double initialScore;
	private final List<List<Formula>> lenghtFormula;
	private final int k = 1; // TODO: colocar no builder
	private final int m = 50;
	
	private Atom target;
	private boolean hasTarget;
	
	private static final ClauseScore END = new ClauseScore(FindCandidates.END, Double.NaN, Double.NaN, new double[0]);
	
	public ParallelLearner(ParallelLearnerBuilder builder) {
		this.atoms = builder.getAtoms();
		this.predicates = new HashSet<Predicate>(atoms.size()*2);
		for (Atom a : atoms) { this.predicates.add(a.predicate); }
		this.formulas = new ArrayList<Formula>();
		this.exactScore = builder.getExactScore();
		this.fastScore = builder.getFastScore();
		this.fastOptimizer = builder.getFastOptimizer();
		this.preciseOptimizer = builder.getPreciseOptimizer();
		this.maxAtoms = builder.getMaxAtoms();
		this.maxVars = builder.getMaxVariables();
		this.threads = builder.getNumberOfThreads();
		this.epslon = builder.getEpslon();
		this.initialArgs = builder.getInitialArgs();
		this.initialScore = builder.getInitialScore();
		this.lenghtFormula = new ArrayList<List<Formula>>(this.maxAtoms);
		for (int i = 0; i < this.maxAtoms; i++) { 
			this.lenghtFormula.add(new ArrayList<Formula>());
		}
		if (builder.getFormulas() != null) {
			this.putFormulas(builder.getFormulas());
		}
	}
	
	public double[] changeArgs(Formula f, double[] lastArgs) {return null;}
	
	public List<Formula> learn(double[] initialArgs, double initialScore) {
		List<Formula> lastCandidates = this.lenghtFormula.get(0);
		double[] argsE = initialArgs;
		double[] argsF = initialArgs;
		double scoreE = initialScore;
		double scoreF = initialScore;
		int formulaLength = 0;
		
		// put atoms
		try {
			if (this.putAtoms()) {
				argsE = this.preciseOptimizer.max(argsE, this.exactScore);
				argsF = this.fastOptimizer.max(argsF, this.fastScore);
				scoreE = this.preciseOptimizer.getValue();
				scoreF = this.fastOptimizer.getValue();
			}
		} catch (OptimizationException e) {
			throw new MyException("Unable to optimize args for initial formulas.", e);
		}
		
		// main loop
		do {
			// finds all candidates
			final BlockingQueue<Formula> candidates = new LinkedBlockingQueue<Formula>();
			new Thread(new FindCandidates(this.atoms, lastCandidates, candidates, this.threads)).start();

			// tests all candidates with fastScore/fastOptimizer
			final BlockingQueue<ClauseScore> scoredQueue = new LinkedBlockingQueue<ClauseScore>();
			final List<ClauseScore> scoreCandidates = new LinkedList<ClauseScore>();
			int testers = Math.max(1, this.threads-1);
			CountDownLatch done = new CountDownLatch(testers);
			for (int i = 0; i < testers; i++) {
				new Thread(new TestFormula(this.fastScore, this.fastOptimizer, argsF, scoreF, done, candidates, scoredQueue, this.epslon)).start();
			}
			new Thread(new ProducersWatcher<ClauseScore>(scoredQueue, done, END)).start();
			
			// All formulas that scored well now will be scored with the exactScore/exactOptimizer
			final BlockingQueue<Formula> finalCandidates = new LinkedBlockingQueue<Formula>();
			final Queue<ClauseScore> finalQueue = new LinkedList<ClauseScore>();
			CountDownLatch finalLatch = new CountDownLatch(1);
			new Thread(new TestFormula(this.exactScore, this.preciseOptimizer, argsE, scoreE, finalLatch, finalCandidates, finalQueue, 1)).start();
			while (true) {
				ClauseScore cs;
				try { cs = scoredQueue.take(); } catch (InterruptedException e) { continue;	}
				if (cs == END) {
					finalCandidates.offer(FindCandidates.END);
					break;	
				}
				finalCandidates.offer(cs.getFormula());
				scoreCandidates.add(cs);
			}
			try { finalLatch.await(); } catch (InterruptedException e) { e.printStackTrace(); }
			
			if (finalQueue.isEmpty()) { // no clauses improved the score
				Collections.sort(scoreCandidates);
				lastCandidates = new LinkedList<Formula>();
				int i = 0;
				for (ClauseScore cs : scoreCandidates) {
					if (i == this.m) {
						break;
					}
					lastCandidates.add(cs.getFormula());
				}
				formulaLength++;
				lastCandidates.addAll(this.lenghtFormula.get(formulaLength));
				
			} else { // some clause improved the score
				List<ClauseScore> clauses = new ArrayList<ClauseScore>(finalQueue);
				Collections.sort(clauses);
				for (ClauseScore sc : clauses) {
					System.out.println(sc.getFormula() + " : w = " + sc.getWeight() + ", s = " + sc.score);
				}
				break;
			}
			
		} while (!lastCandidates.isEmpty() && formulaLength < this.maxAtoms);
		
		
		
		return null;
	}
	
	/**
	 * Check if there is any atom not present in formulas, and add them.
	 * @return boolean if any atom is not present in formulas
	 */
	private boolean putAtoms() {
		boolean b = false;
		for (Atom a : atoms) {
			if(!this.formulas.contains(a)) {
				b = true;
				this.putFormula(a);
			}
		}
		return b;
	}
	
	@Override
	public List<Formula> learn() {
		double[] args = (this.initialArgs == null) ? 
				new double[0] : this.initialArgs;
		double score = Double.isNaN(this.initialScore) ? 
				this.exactScore.getScore(args) : this.initialScore;
		return this.learn(args, score);
	}
	
	@Override
	public void setTarget(Atom a) {
		this.target = a;
		this.hasTarget = true;
	}
	
	protected Atom getTarget() {
		return this.target;
	}
	
	protected boolean hasTarget() {
		return this.hasTarget;
	}
	
	@Override
	public void putFormulas(List<Formula> formulas) {
		this.formulas.addAll(formulas);
		this.exactScore.addFormulas(formulas);
		this.fastScore.addFormulas(formulas);
		for (Formula f : formulas) {
			int i = f.length();
			if (i < this.maxAtoms) {
			  this.lenghtFormula.get(i-1).add(f);
			}
		}
	}
	
	@Override
	public void putFormula(Formula formula) {
		this.formulas.add(formula);
		this.exactScore.addFormula(formula);
		this.fastScore.addFormula(formula);
		int i = formula.length();
		if (i < this.maxAtoms) {
		  this.lenghtFormula.get(i-1).add(formula);
		}
	}
	
	
	
}