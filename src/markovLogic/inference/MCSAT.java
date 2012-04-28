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
import fol.database.BinaryDatabase;
import fol.database.BinaryDB;
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
		BinaryDB world = getRandomAssignment(variables);
		
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
				BinaryDB sample = walk.sat(world);
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
	
	private static BinaryDB getRandomAssignment(List<Atom> variables) {
		Random r = new Random();
		BinaryDB database = new BinaryDatabase();
		for (Atom atom : variables) database.set(atom, r.nextBoolean());
		return database;
	}
	
	
}
