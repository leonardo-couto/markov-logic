package formulaLearner;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import stat.convergence.SequentialConvergenceTester;
import stat.convergence.SequentialTester;
import util.MyException;
import weightLearner.Score;
import weightLearner.wpll.WeightedPseudoLogLikelihood;
import fol.Atom;
import fol.Formula;
import fol.Predicate;
import math.AutomatedLBFGS;
import math.Optimizer;

public class ParallelLearnerBuilder implements FormulaLearnerBuilder {
	
	private Set<Atom> atoms;
	private Atom target;
	private List<Formula> formulas;
	private Score exactScore;
	private Score fastScore;
	private Optimizer fastOptimizer;
	private Optimizer preciseOptimizer;
	private int maxAtoms;
	private int threads;
	private double epslon;
	private boolean hasTarget;
	private double[] initialArgs;
	private double initialScore;
	
	public ParallelLearnerBuilder() {
		this.atoms = null;
		this.target = null;
		this.formulas = null;
		this.hasTarget = false;
		this.initialArgs = null;
		this.initialScore = Double.NaN;
	}
	
	/**
	 * Builds a ParallelLearner.
	 * <br>
	 * The field <code>atoms</code> is required to be set before calling this 
	 * method, not setting it will cause this method to throw an exception.
	 * <br>
	 * Most other fields are required by ParallelLearn, but have a
	 * default value defined.
	 * <code>target</code>, <code>formulas</code>, <code>initialArgs</code> and
	 * <code>initialScore</code> are optional values that do not have a default.
	 * @return
	 */
	@Override
	public ParallelLearner build() {
		if (this.atoms == null || this.atoms.isEmpty()) {
			throw new MyException("Cannot build a FormulaLearner. Atoms not set.");
		}
		return new ParallelLearner(this);
	}
	
	@Override
	public ParallelLearnerBuilder setAtoms(Collection<Atom> atoms) {
		this.atoms = new HashSet<Atom>(atoms);
		Set<Predicate> predicates = new HashSet<Predicate>();
		for (Atom a : atoms) { predicates.add(a.predicate); }
		WeightedPseudoLogLikelihood exactScore = new WeightedPseudoLogLikelihood(predicates);
		WeightedPseudoLogLikelihood fastScore = new WeightedPseudoLogLikelihood(predicates);
		SequentialTester tester = new SequentialConvergenceTester(0.95, 0.05);
		tester.setSampleLimit(500);
		fastScore.setSampleLimit(300);
		fastScore.setTester(tester);
		this.exactScore = exactScore;
		this.fastScore = fastScore;
		this.preciseOptimizer = new AutomatedLBFGS(0.001);
		this.fastOptimizer = new AutomatedLBFGS(0.02);
		return this;
	}
	
	@Override
	public Set<Atom> getAtoms() {
		return this.atoms;
	}
	
	public ParallelLearnerBuilder setTarget(Atom a) {
		this.target = a;
		this.hasTarget = true;
		return this;
	}
	
	public Atom getTarget() {
		return this.target;
	}
	
	public boolean hasTarget() {
		return this.hasTarget;
	}
	
	/**
	 * The score instance must contain no formulas.
	 * @param exactScore
	 * @return
	 */
	public ParallelLearnerBuilder setExactScore(Score exactScore) {
		if (!exactScore.getFormulas().isEmpty()) {
			throw new MyException("Score is not empty");
		}
		this.exactScore = exactScore;
		return this;
	}
	
	/**
	 * The score instance must be empty
	 * @param exactScore
	 * @return
	 */
	public Score getExactScore() {
		return this.exactScore;
	}
	
	public ParallelLearnerBuilder setFastScore(Score fastScore) {
		if (!fastScore.getFormulas().isEmpty()) {
			throw new MyException("Score is not empty");
		}
		this.fastScore = fastScore;
		return this;
	}

	public Score getFastScore() {
		return this.fastScore;
	}
	
	public ParallelLearnerBuilder setPreciseOptimizer(Optimizer optimizer) {
		this.preciseOptimizer = optimizer;
		return this;
	}

	public Optimizer getPreciseOptimizer() {
		return this.preciseOptimizer;
	}
	
	public ParallelLearnerBuilder setFastOptimizer(Optimizer optimizer) {
		this.fastOptimizer = optimizer;
		return this;
	}
	
	public Optimizer getFastOptimizer() {
		return this.fastOptimizer;
	}
	
	public ParallelLearnerBuilder setFormulas(List<Formula> formulas) {
		this.formulas = formulas;
		return this;
	}
	
	public List<Formula> getFormulas() {
		return this.formulas;
	}
	
	public ParallelLearnerBuilder setMaxAtoms(int maxAtoms) {
		this.maxAtoms = maxAtoms;
		return this;
	}

	public int getMaxAtoms() {
		return this.maxAtoms;
	}
	
	public ParallelLearnerBuilder setNumberOfThreads(int threads) {
		this.threads = threads;
		return this;
	}

	public int getNumberOfThreads() {
		return this.threads;
	}
	
	public ParallelLearnerBuilder setEpslon(double epslon) {
		this.epslon = epslon;
		return this;
	}
	
	public double getEpslon() {
		return this.epslon;
	}

	public ParallelLearnerBuilder setInitialArgs(double[] initialArgs) {
		this.initialArgs = initialArgs;
		return this;
	}

	public double[] getInitialArgs() {
		return this.initialArgs;
	}

	public ParallelLearnerBuilder setInitialScore(double initialScore) {
		this.initialScore = initialScore;
		return this;
	}

	public double getInitialScore() {
		return this.initialScore;
	}

}
