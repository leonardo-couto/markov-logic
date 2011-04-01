package structureLearner.busl;

import java.util.HashSet;
import java.util.Set;

import markovLogic.MarkovLogicNetwork;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.BronKerboschCliqueFinder;
import org.jgrapht.graph.DefaultEdge;

import stat.DefaultTest;

import structureLearner.ParallelShortestFirst;
import structureLearner.StructureLearner;

import weightLearner.WeightLearner;

import GSIMN.GSIMN;

import fol.Atom;
import fol.FormulaFactory;
import fol.Predicate;
import fol.Variable;

/**
 * Bottom-Up Structure Learner
 * @see Mihalkova and Mooney Bottom-Up Learning of Markov Logic Network Structure // TODO: completar referencia
 */
public class Busl implements StructureLearner {
	
	private final Set<Predicate> predicates;
	private final Set<Atom> tNodes;
	
	public Busl(Set<Predicate> predicates) {
		this.predicates = predicates;
		this.tNodes = new HashSet<Atom>();
	}

	@Override
	public MarkovLogicNetwork learn() {
		Set<Variable> vars = new HashSet<Variable>();
		
		// create one TNode for each predicate
		for (Predicate p : this.predicates) {
			Atom a = FormulaFactory.generateAtom(p, vars);
			vars.addAll(a.getVariables());
			tNodes.add(a);
		}
		
		MarkovLogicNetwork mln = new MarkovLogicNetwork();
		DefaultTest<Atom> test = new DefaultTest<Atom>(0.05, this.tNodes);
		GSIMN<Atom> gsimn = new GSIMN<Atom>(this.tNodes, test);
		UndirectedGraph<Atom, DefaultEdge> graph = gsimn.run();
		BronKerboschCliqueFinder<Atom, DefaultEdge> cliques = new BronKerboschCliqueFinder<Atom, DefaultEdge>(graph);
		for (Set<Atom> clique : cliques.getAllMaximalCliques()) {
			System.out.println("CLIQUE:");
			System.out.println(clique);
			ParallelShortestFirst psf = new ParallelShortestFirst(clique);
			mln.addAll(psf.learn());
		}
		mln = WeightLearner.updateWeights(mln);
		
		// TODO NAO TERMINADO
		return mln;
	}
	
	public void makeTNode(Predicate p) {
		if (this.tNodes.isEmpty()) {
			this.tNodes.add(FormulaFactory.generateAtom(p));
		}
	}
	
}
