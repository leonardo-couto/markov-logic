package fol.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import stat.convergence.SequentialConvergenceTester;
import stat.convergence.SequentialTester;
import stat.sampling.CrossJoinSampler;
import stat.sampling.Sampler;
import fol.Atom;
import fol.Constant;
import fol.FormulaFactory;
import fol.Predicate;
import fol.Term;
import fol.Variable;

/**
 * <p>This class is a Iterator for groundings of a given Atom.</p>
 * <p>The groundings order are random and if <code>exact</code> is false
 * it is not guaranteed that all of them will be sampled. Unless 
 * <code>exact</code> is true it is also possible that some atoms will be 
 * sampled more than once.</p>
 */
public class Groundings implements Iterator<Atom> {
	
	private static final int EXACT_THRESHOLD = 500;
	
	private final boolean exact;
	private final Atom filter;
	private final Map<Variable, Constant> groundings;
	private final int size;
	private final List<Variable> variables;
	private final Iterator<List<Constant>> sampler;
	
	private int counter;
	
	public Groundings(Atom filter) {
		this(filter, false);
	}
	
	public Groundings(Atom filter, boolean exact) {
		this.filter = filter;
		this.groundings = new HashMap<Variable, Constant>();
		this.variables = new ArrayList<Variable>();
		this.size = this.init();
		this.counter = 0;
		this.exact = size < EXACT_THRESHOLD ? true : exact;
		this.sampler = this.exact ? this.sampler().iterator() : null;
	}
	
	private int init() {
		long l = 1;
		for (Term t : this.filter.terms) {
			if (t instanceof Variable) {
				this.groundings.put((Variable) t, null);
				l = l * t.getDomain().size();
			}
		}
		this.variables.addAll(this.groundings.keySet());
		return (int) Math.min(l, Integer.MAX_VALUE);
	}
	
	private Sampler<Constant> sampler() {
		int size = this.variables.size();
		List<List<Constant>> constants = new ArrayList<List<Constant>>(size);
		for (Variable v : this.variables) constants.add(v.getConstants());
		return new CrossJoinSampler<Constant>(constants);			
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
		if (exact) {
			List<Constant> sample = this.sampler.next();
			for (int i = 0; i < sample.size(); i++) {
				Variable v = this.variables.get(i);
				groundings.put(v, sample.get(i));
			}			
		} else {
			for (Variable v : this.variables) {
				groundings.put(v, v.getRandomConstant());
			}			
		}
		return filter.ground(groundings);
	}

	@Override
	public void remove() {
		// do nothing
	}
	
	public static Groundings iterator(Predicate predicate) {
		return iterator(predicate, false);
	}
	
	public static Groundings iterator(Predicate predicate, boolean exact) {
		Atom filter = FormulaFactory.generateAtom(predicate);
		return new Groundings(filter, exact);
	}
	
	public static int count(Atom filter) {
		long l = 1;
		for (Term t : filter.terms) {
			if (t instanceof Variable) {
				l = l * t.getDomain().size();
			}
		}
		return (int) Math.min(l, Integer.MAX_VALUE);
	}
	
	public static double count(Atom filter, boolean value, Database db) {
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
