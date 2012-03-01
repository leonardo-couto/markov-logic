package markovLogic.structureLearner.busl;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import markovLogic.MarkovLogicNetwork;
import markovLogic.WeightedFormula;
import markovLogic.WeightedFormula.FormulasAndWeights;
import markovLogic.structureLearner.StructureLearner;
import markovLogic.weightLearner.WeightLearner;
import markovLogic.weightLearner.wpll.WeightedPseudoLogLikelihood;
import math.AutomatedLBFGS;
import math.OptimizationException;
import math.Optimizer;

import org.jgrapht.Graph;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.BronKerboschCliqueFinder;
import org.jgrapht.graph.DefaultEdge;

import stat.DefaultTest;
import stat.convergence.SequentialConvergenceTester;
import util.MyException;
import util.Util;
import GSIMN.GSIMN;
import GSIMN.GSITest;
import fol.Atom;
import fol.Formula;
import fol.FormulaFactory;
import fol.Predicate;
import fol.Variable;
import formulaLearner.ParallelLearner;
import formulaLearner.ParallelLearnerBuilder;
import formulaLearner.ScoredLearner;
import formulaLearner.ScoredLearnerBuilder;
import formulaLearner.TestFormula;

/**
 * Bottom-Up Structure Learner
 * @see Mihalkova and Mooney Bottom-Up Learning of Markov Logic Network Structure // TODO: completar referencia
 */
public class Busl implements StructureLearner {
	
	public static PrintStream out = System.out;
	
	private final Set<Predicate> predicates;
	private final Set<Atom> tNodes;
	private final MarkovLogicNetwork mln;
	private WeightLearner wl;
	
	public Busl(Set<Predicate> predicates) {
		this.predicates = predicates;
		this.tNodes = new HashSet<Atom>();
		this.mln = new MarkovLogicNetwork();
		WeightedPseudoLogLikelihood score = new WeightedPseudoLogLikelihood(predicates, 50000);
		score.setTester(new SequentialConvergenceTester(0.99, 0.01));
		this.wl = new WeightLearner(score, new AutomatedLBFGS(0.001));
		GSIMN.out = Util.dummyOutput;
		GSITest.out = Util.dummyOutput;
		ParallelLearner.out = Util.dummyOutput;
		TestFormula.out = Util.dummyOutput;
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
			{
				Set<Predicate> predicates = Atom.getPredicates(clique);
				WeightedPseudoLogLikelihood exactScore = new WeightedPseudoLogLikelihood(predicates, 50000);
				exactScore.setTester(new SequentialConvergenceTester(0.99, 0.01));
				Optimizer preciseOptimizer = new AutomatedLBFGS(0.001);
				builder.setWeightLearner(new WeightLearner(exactScore, preciseOptimizer));
			}
			
			out.println();
			out.println("CLIQUE: " + clique);
			out.println();
			
			ScoredLearner formulaLearner = builder.setAtoms(clique).build();
			List<Formula> formulas = formulaLearner.learn();
			
			this.updateMln(formulas, formulaLearner.getWeightLearner().weights());
		}
		
		Set<Atom> candidates = new HashSet<Atom>();

		// TODO: GRAVAR OS CLIQUES QUE JAH FORAM TESTADOS, INDEPENDENTE DAS VARIAVEIS DE MODO QUE
		// AMIGOS(X,Y), CANCER(Y)  ==  AMIGOS(Z,W), CANCER(W)  !=  AMIGOS(X,Y), CANCER(X)
		while(true) {
			
			for (Predicate p : this.predicates) {
				Set<Atom> nodes = FormulaFactory.generateAtoms(p, vars);
				candidates.addAll(nodes);
				candidates.removeAll(this.tNodes);
			}

			double mlnScore = this.wl.score();

			Queue<TNode> queue = new PriorityQueue<TNode>(candidates.size(), TNode.COMPARATOR);
			for (Atom node : candidates) {
				builder.setTarget(node);
				List<WeightedFormula> list = new ArrayList<WeightedFormula>();
				List<Formula> learnedFormulas = new LinkedList<Formula>();
				gsimn.addVariable(node);
				out.println("***************************************************************************************************************************");
				out.println("***************************************************************************************************************************");
				Graph<Atom, DefaultEdge> cgraph = gsimn.getGraph();
				Graph<Atom, DefaultEdge> neighbors = Util.neighborsGraph(cgraph, node);
				cliques = new BronKerboschCliqueFinder<Atom, DefaultEdge>(neighbors);
				for (Set<Atom> clique : cliques.getAllMaximalCliques()) {
					{
						Set<Predicate> predicates = Atom.getPredicates(clique);
						WeightedPseudoLogLikelihood exactScore = new WeightedPseudoLogLikelihood(predicates, 50000);
						exactScore.setTester(new SequentialConvergenceTester(0.99, 0.01));
						Optimizer preciseOptimizer = new AutomatedLBFGS(0.001);
						builder.setWeightLearner(new WeightLearner(exactScore, preciseOptimizer));
					}

					out.println();
					out.println("CLIQUE: " + clique);
					out.println();

					ScoredLearner formulaLearner = builder.setAtoms(clique).build();
					List<Formula> formulas = formulaLearner.learn();
					double[] weights = formulaLearner.getWeightLearner().weights();
					FormulasAndWeights fws = this.removeDuplicates(formulas, weights, true);
					learnedFormulas.addAll(fws.formulas);
					list.addAll(WeightedFormula.toWeightedFormulas(fws.formulas, fws.weights));

				}

				if (!learnedFormulas.isEmpty()) {
					WeightLearner wl = this.wl.copy();
					wl.addFormulas(learnedFormulas);
					try {
						List<WeightedFormula> cmln = new ArrayList<WeightedFormula>(this.mln);
						cmln.addAll(list);
						wl.learn(WeightedFormula.toFormulasAndWeights(cmln).weights);
					} catch (OptimizationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					double deltaScore = wl.score() - mlnScore;
					queue.add(new TNode(node, list, wl, wl.score()));
					out.println("score improvement: " + deltaScore);
				}

				gsimn.removeVariable(node);
			}
			
			if (queue.isEmpty() || Double.compare(queue.peek().score-mlnScore, 0.002) < 1) {
				break;
			} else {
				out.println("***************************************************************************************************************************");
				out.println("melhor score: " + queue.peek().node + " - " + queue.peek().score);
				out.println("formulas: " + queue.peek().formulas);
				out.println("***************************************************************************************************************************");
				this.wl = queue.peek().learner;
				this.mln.addAll(queue.peek().formulas);
				gsimn.addVariable(queue.peek().node);
				this.tNodes.add(queue.peek().node);
			}
		
		}
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

		FormulasAndWeights fw = this.removeDuplicates(formulas, weights, false);
		
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
	 * @param atoms if true remove all atoms from formulas
	 * @return 
	 */
	private FormulasAndWeights removeDuplicates(List<Formula> formulas, double[] weights, boolean atoms) {
		// remove formulas already in the mln
		
		if (atoms) {
			ListIterator<Formula> it = formulas.listIterator();
			int j = 0;
			while (it.hasNext()) {
				if (it.next() instanceof Atom) {
					it.set(null);
					weights[j] = Double.NaN;
				}
				j++;
			}
		}
		
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
	
	private static class TNode {
		public final Atom node;
		public final List<WeightedFormula> formulas;
		public final WeightLearner learner;
		public final double score;
		
		public TNode(Atom node, List<WeightedFormula> formulas, WeightLearner learner, double score) {
			this.node = node;
			this.formulas = formulas;
			this.learner = learner;
			this.score = score;
		}
		
		public static final Comparator<TNode> COMPARATOR = new Comparator<TNode>() {

			@Override
			public int compare(TNode o1, TNode o2) {
				return -Double.compare(o1.score, o2.score);
			}
		};
	}
	
}
