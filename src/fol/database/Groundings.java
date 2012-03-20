package fol.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import stat.convergence.SequentialConvergenceTester;
import stat.convergence.SequentialTester;
import stat.sampling.CrossJoinSampler;
import stat.sampling.Sampler;
import fol.Atom;
import fol.Constant;
import fol.Formula;
import fol.FormulaFactory;
import fol.Predicate;
import fol.Term;
import fol.Variable;

/**
 * <p>This class is a Iterator for groundings of a given Formula.</p>
 * <p>The groundings order are random and if <code>exact</code> is false
 * it is not guaranteed that all of them will be sampled. Unless 
 * <code>exact</code> is true it is also possible that some atoms will be 
 * sampled more than once.</p>
 */
public class Groundings<T extends Formula> implements Iterator<T> {
	
	private static final int EXACT_THRESHOLD = 500;
	
	private final boolean exact;
	private final T filter;
	private final Map<Variable, Constant> groundings;
	private final int size;
	private final List<Variable> variables;
	private final Iterator<List<Constant>> sampler;
	
	private int counter;
	
	public Groundings(T filter) {
		this(filter, false);
	}
	
	public Groundings(T filter, boolean exact) {
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
		for (Atom atom : this.filter.getAtoms()) {
			for (Term t : atom.terms) {
				if (t instanceof Variable && !this.groundings.containsKey(t)) {
					this.groundings.put((Variable) t, null);
					l = l * t.getDomain().size();
				}
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
	@SuppressWarnings("unchecked")
	public T next() {
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
		return (T) filter.ground(groundings);
	}

	@Override
	public void remove() {
		// do nothing
	}
	
	public static Groundings<Atom> iterator(Predicate predicate) {
		return iterator(predicate, false);
	}
	
	public static Groundings<Atom> iterator(Predicate predicate, boolean exact) {
		Atom filter = FormulaFactory.generateAtom(predicate);
		return new Groundings<Atom>(filter, exact);
	}
	
	public static int count(Formula filter) {
		long l = 1;
		Set<Variable> variables = new HashSet<Variable>();
		for (Atom atom : filter.getAtoms()) {
			for (Term t : atom.terms) {
				if (t instanceof Variable && !variables.contains(t)) {
					l = l * t.getDomain().size();
					variables.add((Variable) t);
				}
			}
		}
		return (int) Math.min(l, Integer.MAX_VALUE);
	}
	
	public static double count(Formula filter, boolean value, Database db) {
		Groundings<Formula> formulas = new Groundings<Formula>(filter);
		int total = formulas.size();
		
		SequentialTester tester = new SequentialConvergenceTester(.99, 0.05);
		tester.setSampleLimit(total);
		while (!tester.hasConverged()) {
			double next = formulas.next().getValue(db) ? 1 : 0;
			tester.increment(next);
		}
		
		double ratio = value ? tester.mean() : 1 - tester.mean();
		return ratio * total;
	}

}
