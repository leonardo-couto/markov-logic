package structureLearner.busl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import markovLogic.MarkovLogicNetwork;
import markovLogic.WeightedFormula;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.BronKerboschCliqueFinder;
import org.jgrapht.graph.DefaultEdge;

import stat.DefaultTest;
import structureLearner.StructureLearner;
import weightLearner.WeightLearner;
import GSIMN.GSIMN;
import fol.Atom;
import fol.Formula;
import fol.FormulaFactory;
import fol.Predicate;
import fol.Variable;
import formulaLearner.FormulaLearner;
import formulaLearner.FormulaLearnerBuilder;
import formulaLearner.ParallelLearnerBuilder;

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
		FormulaLearnerBuilder builder;
		{
			builder = new ParallelLearnerBuilder().setEpslon(0.5).setMaxAtoms(5).
			setNumberOfThreads(Runtime.getRuntime().availableProcessors());
			
		}
		for (Set<Atom> clique : cliques.getAllMaximalCliques()) {
			System.out.println();
			System.out.println("CLIQUE: " + clique);
			System.out.println();
			FormulaLearner formulaLearner = builder.setAtoms(clique).build();
			List<Formula> formulas = formulaLearner.learn();
			// TODO: ESTA COLOCANDO OS ATOMS QUE APARECEM EM CLIQUES DIFERENTES DUAS VEZES!!!
			// deixar mais bonito:
			for (WeightedFormula wf : mln) {
				Formula f = wf.getFormula();
				Iterator<Formula> it = formulas.iterator();
				while (it.hasNext()) {
					if (f == it.next()) {
						it.remove();
					}
				}
			}
			double[] weights = new double[formulas.size()];			
			mln.addAll(WeightedFormula.toWeightedFormulas(formulas, weights));
			mln = WeightLearner.updateWeights(mln);
		}
		
		// TODO NAO TERMINADO
		return mln;
	}
	
	public void makeTNode(Predicate p) {
		if (this.tNodes.isEmpty()) {
			this.tNodes.add(FormulaFactory.generateAtom(p));
		}
	}
	
}
