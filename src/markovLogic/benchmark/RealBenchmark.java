package markovLogic.benchmark;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import markovLogic.MarkovLogicNetwork;
import markovLogic.inference.Evidence;
import markovLogic.inference.Inference;
import markovLogic.inference.RealInference;

import org.apache.commons.math.stat.descriptive.StatisticalSummary;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;

import fol.Atom;
import fol.Predicate;
import fol.database.BinaryDatabase;
import fol.database.Groundings;
import fol.database.RealDB;

public class RealBenchmark {
	
	private final RealDB db;
	private final Inference inference;
	private final MarkovLogicNetwork mln;
	private final List<Predicate> predicates;
	
	public RealBenchmark(MarkovLogicNetwork mln, RealDB db, RealInference inference) {
		this.mln = mln;
		this.db = db;
		this.inference = inference;
		this.predicates = new ArrayList<Predicate>(this.mln.getPredicates());
	}
	
	private StatisticalSummary cll(List<Double> points) {
		SummaryStatistics stats = new SummaryStatistics();
		for (Double point : points) {
			double likelihood = 1 - Math.abs(point.doubleValue());
			double cll = Math.log(likelihood);
			stats.addValue(cll);
		}
		return stats;
	}
	
	/**
	 * Performs benchmark on predicate using the default evidence.<br>
	 * Default is using all other predicates on the MLN as evidence.
	 * @param predicate
	 * @return
	 */
	public Result predicate(Predicate predicate) {
		List<Predicate> predicates = new ArrayList<Predicate>(this.predicates);
		if (!predicates.remove(predicate)) 
			throw new IllegalArgumentException("MLN does not contain predicate " + predicate);
		
		Evidence evidence = new Evidence(new BinaryDatabase());
		for (Predicate p : predicates) evidence.set(p, true);

		
		return this.predicate(predicate, evidence);
	}
	
	/**
	 * 
	 * @param predicate
	 * @param evidence
	 * @return
	 */
	public Result predicate(Predicate predicate, Evidence evidence) {
		Iterator<Atom> iterator = Groundings.iterator(predicate, true);
		List<Double> points = new ArrayList<Double>();

		while (iterator.hasNext()) {
			Atom ground = iterator.next();
			double observed = this.inference.pr(ground, evidence);
			double expected = this.db.valueOf(ground);
			points.add(Double.valueOf(observed-expected));
		}
		
		StatisticalSummary cll = this.cll(points);
		
		return new Result(predicate, 0, cll);
	}

	
}
