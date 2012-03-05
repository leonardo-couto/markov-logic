package markovLogic.inference;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import markovLogic.GroundedMarkovNetwork;
import markovLogic.Grounder;
import markovLogic.MarkovLogicNetwork;
import fol.Atom;
import fol.CNF;
import fol.Clause;
import fol.Formula;
import fol.WeightedFormula;
import fol.database.Database;
import fol.database.SimpleDB;
import fol.operator.Negation;
import fol.sat.WalkSAT;

public class MCSAT implements Inference {
	
	private final MarkovLogicNetwork mln;
	
	public MCSAT(MarkovLogicNetwork mln) {
		this.mln = mln;
	}

	@Override
	public double pr(Atom ground, Evidence evidence) {
		Grounder grounder = new Grounder(this.mln, evidence);
		GroundedMarkovNetwork mrf = grounder.ground(ground);
		
		Random random = new Random();
		List<Atom> variables = mrf.getGroundings();
		Database world = getRandomAssignment(variables);
		
//		List<WeightedFormula<Clause>> clauses = this.getClauses(mrf.getformulas());
		
		int pSamples = 0;
		int fSamples = 0;
		
		int counter = 0;
		
		for (int i = 0; i < 1000; i++) {
			List<Clause> choosen = new ArrayList<Clause>();
			
			for (WeightedFormula<?> wf : mrf.getformulas()) {
				boolean value = wf.getFormula().getValue(world);
				boolean signal = wf.getWeight() > 0;
				
				if (value == signal) { // satisfied formulas
					double negWeight = signal ? - wf.getWeight() : wf.getWeight();
					double add = 1.0 - Math.exp(negWeight);
					
					if (random.nextDouble() < add) {
						Formula f = wf.getFormula();
						CNF cnf = signal ? f.toCNF() : Negation.OPERATOR.apply(f).toCNF();
						choosen.addAll(cnf.getClauses());
					}
				}
			}
			
			if (choosen.isEmpty()) {
				world = getRandomAssignment(variables);
				counter = -2;
				continue;
			} else {
				counter++;
			}
			CNF model = new CNF(choosen);
			WalkSAT walk = new WalkSAT(model);
			boolean solution = false;
			world = getRandomAssignment(variables);
			for (int j = 0; j < 10; j++) {
				Database sample = walk.sat(world);
				if (sample != null) { 
					world = sample;
					solution = true;
					break;
				}
			}
			
			if (solution && counter > 0) {
				boolean value = world.valueOf(ground);
				if (value) pSamples++; else fSamples++;
			}
		}
		
		int total = pSamples + fSamples;		
		return ((double) pSamples) / total;
	}
	
//	private List<WeightedFormula<Clause>> getClauses(List<WeightedFormula<?>> mrf) {
//		List<WeightedFormula<Clause>> clauses = new ArrayList<WeightedFormula<Clause>>();
//		for (WeightedFormula<?> wf : mrf) {
//			double weight = wf.getWeight();
//			CNF formula;
//			if (weight > 0) {
//				formula = wf.getFormula().toCNF();
//			} else {
//				weight = -weight;
//				formula = Negation.OPERATOR.apply(wf.getFormula()).toCNF();
//			}
//			for (Clause c : formula.getClauses()) {
//				WeightedFormula<Clause> clause = new WeightedFormula<Clause>(c, weight);
//				clauses.add(clause);
//			}
//		}
//		return clauses;
//	}
	
	private static Database getRandomAssignment(List<Atom> variables) {
		Random r = new Random();
		Database database = new SimpleDB();
		for (Atom atom : variables) database.set(atom, r.nextBoolean());
		return database;
	}
	
	
}
