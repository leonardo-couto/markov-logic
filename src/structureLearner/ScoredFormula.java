package structureLearner;

import java.util.PriorityQueue;
import java.util.Queue;

import markovLogic.WeightedFormula;
import fol.Formula;

public class ScoredFormula extends WeightedFormula implements Comparable<ScoredFormula> {
	
	public ScoredFormula(Formula clause, double score, double weight) {
		super(clause, weight);
		this.score = score;
	}
	
	final double score;
	
	@Override
	public int compareTo(ScoredFormula o) {
		return Double.compare(this.score, o.score);
	}
	
	public double getScore() {
		return this.score;
	}
	
	// TODO remover!!!!!!!!!!!!!!!
	public static void main(String[] args) {
		Queue<ScoredFormula> queue = new PriorityQueue<ScoredFormula>();
		queue.offer(new ScoredFormula(null, -2, 0));
		queue.offer(new ScoredFormula(null, 0, 0));
		queue.offer(new ScoredFormula(null, 5, 0));
		queue.offer(new ScoredFormula(null, 2, 0));
		while (!queue.isEmpty()) {
			System.out.println(queue.poll().score);
		}
		System.out.println((new ScoredFormula(null, -0.5, 0)).compareTo(new ScoredFormula(null, 0, 0)));
	}

	
}