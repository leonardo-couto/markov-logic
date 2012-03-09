package markovLogic.benchmark;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import markovLogic.MarkovLogicNetwork;
import markovLogic.inference.Evidence;
import markovLogic.inference.Inference;

import org.apache.commons.math.stat.descriptive.StatisticalSummary;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;

import stat.benchmark.InputPoint;
import stat.benchmark.PrecisionRecall;
import fol.Atom;
import fol.Predicate;
import fol.database.Database;
import fol.database.Groundings;

/**
 * See metodology in Kok and Domingos - Learning MLN Structure via
 * Hypergraph Lifting - 2009
 */
public class Benchmark {
	
	private final Database db;
	private final Inference inference;
	private final MarkovLogicNetwork mln;
	private final List<Predicate> predicates;
	
	public Benchmark(MarkovLogicNetwork mln, Database db, Inference inference) {
		this.mln = mln;
		this.db = db;
		this.inference = inference;
		this.predicates = new ArrayList<Predicate>(this.mln.getPredicates());
	}
	
	private double auc(List<InputPoint> points) {
		PrecisionRecall pr = new PrecisionRecall(points);
		return pr.auc();		
	}
	
	private StatisticalSummary cll(List<InputPoint> points) {
		SummaryStatistics stats = new SummaryStatistics();
		for (InputPoint point : points) {
			double likelihood = point.expected ? point.observed : 1-point.observed;
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
		
		Evidence evidence = new Evidence(this.db);
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
		List<InputPoint> points = new ArrayList<InputPoint>();

		while (iterator.hasNext()) {
			Atom ground = iterator.next();
			double observed = this.inference.pr(ground, evidence);
			boolean expected = this.db.valueOf(ground);
			InputPoint point = new InputPoint(observed, expected);
			points.add(point);
		}
		
		double auc = this.auc(points);
		StatisticalSummary cll = this.cll(points);
		
		return new Result(predicate, auc, cll);
	}
	
}
