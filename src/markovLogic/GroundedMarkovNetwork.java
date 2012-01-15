package markovLogic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import util.ListPointer;
import fol.Atom;
import fol.Constant;
import fol.Formula;
import fol.Predicate;
import fol.Term;
import fol.Variable;

public class GroundedMarkovNetwork {
	
	private final List<WeightedFormula> formulas;
	private final List<Atom> groundings;
	private final List<List<Integer>> mapFormulaGrounding;
	
	private GroundedMarkovNetwork() {
		super();
		this.formulas = new LinkedList<WeightedFormula>();
		this.groundings = new LinkedList<Atom>();
		this.mapFormulaGrounding = new LinkedList<List<Integer>>();
	}
	
	/**
	 * The query Atom is always in the first position
	 * @return a List of all grounded atoms in the mln
	 */
	public List<Atom> getGroundings() {
		return new ArrayList<Atom>(this.groundings);
	}
	
	public List<WeightedFormula> getGroundedFormulas() {
		return this.formulas;
	}
	
	// TODO: se tiver dois predicados iguais na mesma formula, ver o que fazer.
	//       se eles gerarem groundings diferentes, deveriam estar os dois
	public static GroundedMarkovNetwork ground(MarkovLogicNetwork mln, Atom query, Collection<Atom> given) {
		GroundedMarkovNetwork groundedMln = new GroundedMarkovNetwork();
		
		// Store the atomicFormulas
		HashMap<Predicate,Double> atomicFormulas = new HashMap<Predicate,Double>();
		for (WeightedFormula f : mln) {
			if (f.getFormula() instanceof Atom) {
				atomicFormulas.put(((Atom) f.getFormula()).predicate, f.getWeight());
			}
		}
		
		Set<WeightedFormula> groundedFormulas = new HashSet<WeightedFormula>();
		List<Atom> groundings = new LinkedList<Atom>();
		Queue<Atom> queue = new LinkedList<Atom>();
		queue.offer(query);
		groundings.add(query);
		
		// Main loop, ground all formulas
		while (!queue.isEmpty()) {
			Atom a = queue.poll(); // starts with a grounded atom
			for (WeightedFormula wf : mln) {
				Formula f = wf.getFormula();
				double w = wf.getWeight();
				if (f.hasPredicate(a.predicate)) {
					if (f instanceof Atom) {
						groundedFormulas.add(new WeightedFormula(a, w));
					} else {
						Formula aux = replaceVars(a, f);
						List<Formula> formulas = getGroundings(aux); // ground all other variables
						for (Formula grounded : formulas) { // check if any new ground has been produced
							groundedFormulas.add(new WeightedFormula(grounded, w));
							List<Atom> groundedAtoms = grounded.getAtoms();
							for (int i = 0; i < groundedAtoms.size(); i++) {
								if (!groundedAtomIn(groundedAtoms, i, groundings)) {
									if (!groundedAtomIn(groundedAtoms, i, given)) {
										// new grounding!
										Atom gAtom = groundedAtoms.get(i);
									    groundings.add(gAtom);
									    queue.offer(gAtom);
									} else {
										// evidence, look for an atomic formula that
										// represents this evidence
										Atom aGiven = groundedAtoms.get(i);
										if (atomicFormulas.containsKey(aGiven.predicate)) {
											groundedFormulas.add(new WeightedFormula(aGiven, atomicFormulas.get(aGiven.predicate)));
										}
									}
								}
							}
						}
					}				
				}
			}
		}
		
		// initializes the GroundedMarkovNetwork instance
		groundedMln.groundings.addAll(groundings);
		for (WeightedFormula f : groundedFormulas) {
			List<Integer> map = new LinkedList<Integer>();
			groundedMln.formulas.add(f);
			groundedMln.mapFormulaGrounding.add(map);
			map: for (Atom a : f.getFormula().getAtoms()) {
				for (int i = 0; i < groundings.size(); i++) {
					if (a == groundings.get(i)) {
						map.add(i);
						continue map;
					}
				}
				map.add(null); // givenAtom
			}
		}
		
		return groundedMln;
	}
	
	/**
	 * Checks if the Atom in the <code>atomIndex</code> position of <code>groundedAtoms</code> List 
	 * is in <code>container</code>. Removes duplicate instances of the same atom
	 * @param groundedAtoms
	 * @param atomIndex
	 * @param container
	 * @return true is the container contains the groundedAtom
	 */
	private static boolean groundedAtomIn(List<Atom> groundedAtoms, int atomIndex, Collection<Atom> container) {
		Atom gAtom = groundedAtoms.get(atomIndex);
		for (Atom g : container) {
			if (gAtom.equals(g)) {
				groundedAtoms.set(atomIndex, g); // remove duplicate instances of the same atom
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Replace the variables in formula for the constants in a
	 * @return a copy of formula with some variables replaced
	 */
	private static Formula replaceVars(Atom a, Formula formula) {
		Formula f = formula.copy();
		ListPointer<Atom> p = f.getAtomPointer(a.predicate);
		
		List<Variable> vars = new ArrayList<Variable>();
		for (Term t : p.get().terms) {
			vars.add((Variable) t);
		}
		List<Constant> cons = new ArrayList<Constant>();
		for (Term t : a.terms) {
			cons.add((Constant) t);
		}
		p.set(a);
		f = f.replaceVariables(vars, cons);
		return f;
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
				v.newConstant();
			}
			List<Constant> constants = new LinkedList<Constant>(v.getConstants());
			for (Formula aux : formulas) {
				for (Constant c : constants) {
					newFormulas.add(aux.replaceVariables(Collections.singletonList(v), Collections.singletonList(c)));
				}
			}
			formulas = newFormulas;
		}
		return formulas;
	}
	
	/**
	 * Get the sum of weights of the satisfied formulas given
	 * the possible world <code>world</code>
	 * @param world List of Atom.TRUE and Atom.FALSE representing
	 *     the groundings truth values.
	 * @return the sum of weights of all formulas that are true
	 *         in the given world.
	 */
	public double sumWeights(List<Atom> world) {
		double value = 0d;
		for (int i = 0; i < this.formulas.size(); i++) {
			List<Integer> idx = this.mapFormulaGrounding.get(i);
			WeightedFormula wf = this.formulas.get(i);
			Formula f = wf.getFormula();
			double d = wf.getWeight();
			if (f instanceof Atom) {
				if (idx.get(0) == null) { // evidence
					value = value + d * f.getValue();
				} else {
					value = value + d * world.get(idx.get(0)).getValue();
				}
			} else {
				List<Atom> atoms = f.getAtoms();
				for (int j = 0; j < atoms.size(); j++) {
					Integer k = idx.get(j);
					if (k != null) {
						atoms.set(j, world.get(k));
					}
				}
				value = value + d * f.getValue();
			}
		}
		return value;
	}
	
}
