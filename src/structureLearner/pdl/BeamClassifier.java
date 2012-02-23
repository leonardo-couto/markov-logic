package structureLearner.pdl;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import structureLearner.FormulaTester;
import structureLearner.ScoredFormula;
import util.ProducersWatcher;

import fol.Formula;
import weightLearner.WeightLearner;

public class BeamClassifier {
	
	private final int beamSize;
	private final WeightLearner wlearner;
	private final int threads;
	private final double epslon;
	
	private static final ScoredFormula END = new ScoredFormula(null, 0, 0);
	
	public BeamClassifier(WeightLearner wlearner, int beamSize, double epslon) {
		this.wlearner = wlearner;
		this.beamSize = beamSize;
		this.epslon = epslon;
		this.threads = Runtime.getRuntime().availableProcessors();
	}
	
	/**
	 * Return a List with the highest scored Formulas, limited to <code>beamSize</code> Formulas.
	 * The list is ordered in ascending order of scored. So the Formula with highest score is list
	 * last element.
	 * @param candidates
	 * @param weights
	 * @param score
	 * @return
	 */
	public List<ScoredFormula> beam(List<? extends Formula> candidates, double[] weights, double score) {
		BlockingQueue<ScoredFormula> scoredFormulas = this.scoreCandidates(candidates, weights, score);
		Queue<ScoredFormula> beam = new PriorityQueue<ScoredFormula>(this.beamSize);
		List<ScoredFormula> orderedBeam = new ArrayList<ScoredFormula>(this.beamSize);
		boolean full = false;
		while (true) {
			ScoredFormula formula;
			try { formula = scoredFormulas.take(); } catch (InterruptedException e) { continue;	}
			if (formula == END) break;
			if (full) {
				if (beam.peek().compareTo(formula) <  0) {
					beam.poll();
					beam.offer(formula);
				}
			} else {
				beam.offer(formula);
				full = (this.beamSize == beam.size());
			}
		}
		while (!beam.isEmpty()) {
			orderedBeam.add(beam.poll());
		}
		return orderedBeam;
	}
	
	private BlockingQueue<ScoredFormula> scoreCandidates(List<? extends Formula> candidates, 
			double[] initialArgs, double initialScore) {
		
		Queue<Formula> candidateQueue = new ConcurrentLinkedQueue<Formula>(candidates);
		BlockingQueue<ScoredFormula> scoredQueue = new LinkedBlockingQueue<ScoredFormula>();
		int testers = Math.max(1, this.threads);
		CountDownLatch done = new CountDownLatch(testers);
		
		for (int i = 0; i < testers; i++) {
			new Thread(new FormulaTester(this.wlearner, initialArgs, 
					initialScore, done, candidateQueue, scoredQueue, this.epslon)).start();
		}
		new Thread(new ProducersWatcher<ScoredFormula>(scoredQueue, done, END)).start();
		return scoredQueue;
	}
	
	

}
