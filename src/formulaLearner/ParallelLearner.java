package formulaLearner;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import markovLogic.WeightedFormula;
import math.OptimizationException;
import util.MyException;
import weightLearner.WeightLearner;
import fol.Atom;
import fol.Formula;

// TODO: doNotReplace flag? (set initial formulas not replaceable)
public class ParallelLearner implements ScoredLearner {
	
	public static PrintStream out = System.out;
	
	private final Set<Atom> atoms;
	private final List<Formula> formulas;
	
	private final WeightLearner fastLearner;
	private final WeightLearner preciseLearner;
	private final int maxAtoms;
	private final int threads;
	private final double epslon;
	private final double[] initialArgs;
	private final double initialScore;
	private final List<List<Formula>> lenghtFormula;
	private final int m = 50; // TODO: COLOCAR NO BUILDER
	
	private Atom target;
	private boolean hasTarget;
	
	private static final ClauseScore END = new ClauseScore(
			FindCandidates.END, Double.NaN, Double.NaN);
	
	public ParallelLearner(ParallelLearnerBuilder builder) {
		this.atoms = builder.getAtoms();
		this.formulas = new ArrayList<Formula>();
		this.fastLearner = builder.getFastLearner();
		this.preciseLearner = builder.getWeightLearner();
		this.maxAtoms = builder.getMaxAtoms();
		this.threads = builder.getNumberOfThreads();
		this.epslon = builder.getEpslon();
		this.initialArgs = builder.getInitialArgs();
		this.initialScore = builder.getInitialScore();
		this.hasTarget = builder.getTarget() != null;
		this.target = this.hasTarget ? builder.getTarget() : null;
		this.lenghtFormula = new ArrayList<List<Formula>>(this.maxAtoms);
		for (int i = 0; i < this.maxAtoms; i++) { 
			this.lenghtFormula.add(new ArrayList<Formula>());
		}
		if (builder.getFormulas() != null) {
			this.putFormulas(builder.getFormulas());
		}
	}
	
	private BlockingQueue<Formula> findCandidates(List<Formula> seeds) {
		BlockingQueue<Formula> candidates = new LinkedBlockingQueue<Formula>();
		new Thread(new FindCandidates(this.atoms, seeds, candidates, this.threads)).start();
		return candidates;
	}
	
	private BlockingQueue<ClauseScore> scoreCandidates(BlockingQueue<Formula> candidates, 
			double[] initialArgs, double initialScore) {
		BlockingQueue<ClauseScore> scoredQueue = new LinkedBlockingQueue<ClauseScore>();
		int testers = Math.max(1, this.threads-1);
		CountDownLatch done = new CountDownLatch(testers);
		for (int i = 0; i < testers; i++) {
			new Thread(new TestFormula(this.fastLearner, initialArgs, 
					initialScore, done, candidates, scoredQueue, this.epslon)).start();
		}
		new Thread(new ProducersWatcher<ClauseScore>(scoredQueue, done, END)).start();
		return scoredQueue;
	}
	
	private List<ClauseScore> bestCandidates(BlockingQueue<ClauseScore> scoredQueue,
			List<ClauseScore> scoredCandidates, double[] initialArgs, double initialScore) {
		final BlockingQueue<Formula> finalCandidates = new LinkedBlockingQueue<Formula>();
		final Queue<ClauseScore> candidatesQueue = new LinkedList<ClauseScore>();
		CountDownLatch done = new CountDownLatch(1);
		new Thread(new TestFormula(this.preciseLearner, initialArgs, 
				initialScore, done, finalCandidates, candidatesQueue, 1)).start();
		while (true) {
			ClauseScore cs;
			try { cs = scoredQueue.take(); } catch (InterruptedException e) { continue;	}
			if (cs == END) {
				finalCandidates.offer(FindCandidates.END);
				break;	
			}
			if (cs.score > 0.05) {
				finalCandidates.offer(cs.getFormula());
			}
			scoredCandidates.add(cs);
		}
		try { done.await(); } catch (InterruptedException e) { e.printStackTrace(); }
		return new ArrayList<ClauseScore>(candidatesQueue);
	}	
	
	private List<Formula> learn(double[] initialArgs, double initialScore) {
		List<Formula> lastCandidates = this.lenghtFormula.get(0);
		double[] argsE = initialArgs;
		double[] argsF = initialArgs;
		double scoreE = initialScore;
		double scoreF = initialScore;
		int formulaLength = 0;
		
		// put atoms
		try {
			if (this.putAtoms()) {
				argsF = this.fastLearner.learn(argsF);
				argsE = this.preciseLearner.learn(argsF);
			}
		} catch (OptimizationException e) {
			throw new MyException("Unable to optimize args for initial formulas.", e);
		}
		
		// main loop
		boolean findCandidates = true;
		BlockingQueue<Formula> reuse = null;
		do {
			argsE = this.preciseLearner.weights();
			argsF = this.fastLearner.weights();
			scoreE = this.preciseLearner.score();
			scoreF = this.fastLearner.score();
			
			// finds all candidates
			BlockingQueue<Formula> candidates;
			if (findCandidates) {
				candidates = this.findCandidates(lastCandidates);
			} else {
				candidates = reuse;
				findCandidates = true;
			}
			// tests all candidates with fastScore/fastOptimizer
			BlockingQueue<ClauseScore> scoredQueue = this.scoreCandidates(candidates, argsF, scoreF);
			List<ClauseScore> scoredCandidates = new LinkedList<ClauseScore>();
			// All formulas that scored well now will be scored with the exactScore/exactOptimizer
			List<ClauseScore> finalCandidates = this.bestCandidates(scoredQueue, scoredCandidates, 
					argsE, scoreE);
			
			if (finalCandidates.isEmpty()) { // no clauses improved the score
				Collections.sort(scoredCandidates);
				int size = Math.min(scoredCandidates.size(), this.m);
				lastCandidates = WeightedFormula.toFormulasAndWeights(scoredCandidates).
					formulas.subList(0, size);
				formulaLength++;
				lastCandidates.addAll(this.lenghtFormula.get(formulaLength));
				
			} else { // some clause improved the score
				Collections.sort(finalCandidates);
				for (ClauseScore sc : finalCandidates) {
					out.println(sc.getFormula() + " : w = " + sc.getWeight() + ", s = " + sc.score);
				}
				out.println("");
				ClauseScore c = finalCandidates.get(0);
				this.lenghtFormula.get(formulaLength+1).add(c.getFormula());
				this.updateScores(c);
				reuse = new LinkedBlockingQueue<Formula>();
				for (ClauseScore cs : scoredCandidates) {
					if (cs.getFormula() != c.getFormula()) {
						reuse.add(cs.getFormula());
					}
				}
				reuse.add(FindCandidates.END);
				findCandidates = false;
			}
			
		} while (!lastCandidates.isEmpty() && formulaLength < this.maxAtoms);
		
		return this.preciseLearner.getFormulas();
	}
	
	/**
	 * Update the scores with the learned Formula
	 * @param c ClauseScore representing the learned Formula
	 */
	private void updateScores(ClauseScore c) {
		this.preciseLearner.addFormula(c.getFormula());
		this.fastLearner.addFormula(c.getFormula());
		double[] argsE = this.preciseLearner.weights();
		double[] argsF = this.fastLearner.weights();
		argsE = Arrays.copyOf(argsE, argsE.length+1);
		argsF = Arrays.copyOf(argsF, argsF.length+1);
		argsE[argsE.length-1] = c.getWeight();
		argsF[argsF.length-1] = c.getWeight();
		try { 
			this.fastLearner.learn(argsF);
			this.preciseLearner.learn(argsE);
		} catch (Exception e) { 
			// TODO: tratar excecao
			e.printStackTrace(); 
		}
	}
	
	/**
	 * Check if there is any atom not present in formulas, and add them.
	 * @return boolean true if any atom is not present in formulas
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
		double[] args = (this.initialArgs == null) ? new double[0] : this.initialArgs;
		double score = Double.isNaN(this.initialScore) ? 
				this.preciseLearner.getScore().getScore(args) : this.initialScore;
		return this.learn(args, score);
	}
	
	@Override
	public void setTarget(Atom a) {
		this.target = a;
		this.hasTarget = true;
		this.refreshLengthFormula();
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
		for (Formula f : formulas) {
			checkInsert(f, this.preciseLearner);
			checkInsert(f, this.fastLearner);
		}
		this.refreshLengthFormula();
	}
	
	@Override
	public void putFormula(Formula formula) {
		this.formulas.add(formula);
		checkInsert(formula, this.preciseLearner);
		checkInsert(formula, this.fastLearner);
		if (!this.hasTarget() || formula.getAtoms().contains(this.getTarget())) {
			int i = formula.length();
			if (i < this.maxAtoms) {
				this.lenghtFormula.get(i-1).add(formula);
			}
		}
	}
	
	/**
	 * The lengthFormula List is used has seed to generate new formulas
	 * if this list contains only formulas with a given Atom, all
	 * learned formulas will also contain that Atom. Thus we use
	 * this to achieve the target property.
	 */
	private void refreshLengthFormula() {
		for (List<Formula> length : this.lenghtFormula) { 
			length.clear();
		}
		for (Formula f : this.formulas) {
			if (!this.hasTarget() || f.getAtoms().contains(this.getTarget())) {
				int i = f.length();
				if (i < this.maxAtoms) {
					this.lenghtFormula.get(i-1).add(f);
				}
			}
		}
	}

	@Override
	public WeightLearner getWeightLearner() {
		return this.preciseLearner;
	}

	@Override
	public ScoredLearner copy() {
		ParallelLearnerBuilder builder = new ParallelLearnerBuilder();
		builder.setAtoms(this.atoms);
		builder.setEpslon(this.epslon);
		builder.setFastLearner(this.fastLearner.copy());
		builder.setFormulas(new ArrayList<Formula>(this.preciseLearner.getFormulas()));
		builder.setInitialArgs(this.preciseLearner.weights());
		builder.setInitialScore(this.preciseLearner.score());
		builder.setMaxAtoms(this.maxAtoms);
		builder.setNumberOfThreads(this.threads);
		builder.setTarget(this.target);
		builder.setWeightLearner(this.preciseLearner.copy());
		return builder.build();
	}
	
	private static void checkInsert(Formula f, WeightLearner learner) {
		for (Formula g : learner.getFormulas()) {
			if (f == g) {
				return;
			}
		}
		learner.addFormula(f);
	}
	
	
	
}