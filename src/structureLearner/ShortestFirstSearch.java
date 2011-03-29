package structureLearner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import markovLogic.MarkovLogicNetwork;
import math.AutomatedLBFGS;
import math.LBFGS.ExceptionWithIflag;
import weightLearner.Score;
import weightLearner.WeightedPseudoLogLikelihood;
import fol.Atom;
import fol.Formula;

/**
 * @author Leonardo Castilho Couto
 *
 */
public class ShortestFirstSearch extends AbstractLearner {
	
	private List<Formula> clauses;
	private Map<Integer, List<Formula>> lengthClauses;
	private FormulaGenerator cg;
	private int k, m;
	private double epslon;
	private Score wpll;
	AutomatedLBFGS weightLearner;

	public ShortestFirstSearch(Set<Atom> atoms) {
		super(atoms);
		clauses = new ArrayList<Formula>(atoms);
		cg = new FormulaGenerator(atoms);
		lengthClauses = new HashMap<Integer, List<Formula>>();
		lengthClauses.put(new Integer(1), new ArrayList<Formula>(clauses));
		wpll = new WeightedPseudoLogLikelihood(this.predicates);
		wpll.addFormulas(clauses);
		weightLearner = new AutomatedLBFGS();
		m = 1000;
		k = 3;
		epslon = 0.1;
	}

	@Override
	public MarkovLogicNetwork learn() {
		double[] weights = new double[clauses.size()];
		Arrays.fill(weights, 0);
		try {
			weights = weightLearner.max(weights, this.wpll, this.wpll);
		} catch (ExceptionWithIflag e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		double score = wpll.getScore(weights);
		
		int i = 0;
		
		while(i < 100) {
			i++;
			System.out.println("**********" + score);
			Set<Formula> formulas = findBestClauses(score, weights);
			if (formulas.isEmpty()) {
				return MarkovLogicNetwork.toMarkovLogic(clauses, weights);
			}
			for (Formula f : formulas) {
				System.out.println(f); // TODO: remove!!
				wpll.addFormula(f);
				clauses.add(f);
			}
			try {
				weights = Arrays.copyOf(weights, clauses.size());
				weights = weightLearner.max(weights, this.wpll, this.wpll);
			} catch (ExceptionWithIflag e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			score = wpll.getScore(weights);
		}
		return MarkovLogicNetwork.toMarkovLogic(clauses, weights);
	}
	
	public Set<Formula> findBestClauses(double score, double[] weights) {
		Set<Formula> out = new HashSet<Formula>();
		List<weightedClause> bestClauses = new ArrayList<weightedClause>();
		List<weightedClause> candidates = new ArrayList<weightedClause>();
		double[] newWeights = Arrays.copyOf(weights, weights.length + 1);
		double[] aux;
		boolean stop = false;
		
		for (int i = 1; i < cg.getMaxAtoms(); i++) {
			// add all clauses of length i;
			Collection<Formula> next = lengthClauses.get(new Integer(i));
			
			// add all candidates to be expanded
			Collections.sort(candidates);
			for (int j = 0; j < Math.min(m, candidates.size()); j++) {
				next.add(candidates.get(j).clause);
			}
			
			candidates = new ArrayList<weightedClause>();
			
			next = cg.generateFormulas(next);
			// TODO: Needs to make sure no clause in here is equal clauses already in mln.
			// the above does not work because of Formula.equals.
			// next.removeAll(lengthClauses.get(new Integer(i+1)));
			
			// TODO: PARALELIZAR ESSA PARTE
			// - CRIAR UM WPLL PARA CADA THREAD
			
			int total = next.size();
			int partial = 0; // TODO: REMOVE
			
			for (Formula f : next) {
				
				partial++;
				System.out.println(partial + "/" + total); // TODO: REMOVE
				
				wpll.addFormula(f);
				double newScore = 0;
				double learnedWeight;
				try {
					aux = weightLearner.max(newWeights, this.wpll, this.wpll);
					learnedWeight = aux[aux.length -1];
					newScore = wpll.getScore(aux);
				} catch (ExceptionWithIflag e) {
					System.out.println("*tentando novamente*"); // TODO: nao funciona, mas usar isso para debugar
//					e.printStackTrace();
					try {
						double[] ad = Arrays.copyOf(newWeights, newWeights.length);
						ad[ad.length-1] = -10.0d;
						aux = weightLearner.max(ad, this.wpll, this.wpll);
						learnedWeight = aux[aux.length -1];
						newScore = wpll.getScore(aux);
					} catch (ExceptionWithIflag e1) {
						wpll.removeFormula(f);
						continue;
					}
				}
				wpll.removeFormula(f);
				System.out.println(Double.toString(score - newScore) + " " + f); // TODO: Remove
				
				if (Double.compare(newScore, score) > 0 && Double.compare(Math.abs(learnedWeight), epslon) > 0) {
					stop = true;
					bestClauses.add(new weightedClause(f, score - newScore, learnedWeight));
				} else {
					candidates.add(new weightedClause(f, score - newScore, learnedWeight));
				}
			}
			
			candidates.addAll(bestClauses);
			if (stop) {
				Collections.sort(bestClauses);
				for (int j = 0; j < Math.min(k, bestClauses.size()); j++) {
					out.add(bestClauses.get(j).clause);
					candidates.remove(bestClauses.get(j));
				}
				return out;
			}
			
		}
		return out;
	}


	private class weightedClause implements Comparable<weightedClause> {
		
		public weightedClause(Formula clause, double score, double weight) {
			this.score = score;
			this.clause = clause;
		}
		
		double score;
		Formula clause;
		
		@Override
		public int compareTo(weightedClause o) {
			return Double.compare(this.score, o.score);
		}
	
		
	}
	
	

}
