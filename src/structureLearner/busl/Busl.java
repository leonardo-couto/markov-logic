package structureLearner.busl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import markovLogic.MarkovLogicNetwork;
import markovLogic.WeightedFormula;
import markovLogic.WeightedFormula.FormulasAndWeights;
import math.AutomatedLBFGS;
import math.OptimizationException;
import math.Optimizer;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.BronKerboschCliqueFinder;
import org.jgrapht.graph.DefaultEdge;

import stat.DefaultTest;
import structureLearner.StructureLearner;
import util.MyException;
import weightLearner.Score;
import weightLearner.WeightLearner;
import weightLearner.wpll.WeightedPseudoLogLikelihood;
import GSIMN.GSIMN;
import fol.Atom;
import fol.Formula;
import fol.FormulaFactory;
import fol.Predicate;
import fol.Variable;
import formulaLearner.ParallelLearnerBuilder;
import formulaLearner.ScoredLearner;
import formulaLearner.ScoredLearnerBuilder;

/**
 * Bottom-Up Structure Learner
 * @see Mihalkova and Mooney Bottom-Up Learning of Markov Logic Network Structure // TODO: completar referencia
 */
public class Busl implements StructureLearner {
	
	private final Set<Predicate> predicates;
	private final Set<Atom> tNodes;
	private final MarkovLogicNetwork mln;
	private final WeightLearner wl;
	
	public Busl(Set<Predicate> predicates) {
		this.predicates = predicates;
		this.tNodes = new HashSet<Atom>();
		this.mln = new MarkovLogicNetwork();
		this.wl = new WeightLearner(
				new WeightedPseudoLogLikelihood(this.predicates), 
				new AutomatedLBFGS(0.001));
	}

	@Override
	public MarkovLogicNetwork learn() {
		Set<Variable> vars = new HashSet<Variable>();
		
		// create one TNode for each predicate
		for (Predicate p : this.predicates) {
			vars.addAll(this.makeTNode(p));
		}
		{
			List<Formula> formulas = new ArrayList<Formula>(this.tNodes);
		    this.updateMln(formulas, new double[formulas.size()]);
		}
		
		DefaultTest<Atom> test = new DefaultTest<Atom>(0.05, this.tNodes);
		GSIMN<Atom> gsimn = new GSIMN<Atom>(this.tNodes, test);
		UndirectedGraph<Atom, DefaultEdge> graph = gsimn.getGraph();
		BronKerboschCliqueFinder<Atom, DefaultEdge> cliques = new BronKerboschCliqueFinder<Atom, DefaultEdge>(graph);
		ScoredLearnerBuilder builder;
		{
			builder = new ParallelLearnerBuilder().setEpslon(0.5).setMaxAtoms(5).
			setNumberOfThreads(Runtime.getRuntime().availableProcessors());			
		}
		
		for (Set<Atom> clique : cliques.getAllMaximalCliques()) {
			Set<Predicate> predicates = Atom.getPredicates(clique);
			{
				Score exactScore = new WeightedPseudoLogLikelihood(predicates);
				Optimizer preciseOptimizer = new AutomatedLBFGS(0.001);
				builder.setWeightLearner(new WeightLearner(exactScore, preciseOptimizer));
			}
			
			System.out.println();
			System.out.println("CLIQUE: " + clique);
			System.out.println();
			
			ScoredLearner formulaLearner = builder.setAtoms(clique).build();
			List<Formula> formulas = formulaLearner.learn();
			
			this.updateMln(formulas, formulaLearner.getWeightLearner().weights());
		}
		
		List<Atom> candidates = new LinkedList<Atom>();
		for (Predicate p : this.predicates) {
			Set<Atom> nodes = FormulaFactory.generateAtoms(p, vars);
			nodes.removeAll(this.tNodes);
			candidates.addAll(nodes);
		}
		
		double mlnScore = this.wl.score();
		
		for (Atom candidate : candidates) {
			
		}
		
		// TODO NAO TERMINADO
		return mln;
	}
	
	private Set<Variable> makeTNode(Predicate p) {
		Atom atom = FormulaFactory.generateAtom(p);
		this.tNodes.add(atom);
		return atom.getVariables();
	}
	
	/**
	 * Add formulas to the mln and updates the MLN weights to 
	 * values that maximize the network score.
	 * @param formulas Formulas to be added to the mln
	 * @param weights added Formulas weights
	 * @return mln's score
	 */
	private double updateMln(List<Formula> formulas, double[] weights) {

		FormulasAndWeights fw = this.removeDuplicates(formulas, weights);
		
		// add the formulas to mln and mln's associated weightLearner
		this.mln.addAll(WeightedFormula.toWeightedFormulas(fw.formulas, fw.weights));
		this.wl.addFormulas(fw.formulas);
		
		// learn the optimum weights
		fw = WeightedFormula.toFormulasAndWeights(this.mln);
		try {
			weights = this.wl.learn(fw.weights);
		} catch (OptimizationException e) {
			throw new MyException(
					"Fatal error while learning the MLN weights.", e);
		}
		
		// update the MLN with optimum weights
		this.mln.clear();
		this.mln.addAll(WeightedFormula.toWeightedFormulas(fw.formulas, weights));
		
		return this.wl.score();
	}
	
	/**
	 * Remove formulas already in the MLN.
	 * @param formulas Formulas to be added to the mln
	 * @param weights added Formulas weights
	 * @return 
	 */
	private FormulasAndWeights removeDuplicates(List<Formula> formulas, double[] weights) {
		// remove formulas already in the mln
		for (WeightedFormula wf : this.mln) {
			Formula f = wf.getFormula();
			ListIterator<Formula> it = formulas.listIterator();
			int j = 0;
			while (it.hasNext()) {
				if (f == it.next()) {
					it.set(null);
					weights[j] = Double.NaN;
				}
				j++;
			}
		}
		
		ListIterator<Formula> it = formulas.listIterator();
		formulas = new ArrayList<Formula>(formulas.size());
		while (it.hasNext()) {
			Formula f = it.next();
			if (f != null) {
				formulas.add(f);
			}
		}

		// remove the weights of removed formulas
		double[] nweights = new double[formulas.size()];
		{
			int j = 0;
			for (double d : weights) {
				if (!Double.isNaN(d)) {
					nweights[j] = d;
					j++;
				}
			}
		}
		
		return new FormulasAndWeights(formulas, nweights);
	}
	
}
