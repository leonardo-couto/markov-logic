package markovLogic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import markovLogic.inference.Evidence;

import fol.Atom;
import fol.Constant;
import fol.Formula;
import fol.Literal;
import fol.Variable;
import fol.WeightedFormula;
import fol.database.Database;

public class Grounder {
	
	private final MarkovLogicNetwork mln;
	private final Database db;
	private final Database evidence;
	
	private final Map<Atom,Literal> cache;
	private final Map<Atom,Atom> queued;
	
	public Grounder(MarkovLogicNetwork mln, Evidence evidence) {
		this.mln = mln;
		this.db = evidence.getDatabase();
		this.evidence = evidence;
		
		this.cache = new HashMap<Atom, Literal>();
		this.queued = new HashMap<Atom, Atom>();
	}
	
	public GroundedMarkovNetwork ground(Atom query) {
		
		List<WeightedFormula<?>> grounds = new ArrayList<WeightedFormula<?>>();
		
		Queue<Atom> queue = new LinkedList<Atom>();
		queue.offer(query);
		this.cache.put(query, new Literal(query, true));
		
		// Main loop, ground all formulas
		while (!queue.isEmpty()) {
			Atom ground = queue.poll(); // starts with a grounded atom
			
			for (WeightedFormula<?> wf : this.mln.getFormulas()) {
				Formula f = wf.getFormula();
				double w = wf.getWeight();
				if (f.hasPredicate(ground.predicate)) {
					Formula aux = replaceVars(ground, f);
					List<Formula> formulas = getGroundings(aux); // ground all other variables
					formulas = processFormulas(formulas, queue);
					double[] weights = new double[formulas.size()];
					Arrays.fill(weights, w);
					grounds.addAll(WeightedFormula.toWeightedFormulas(formulas, weights));
				}
			}
			this.queued.put(ground, ground);
		}
		
		return new GroundedMarkovNetwork(grounds, this.cache.keySet());
	}
	
	// check if any new ground has been produced, if has add to queue
	// replace all the evidences
	// replace all atoms for cached atoms
	private List<Formula> processFormulas(List<Formula> formulas, Queue<Atom> queue) {
		List<Formula> out = new ArrayList<Formula>(formulas.size());
		
		formulas: for (Formula formula : formulas) { 
			List<Atom> atoms = formula.getAtoms();
			for (Atom atom : atoms) {
				if (this.queued.containsKey(atom)) continue formulas; // formula already exists
				
				Literal cached = this.cache.get(atom);
				if (cached == null) {
					if (this.evidence.valueOf(atom)) {
						// is evidence
						boolean value = this.db.valueOf(atom);
						formula = formula.replace(atom, value ? Literal.TRUE : Literal.FALSE);

					} else {
						// new grounding!
						this.cache.put(atom, new Literal(atom, true));
						queue.offer(atom);
					}
				} else {
					formula = formula.replace(atom, cached);
				}
			}
			
			out.add(formula);
		}
		
		return out;
	}
	
	/**
	 * Replace the variables in formula for the constants in Atom a
	 * @return a copy of formula with some variables replaced
	 */
	private static Formula replaceVars(Atom grounded, Formula formula) {
		Atom formulaAtom = null;
		for (Atom a : formula.getAtoms()) {
			if (a.predicate == grounded.predicate) {
				formulaAtom = a;
				break;
			}
		}
		
		int terms = grounded.terms.length;
		Map<Variable, Constant> groundings = new HashMap<Variable, Constant>(terms*2);
		for (int i = 0; i < terms; i++) {
			groundings.put((Variable) formulaAtom.terms[i], (Constant) grounded.terms[i]);
		}
		
		return formula.ground(groundings);
	}
	
	/**
	 * Replace all variables in f, if there is more than one constant to replace
	 * one variable, duplicate the formula, thus creating all possible groundings
	 * for f.
	 * @param f Formula to be grounded
	 * @return a List of all possible grounds for f
	 */
	private static List<Formula> getGroundings(Formula f) {
		List<Formula> formulas = new LinkedList<Formula>();
		formulas.add(f);
		for (Variable v : f.getVariables()) {
			List<Formula> newFormulas = new LinkedList<Formula>();
			if (v.getConstants().isEmpty()) {
				v.getDomain().newConstant();
			}
			List<Constant> constants = new LinkedList<Constant>(v.getConstants());
			for (Formula aux : formulas) {
				for (Constant c : constants) {
					newFormulas.add(aux.ground(Collections.singletonMap(v, c)));
				}
			}
			formulas = newFormulas;
		}
		return formulas;
	}



}
