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
		
		int pSamples = 0;
		int fSamples = 0;
		
		for (int i = 0; i < 10000; i++) {
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
			
			CNF model = new CNF(choosen);
			WalkSAT walk = new WalkSAT(model);
			boolean solution = false;
			for (int j = 0; j < 10; j++) {
				Database sample = walk.sat(world);
				if (sample != null) { 
					world = sample;
					solution = true;
				}
			}
			
			if (solution) {
				boolean value = world.valueOf(ground);
				System.out.println(ground + ": " + value);
				if (value) pSamples++; else fSamples++;
			} else {
				world = getRandomAssignment(variables);
			}
		}
		
		int total = pSamples + fSamples;		
		return ((double) pSamples) / total;
	}
	
	private static Database getRandomAssignment(List<Atom> variables) {
		Random r = new Random();
		Database database = new SimpleDB();
		for (Atom atom : variables) database.set(atom, r.nextBoolean());
		return database;
	}
	
	
}
