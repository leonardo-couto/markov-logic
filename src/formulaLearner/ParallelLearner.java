package formulaLearner;

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
// TODO: fazer a parte de targetAtom
public class ParallelLearner implements ScoredLearner {
	
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
			FindCandidates.END, Double.NaN, Double.NaN, new double[0]);
	
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
		this.lenghtFormula = new ArrayList<List<Formula>>(this.maxAtoms);
		for (int i = 0; i < this.maxAtoms; i++) { 
			this.lenghtFormula.add(new ArrayList<Formula>());
		}
		if (builder.getFormulas() != null) {
			this.putFormulas(builder.getFormulas());
		}
	}
	
	public double[] changeArgs(Formula f, double[] lastArgs) {return null;}
	
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
				argsE = this.preciseLearner.learn(argsE);
				argsF = this.fastLearner.learn(argsF);
				scoreE = this.preciseLearner.score();;
				scoreF = this.fastLearner.score();
			}
		} catch (OptimizationException e) {
			throw new MyException("Unable to optimize args for initial formulas.", e);
		}
		
		// main loop
		boolean findCandidates = true;
		BlockingQueue<Formula> reuse = null;
		do {
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
			
			// TODO: REMOVER:
			if (!finalCandidates.isEmpty()) {
				Collections.sort(finalCandidates);
				for (ClauseScore sc : finalCandidates) {
					System.out.println(sc.getFormula() + " : w = " + sc.getWeight() + ", s = " + sc.score);
				}
				System.out.println("");
				ClauseScore c = finalCandidates.get(0);
				this.preciseLearner.addFormula(c.getFormula());
				this.lenghtFormula.get(formulaLength+1).add(c.getFormula());
				argsE = Arrays.copyOf(argsE, argsE.length+1);
				argsE[argsE.length-1] = c.getWeight();
				scoreE = scoreE + c.score;
				this.fastLearner.addFormula(c.getFormula());
				try { this.fastLearner.learn(argsF);
				} catch (Exception e) { e.printStackTrace(); }
				argsF = this.fastLearner.weights();
				scoreF = this.fastLearner.score();
				reuse = new LinkedBlockingQueue<Formula>();
				for (ClauseScore cs : scoredCandidates) {
					if (cs.getFormula() != c.getFormula()) {
						reuse.add(cs.getFormula());
					}
				}
				for (int i = 0; i < this.threads; i++) {
					reuse.add(FindCandidates.END);
				}
				findCandidates = false;
				continue;
			}
			
			// TODO: END REMOVER
			
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
					System.out.println(sc.getFormula() + " : w = " + sc.getWeight() + ", s = " + sc.score);
				}
				// TODO: PAREI AQUI, ATUALIZAR OS SCORES E INITIAL ARGS
				// DAR UM JEITO DE MANTER OS CANDIDATES SEM PASSAR DE NOVO POR ELES
				// foi feito ali em cima, dar uma melhorada e jogar para ca
				break;
			}
			
		} while (!lastCandidates.isEmpty() && formulaLength < this.maxAtoms);
		
		
		
		return this.preciseLearner.getFormulas();
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
		double[] args = (this.initialArgs == null) ? new double[0] : this.initialArgs;
		double score = Double.isNaN(this.initialScore) ? 
				this.preciseLearner.getScore().getScore(args) : this.initialScore;
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
		for (Formula f : formulas) {
			checkInsert(f, this.preciseLearner);
			checkInsert(f, this.fastLearner);
		}
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
		checkInsert(formula, this.preciseLearner);
		checkInsert(formula, this.fastLearner);
		int i = formula.length();
		if (i < this.maxAtoms) {
		  this.lenghtFormula.get(i-1).add(formula);
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