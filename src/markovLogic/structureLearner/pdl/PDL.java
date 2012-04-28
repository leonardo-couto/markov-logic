package markovLogic.structureLearner.pdl;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import markovLogic.MarkovLogicNetwork;
import markovLogic.structureLearner.StructureLearner;
import markovLogic.weightLearner.L1RegularizedScore;
import markovLogic.weightLearner.Score;
import markovLogic.weightLearner.WeightLearner;
import markovLogic.weightLearner.wpll.CountCache;
import markovLogic.weightLearner.wpll.CountsGenerator;
import markovLogic.weightLearner.wpll.WeightedPseudoLogLikelihood;
import math.AutomatedLBFGS;
import math.OptimizationException;
import math.Optimizer;
import fol.Clause;
import fol.Formula;
import fol.FormulaFactory;
import fol.Predicate;
import fol.WeightedFormula;
import fol.database.BinaryDB;
import fol.database.RealDB;

public class PDL implements StructureLearner {
	
	// PARAMETERS !!!!!
	private static final int MAX_VARS = 3;
	private static final int BEAM_SIZE = 50;
//	private static final double EPSLON = 1e-8; // min absolute weight
	private static final double MIN_IMPROVEMENT = 0.02; // percent value of min score improvement
	private static final double MIN_ABSOLUTE_IMPROVEMENT = 0.005;
	private static final int MAX_LITERALS = 5;
	private static final int LOW_SAMPLE_SIZE = 250;
	private static final int HIGH_SAMPLE_SIZE = 1000;
	private static final double LOW_LBFGS_PRECISION = 0.01;
	private static final double HIGH_LBFGS_PRECISION = 0.0005;
	private static final int THREADS = Runtime.getRuntime().availableProcessors();
	private static final double L1_WEIGHT = -0.1;
	
	private final FormulaFactory factory;
	private final List<Clause> atoms;
	private final CountCache cache;
	
	private final CountsGenerator preciseCounter;
	private final CountsGenerator fastCounter;
	
	private final WeightLearner fastLearner;
	private final WeightLearner preciseLearner;
	private WeightLearner l1Learner;
	
	public PDL(Set<Predicate> predicates, BinaryDB db) {
		this(predicates, db, null);
	}
	
	public PDL(Set<Predicate> predicates, RealDB db) {
		this(predicates, null, db);
	}
	
	private PDL(Set<Predicate> predicates, BinaryDB bin, RealDB real) {
		this.factory = new FormulaFactory(predicates, MAX_VARS);
		this.atoms = this.factory.getUnitClauses();
		this.cache = (bin == null) ? new CountCache(real) : new CountCache(bin);
		
		Score fastScore = new WeightedPseudoLogLikelihood(predicates, this.cache, LOW_SAMPLE_SIZE);
		Score preciseScore = new WeightedPseudoLogLikelihood(predicates, this.cache, HIGH_SAMPLE_SIZE);
		L1RegularizedScore l1Score = new L1RegularizedScore(fastScore).setConstantWeight(L1_WEIGHT);
		
		Optimizer fastOptimizer = new AutomatedLBFGS(LOW_LBFGS_PRECISION);
		Optimizer preciseOptimizer = new AutomatedLBFGS(HIGH_LBFGS_PRECISION);
		
		this.fastLearner = new WeightLearner(fastScore, fastOptimizer); 
		this.l1Learner = new WeightLearner(l1Score, fastOptimizer);
		this.preciseLearner = new WeightLearner(preciseScore, preciseOptimizer);
		this.l1Learner = this.fastLearner; // TODO: REMOVER, MODIFICACAO PARA TESTE
		
		this.preciseCounter = new CountsGenerator(this.cache, HIGH_SAMPLE_SIZE, THREADS);
		this.fastCounter = new CountsGenerator(this.cache, LOW_SAMPLE_SIZE, THREADS);
	}

	@Override
	public MarkovLogicNetwork learn() {
		ClauseFilter filter = new ClauseFilter(this.fastLearner, this.factory, this.fastCounter);
		
		this.preciseCounter.count(this.atoms);
	
		// add unit clauses, learn weights and gets the score
		this.l1Learner.addFormulas(this.atoms);
		this.preciseLearner.addFormulas(this.atoms);
		try {
			this.l1Learner.learn(new double[this.atoms.size()]);
			this.preciseLearner.learn(this.l1Learner.weights());
		} catch (Exception e) {
			System.err.println("Could not learn atoms weight.");
			e.printStackTrace();
			System.exit(1);			
		}
		double score = this.preciseLearner.score();
		List<Clause> candidates = this.atoms;
		
		for (int i = 1; i < MAX_LITERALS; i++) {
			candidates = this.factory.generatePositiveClauses(candidates);
			System.out.println("FILTRANDO CANDIDATOS");
			candidates = filter.filter(candidates);
			System.out.println("IMPRIMINDO CANDIDATOS");
			for (Formula f : candidates) {
				System.out.println(f);
			}			
			System.out.println("ADICIONANDO CANDIDATOS");

//			List<ConjunctiveNormalForm> clauses = this.batchLearn(candidates);
			List<Clause> clauses = candidates.subList(0, Math.min(BEAM_SIZE, candidates.size()));
			this.preciseCounter.count(clauses);
			
			for (Clause cnf : clauses) {
				if (this.addClause(cnf, score)) {
					double[] weights = this.preciseLearner.weights();
					score = this.preciseLearner.score();
					System.out.println(String.format("%s;%s; %s", score, weights[weights.length-1], cnf));
				}
			}
			
			this.cache.clear();
			
			if (clauses.isEmpty()) break;
			candidates = clauses;
		}
		
		// TODO: TIRAR AS QUE TEM PESO ZERO (no L1 score) ANTES DE ADICIONAR NA MLN
		// menos os atomos
		
		double[] weights = this.preciseLearner.weights();
		List<Formula> formulas = this.preciseLearner.getFormulas();
		List<WeightedFormula<Formula>> wfs = WeightedFormula.toWeightedFormulas(formulas, weights);
		MarkovLogicNetwork mln = new MarkovLogicNetwork(wfs);
		
		return mln;
	}
	
	private boolean addClause(Formula formula, double lastScore) {
		try {
			double[] weights = this.preciseLearner.weights();
			int length = weights.length;
			weights = Arrays.copyOf(weights, length+1);
				
			this.preciseLearner.addFormula(formula);
			weights = this.preciseLearner.learn(weights);
			double score = this.preciseLearner.score();
			double min = Math.max(-MIN_IMPROVEMENT*lastScore, MIN_ABSOLUTE_IMPROVEMENT);
			if (Double.compare(score-lastScore, min) > -1) {
				
				{
					boolean improved = true;
					double iscore, imin, ilastScore;
					int counter = 0;
					ilastScore = this.preciseLearner.score();

					while (improved) {
						counter++;
						weights = this.preciseLearner.learn(weights);
						iscore = this.preciseLearner.score();
						imin = (ilastScore + Math.max(-lastScore*MIN_IMPROVEMENT/20, MIN_ABSOLUTE_IMPROVEMENT/10));
						ilastScore = iscore;
						improved = iscore > imin;
						System.out.println(counter + ": " + iscore);
					}
				}
				
				
				double[] initialWeights = this.l1Learner.weights();
				initialWeights = Arrays.copyOf(initialWeights, length+1);
				initialWeights[length] = weights[length];
				this.l1Learner.addFormula(formula);
				this.l1Learner.learn(initialWeights);
				return true;				
			} else {
				this.preciseLearner.removeFormula(formula);
				weights = Arrays.copyOf(weights, length);
				this.preciseLearner.learn(weights);
				return false;
			}
			
		} catch (OptimizationException e) {
			e.printStackTrace();
		}
		return false;
	}
	
}
