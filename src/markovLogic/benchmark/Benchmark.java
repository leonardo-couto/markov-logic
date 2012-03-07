package markovLogic.benchmark;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import markovLogic.MarkovLogicNetwork;
import markovLogic.inference.Evidence;
import markovLogic.inference.Inference;
import stat.benchmark.InputPoint;
import stat.benchmark.PrecisionRecall;
import fol.Atom;
import fol.FormulaFactory;
import fol.Predicate;
import fol.database.Database;
import fol.database.Groundings;

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
	
	
	public double auc(Predicate predicate) {
		List<Predicate> predicates = new ArrayList<Predicate>(this.predicates);
		if (!predicates.remove(predicate)) 
			throw new IllegalArgumentException("MLN does not contain predicate " + predicate);
		
		Evidence evidence = new Evidence(this.db);
		for (Predicate p : predicates) evidence.set(p, true);
		
		// TODO: preciso de um iterator exato
		Atom atom = FormulaFactory.generateAtom(predicate);
		Iterator<Atom> iterator = new Groundings(atom);
		List<InputPoint> points = new ArrayList<InputPoint>();

		while (iterator.hasNext()) {
			Atom ground = iterator.next();
			double observed = this.inference.pr(ground, evidence);
			boolean expected = this.db.valueOf(ground);
			InputPoint point = new InputPoint(observed, expected);
			points.add(point);			
		}
		
		PrecisionRecall pr = new PrecisionRecall(points);
		
		return pr.auc();
	}

}
