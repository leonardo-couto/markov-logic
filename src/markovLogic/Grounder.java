package markovLogic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
import fol.database.BinaryDB;

public class Grounder {
	
	private final MarkovLogicNetwork mln;
	private final BinaryDB db;
	private final Evidence evidence;
	
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
					List<Formula> partialGrounds = Grounder.replaceVars(ground, f);
					List<Formula> formulas = Grounder.getGroundings(partialGrounds); // ground all other variables
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
					if (this.evidence.isEvidence(atom)) {
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
	private static List<Formula> replaceVars(Atom grounded, Formula formula) {
		List<Atom> formulaAtom = new ArrayList<Atom>();
		for (Atom a : formula.getAtoms()) {
			if (a.predicate == grounded.predicate) {
				formulaAtom.add(a);
			}
		}
		
		int terms = grounded.terms.length;
		List<Formula> formulas = new ArrayList<Formula>();
		for (Atom target : formulaAtom) {
			Map<Variable, Constant> groundings = new HashMap<Variable, Constant>(terms*2);
			for (int i = 0; i < terms; i++) {
				groundings.put((Variable) target.terms[i], (Constant) grounded.terms[i]);
			}
			formulas.add(formula.ground(groundings));
		}
		
		return formulas;
	}
	
	/**
	 * <p>
	 * Replace all variables in formulas, if there is more than one constant to 
	 * replace one variable, duplicate the formula, thus creating all possible 
	 * groundings for formulas.</p>
	 * @param formulas List<Formula> to be grounded
	 * @return a List of all possible grounds for f
	 */
	private static List<Formula> getGroundings(List<Formula> formulas) {
		List<Formula> out = new ArrayList<Formula>();
		
		for (Formula f : formulas) {
			List<Formula> grounds = new LinkedList<Formula>();
			grounds.add(f);
			for (Variable v : f.getVariables()) {
				List<Formula> newFormulas = new LinkedList<Formula>();
				if (v.getConstants().isEmpty()) {
					v.getDomain().newConstant();
				}
				List<Constant> constants = new LinkedList<Constant>(v.getConstants());
				for (Formula aux : grounds) {
					for (Constant c : constants) {
						newFormulas.add(aux.ground(Collections.singletonMap(v, c)));
					}
				}
				grounds = newFormulas;
			}
			out.addAll(grounds);
		}
		
		return Grounder.removeDuplicates(out);
	}
	
	/**
	 * Does not guarantee uniqueness, but eliminate some duplicate formulas.
	 * @param formulas a List of formulas
	 * @return Ordered formulas with fewer duplicates
	 */
	private static List<Formula> removeDuplicates(List<Formula> formulas) {
		if (formulas.isEmpty()) return formulas;
				
		Collections.sort(formulas, new SimpleFormulaComparator());	

		int size = formulas.size();
		List<Formula> uniqueFormulas = new ArrayList<Formula>(size);
		Formula previous = formulas.get(0);
		uniqueFormulas.add(previous);
		for (int i = 1; i < size; i++) {
			Formula current = formulas.get(i);
			if (!(previous.equals(current))) {
				uniqueFormulas.add(current);
				previous = current;
			}
		}
		
		return uniqueFormulas;
	}
	
	private static class SimpleFormulaComparator implements Comparator<Formula> {

		@Override
		public int compare(Formula o1, Formula o2) {
			List<Atom> l1 = o1.getAtoms();
			List<Atom> l2 = o2.getAtoms();			
			
			int length1 = l1.size();
			int length2 = l2.size();

			int min = Math.max(length1, length2);
			for (int i = 0; i < min; i++) {
				Atom a1 = l1.get(i);
				Atom a2 = l2.get(i);
				int diff = a1.compareTo(a2);
				if (diff != 0) return diff;
			}
			
			return length1 == length2 ? 0 : length1 < length2 ? -1 : 1;
		}
		
		
	}



}
