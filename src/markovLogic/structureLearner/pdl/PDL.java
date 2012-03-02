package markovLogic.structureLearner.pdl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
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
import fol.WeightedFormula.AbsoluteWeightComparator;
import fol.database.Database;

public class PDL implements StructureLearner {
	
	// PARAMETERS !!!!!
	private static final int MAX_VARS = 3;
	private static final int BEAM_SIZE = 50;
	private static final double EPSLON = 10e-8; // min absolute weight
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
	private final Comparator<WeightedFormula<?>> comparator;
	private final CountCache cache;
	
	private final CountsGenerator preciseCounter;
	private final CountsGenerator fastCounter;
	
	private final WeightLearner fastLearner;
	private final WeightLearner preciseLearner;
	private WeightLearner l1Learner;
	
	private final L1RegularizedScore l1Score;
	
	public PDL(Set<Predicate> predicates, Database db) {
		this.factory = new FormulaFactory(predicates, MAX_VARS);
		this.atoms = this.factory.getUnitClauses();
		this.cache = new CountCache(db);
		
		Score fastScore = new WeightedPseudoLogLikelihood(predicates, this.cache, LOW_SAMPLE_SIZE);
		Score preciseScore = new WeightedPseudoLogLikelihood(predicates, this.cache, HIGH_SAMPLE_SIZE);
		L1RegularizedScore l1Score = new L1RegularizedScore(fastScore).setConstantWeight(L1_WEIGHT);
		
		Optimizer fastOptimizer = new AutomatedLBFGS(LOW_LBFGS_PRECISION);
		Optimizer preciseOptimizer = new AutomatedLBFGS(HIGH_LBFGS_PRECISION);
		
		this.l1Score = l1Score;
		this.fastLearner = new WeightLearner(fastScore, fastOptimizer); 
		this.l1Learner = new WeightLearner(l1Score, fastOptimizer);
		this.preciseLearner = new WeightLearner(preciseScore, preciseOptimizer);
		this.l1Learner = this.fastLearner; // TODO: REMOVER, MODIFICACAO PARA TESTE
		
		this.comparator = new AbsoluteWeightComparator();
		
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
			
			
			if (clauses.isEmpty()) break;
			candidates = clauses;
		}
		
		// TODO: TIRAR AS QUE TEM PESO ZERO (no L1 score) ANTES DE ADICIONAR NA MLN
		// menos os atomos
		
		double[] weights = this.preciseLearner.weights();
		List<Formula> formulas = this.preciseLearner.getFormulas();
		MarkovLogicNetwork mln = new MarkovLogicNetwork();
		mln.addAll(WeightedFormula.toWeightedFormulas(formulas, weights));
		
		return mln;
	}
	
	private List<Clause> batchLearn(List<Clause> candidates) {
//		System.out.println("COMECANDO AS THREADS!!");
//		this.fastCounter.count(candidates);		
		
		try {
			List<WeightedFormula<Clause>> wFormulas;
			double[] initialWeights = this.l1Learner.weights();
			int initialSize = initialWeights.length;
			this.l1Score.setStart(initialSize);
			double[] weights = initialWeights;
			int counter = 0;
			
			this.l1Learner.addFormulas(candidates);
			{
				boolean improved = true;
				double score, min, lastScore;
				lastScore = this.l1Learner.score();

				while (improved) {
					counter++;
					weights = this.l1Learner.learn(weights);
					score = this.l1Learner.score();
					min = (lastScore + Math.max(-lastScore*MIN_IMPROVEMENT/20, MIN_ABSOLUTE_IMPROVEMENT/10));
					lastScore = score;
					improved = score > min;
					System.out.println(counter + ": " + score);
				}
			}
			
			
//			counter = 0;
//			int quantity = weights.length;
//			System.out.println("TOTAL FORMULAS = " + quantity);
//			boolean stop = false;
//			
//			System.out.println("PESO: 0");
//			Comparator<WeightedFormula<Formula>> mycomparator = new WeightedFormula.AbsoluteWeightComparator<Formula>(true);
//			List<WeightedFormula<Formula>> fff = WeightedFormula.toWeightedFormulas(this.l1Learner.getFormulas(), weights);
//			Collections.sort(fff, mycomparator);
//			this.printBatchLearner(fff);
//			double l1w = -1;
//			
//			do {
//				while (this.removeClauses(this.l1Learner, EPSLON, initialSize) > 0) {
//					weights = this.l1Learner.weights();
//					counter++;
//					System.out.println(counter + ": " + (quantity - weights.length));
//				}
//				
//				if (weights.length > BEAM_SIZE) {
//					int diff = weights.length - BEAM_SIZE;
//					double increase = diff > 500 ? -1.0 : diff > 100 ? -0.5 : -0.1;
//					this.l1Score.setConstantWeight(this.l1Score.getWeight() + increase);
//					System.out.println("PESO: " + this.l1Score.getWeight());
//					if (this.l1Score.getWeight() < l1w) {
//						l1w = l1w -1;
//						fff = WeightedFormula.toWeightedFormulas(this.l1Learner.getFormulas(), weights);
//						Collections.sort(fff, mycomparator);
//						this.printBatchLearner(fff);
//					}
//					weights = this.l1Learner.learn(weights);
//				} else {
//					stop = true;
//				}
//
//			} while (!stop);
//			this.l1Score.setConstantWeight(L1_WEIGHT);
//			
//			System.out.println(String.format("SOBRARAM %s FORMULAS!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!", weights.length));
//		
//			
//			int finalLength = weights.length;
//			weights = Arrays.copyOfRange(weights, initialSize, finalLength);
			
			int finalLength = Math.min(weights.length, initialSize + BEAM_SIZE);
			weights = Arrays.copyOfRange(weights, initialSize, finalLength);
			
			List<Formula> sublist = this.l1Learner.getFormulas().subList(initialSize, finalLength);
			List<Clause> formulas = new ArrayList<Clause>(sublist.size());
			for (Formula f : sublist) {	formulas.add((Clause) f); }

			wFormulas = WeightedFormula.toWeightedFormulas(formulas, weights);
			Collections.sort(wFormulas, this.comparator);
			this.printBatchLearner(wFormulas);
			
			// clean weightLearner
			ListIterator<Clause> iterator = candidates.listIterator(candidates.size());
			while(iterator.hasPrevious()) {
				this.l1Learner.removeFormula(iterator.previous());
			}
			this.l1Learner.learn(initialWeights);
			
			return WeightedFormula.toFormulasAndWeights(wFormulas).formulas;
		} catch (OptimizationException e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}
	
	private <T extends Formula> void printBatchLearner(List<WeightedFormula<T>> wFormulas) {
		System.out.println("************* IMPRIMINDO FORMULAS BATHLEARNER **************");
		for (WeightedFormula<T> wf : wFormulas) {
			System.out.println(wf);
		}
		System.out.println("************* END IMPRIMINDO FORMULAS BATHLEARNER **************");			
	}
	
//	private int removeClauses(WeightLearner learner, double weightThreeshold, int from) throws OptimizationException {
//		List<Formula> formulas = new ArrayList<Formula>(learner.getFormulas());
//		double[] weights = learner.weights();
//		double[] newWeights = new double[weights.length];
//		int remove = 0;
//		int j = 0;
//		for (int i = 0; i < from; i++) {
//			newWeights[i] = weights[i];
//			j++;
//		}
//		for (int i = from; i < weights.length; i++) {
//			if (Math.abs(weights[i]) < weightThreeshold) {
//				remove++;
//				learner.removeFormula(formulas.get(i));
//			} else {
//				newWeights[j] = weights[i];
//				j++;
//			}
//		}
//		
//		if (remove > 0) {
//			weights = Arrays.copyOf(newWeights, weights.length-remove);
//			weights = learner.learn(weights);
//		}
//		
//		return remove;
//	}
	
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
