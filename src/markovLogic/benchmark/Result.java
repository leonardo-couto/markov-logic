package markovLogic.benchmark;

import org.apache.commons.math.stat.descriptive.StatisticalSummary;

import fol.Predicate;

public class Result {
	
	public final Predicate predicate;
	public final double auc;
	public final StatisticalSummary cll;
	
	public Result(Predicate predicate, double auc, StatisticalSummary cll) {
		this.predicate = predicate;
		this.auc = auc;
		this.cll = cll;
	}	

}
