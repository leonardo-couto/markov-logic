package formulaLearner;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import markovLogic.weightLearner.WeightLearner;
import markovLogic.weightLearner.wpll.WeightedPseudoLogLikelihood;
import math.AutomatedLBFGS;
import stat.convergence.SequentialConvergenceTester;
import stat.convergence.SequentialTester;
import util.MyException;
import fol.Atom;
import fol.Formula;
import fol.Predicate;

public class ParallelLearnerBuilder implements ScoredLearnerBuilder {
	
	private Set<Atom> atoms;
	private Atom target;
	private List<Formula> formulas;
	private WeightLearner fastLearner;
	private WeightLearner preciseLearner;
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
		this.fastLearner = null;
		this.preciseLearner = null;
		this.maxAtoms = 0;
		this.threads = 0;
		this.epslon = 0;
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
		
		ParallelLearner pl;
		if (this.fastLearner == null) {
			Set<Predicate> predicates = Atom.getPredicates(this.atoms);
			WeightedPseudoLogLikelihood fastScore = new WeightedPseudoLogLikelihood(predicates, 300);
			SequentialTester tester = new SequentialConvergenceTester(0.95, 0.05);
			tester.setSampleLimit(500);
			fastScore.setTester(tester);
			this.fastLearner = new WeightLearner(fastScore, new AutomatedLBFGS(0.02));
			pl = new ParallelLearner(this);
			this.fastLearner = null;
		} else {
			pl = new ParallelLearner(this);
		}
		
		return pl;
	}
	
	@Override
	public ParallelLearnerBuilder setAtoms(Collection<Atom> atoms) {
		this.atoms = new HashSet<Atom>(atoms);
		return this;
	}
	
	@Override
	public Set<Atom> getAtoms() {
		return this.atoms;
	}
	
	@Override
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
	
	public ParallelLearnerBuilder setWeightLearner(WeightLearner wLearner) {
		this.preciseLearner = wLearner;
		return this;
	}
	
	public WeightLearner getWeightLearner() {
		return this.preciseLearner;
	}
	
	public ParallelLearnerBuilder setFastLearner(WeightLearner fastLearner) {
		this.fastLearner = fastLearner;
		return this;
	}
	
	public WeightLearner getFastLearner() {
		return this.fastLearner;
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
