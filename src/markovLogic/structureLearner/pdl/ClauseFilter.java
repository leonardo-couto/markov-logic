package markovLogic.structureLearner.pdl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import markovLogic.weightLearner.WeightLearner;
import markovLogic.weightLearner.wpll.CountsGenerator;
import math.OptimizationException;
import fol.Clause;
import fol.FormulaFactory;
import fol.WeightedFormula;
import fol.WeightedFormula.AbsoluteWeightComparator;

public class ClauseFilter {
	
	private static final List<Clause> END = Collections.emptyList();
	
	private final WeightLearner wlearner;
	private final FormulaFactory factory;
	private final CountsGenerator counter;
	
	public ClauseFilter(WeightLearner learner, FormulaFactory factory, CountsGenerator counter) {
		this.wlearner = learner;
		this.factory = factory;
		this.counter = counter;
	}
	
	public List<Clause> filter(List<Clause> candidates) {
		
		List<Clause> all = new ArrayList<Clause>();
		Queue<List<Clause>> flipQueue = new ConcurrentLinkedQueue<List<Clause>>();
		
		for (Clause candidate : candidates) {
			List<Clause> flips = this.factory.flipSigns(candidate);
			flipQueue.add(flips);
			all.addAll(flips);			
		}
		
		// generate counts
		this.counter.count(all);
		double[] weights = this.wlearner.weights();
		try {
			weights = this.wlearner.learn(weights);
		} catch(Exception e) {
			weights = Arrays.copyOf(weights, this.wlearner.getFormulas().size());
		}
		double score = this.wlearner.score();
		
		// selection
		int threads = Runtime.getRuntime().availableProcessors();
		CountDownLatch done = new CountDownLatch(threads);
		Queue<WeightedFormula<Clause>> selection = new ConcurrentLinkedQueue<WeightedFormula<Clause>>();
		for (int i = 0; i < threads; i++) {
			flipQueue.offer(END);
			Runner r = new Runner(flipQueue, selection, this.wlearner, weights, score, done);
			new Thread(r).start();
		}
		try {
			done.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		List<WeightedFormula<Clause>> clauses = new ArrayList<WeightedFormula<Clause>>(selection);
		Collections.sort(clauses, new AbsoluteWeightComparator(true));
		return WeightedFormula.toFormulasAndWeights(clauses).formulas;
	}
	
	private static class Runner implements Runnable {
		
		private final Queue<List<Clause>> queue;
		private final CountDownLatch done;
		private final Queue<WeightedFormula<Clause>> clauses;
		private final WeightLearner learner;
		private final double[] weights;
		private final double initialScore;
		
		public Runner(Queue<List<Clause>> queue, Queue<WeightedFormula<Clause>> clauses, 
				WeightLearner learner, double[] weights, double score, CountDownLatch done) {
			this.queue = queue;
			this.done = done;
			this.clauses = clauses;
			this.learner = learner.copy();
			this.weights = weights;
			this.initialScore = score;
		}

		@Override
		public void run() {
			try {
				while (true) {
					List<Clause> flips = this.queue.poll();
					if (flips == ClauseFilter.END) return;
					
					double max = 0;
					int index  = -1;
					for (int i = 0; i < flips.size(); i++) {
						Clause clause = flips.get(i);
						try {
							this.learner.addFormula(clause);
							double w = this.learner.learn(this.weights)[this.weights.length];
							double score = (this.learner.score()-this.initialScore) * Math.abs(w);
							if (score > max) {
								max = score;
								index = i;
							}					
						} catch (OptimizationException e) {
							e.printStackTrace();
						} finally {
							this.learner.removeFormula(clause);
						}
					}
					if (index > -1) {
						WeightedFormula<Clause> cnf = new WeightedFormula<Clause>(flips.get(index), max);
						this.clauses.offer(cnf);
					}					
				}				
			} finally {
				this.done.countDown();
			}
			
		}
		
		
	}

}
