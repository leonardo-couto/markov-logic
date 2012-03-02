package fol.database;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import stat.convergence.SequentialConvergenceTester;
import stat.convergence.SequentialTester;
import fol.Atom;
import fol.Constant;
import fol.Term;
import fol.Variable;

/**
 * <p>This class is a Iterator for groundings of a given Atom.</p>
 * <p>The groundings order are random and it is not guaranteed 
 * that all of them will be sampled. It is also possible that some
 * atoms will be sampled more than once.</p>
 */
public class Groundings implements Iterator<Atom> {
	
	private final Atom filter;
	private final Map<Variable, Constant> groundings;
	private final int size;
	
	private int counter;

	public Groundings(Atom filter) {
		this.filter = filter;
		this.groundings = new HashMap<Variable, Constant>();
		this.size = this.init();
		this.counter = 0;
	}
	
	private int init() {
		long l = 1;
		for (Term t : this.filter.terms) {
			if (t instanceof Variable) {
				this.groundings.put((Variable) t, null);
				l = l * t.getDomain().size();
			}
		}
		return (int) Math.min(l, Integer.MAX_VALUE);
	}
	
	public int size() {
		return this.size;
	}

	@Override
	public boolean hasNext() {
		return this.counter < size;
	}

	@Override
	public Atom next() {
		counter++;
		for (Variable v : groundings.keySet()) {
			groundings.put(v, v.getRandomConstant());
		}
		return filter.ground(groundings);
	}

	@Override
	public void remove() {
		// do nothing
	}
	
	public static int groundingCount(Atom filter) {
		long l = 1;
		for (Term t : filter.terms) {
			if (t instanceof Variable) {
				l = l * t.getDomain().size();
			}
		}
		return (int) Math.min(l, Integer.MAX_VALUE);
	}
	
	public static double groundingCount(Atom filter, boolean value, Database db) {
		Groundings atoms = new Groundings(filter);
		int total = atoms.size();
		
		SequentialTester tester = new SequentialConvergenceTester(.99, 0.05);
		tester.setSampleLimit(total);
		while (!tester.hasConverged()) {
			double next = db.valueOf(atoms.next()) ? 1 : 0;
			tester.increment(next);
		}
		
		double ratio = value ? tester.mean() : 1 - tester.mean();
		return ratio * total;
	}

}
