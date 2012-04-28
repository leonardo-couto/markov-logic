package fol.sat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import fol.Atom;
import fol.CNF;
import fol.CNF.ReducedCNF;
import fol.Clause;
import fol.Literal;
import fol.database.BinaryDB;
import fol.database.BinaryDatabase;

/**
 * Algorithm to solve the boolean satisfability problem, based on:<br>
 * Selman et. al. - Local Search Strategies for Satisfiability Testing (1996)<br>
 * McAllester et. al. - Evidence for Invariants in Local Search (1997)
 */
public class WalkSAT {
	
	private final List<Literal> constants;
	private final List<Clause> clauses;
	private final Random picker;
	private final Random mRandom;
	private final Random pRandom;
	private final Random qRandom;
	private final Map<Clause, Boolean> satMap;
	private final List<Atom> variables;
	
	// Maps a Atom to all the clauses it appears with a true signal
	// and false signal in trueLiterals and falseLiterals respectively.
	private final Map<Atom, List<Clause>> trueLiterals;
	private final Map<Atom, List<Clause>> falseLiterals;
	
	
	private BinaryDB assignment;
	
	/**
	 * Probability of choosing a random walk approach rather
	 * than the greedy one.
	 */
	private final double p; // default = 0.5
	private final double q; // default = 0.6
	private final int maxTries; // < 10
	private final int maxFlips;
	
	private double temperature; // default = 0.5 (from paper Domingos)
	
	public WalkSAT(CNF cnf) {
		this.constants = new ArrayList<Literal>();
		this.variables = new ArrayList<Atom>();
		this.clauses = this.reduce(cnf);
		this.picker = new Random();
		this.mRandom = new Random();
		this.pRandom = new Random();
		this.qRandom = new Random();
		this.satMap = new HashMap<Clause, Boolean>();
		this.assignment = this.assignConstants(new BinaryDatabase());
		this.trueLiterals = new HashMap<Atom, List<Clause>>();
		this.falseLiterals = new HashMap<Atom, List<Clause>>();
		this.mapAtoms(this.clauses);
		int size = this.variables.size();
		
		this.p = 0.5;
		this.q = 0.6;
		this.temperature = 0.5;
		this.maxTries = 2;
		this.maxFlips = 2*size*size;
	}
	
	/**
	 * Attribute the value of all constants to the database
	 * @param db
	 * @return
	 */
	private BinaryDB assignConstants(BinaryDB db) {
		for (Literal l : this.constants) {
			db.set(l.atom, l.signal);
		}
		return db;
	}

	/**
	 * The break value counts how many clauses that were satisfied
	 * before are now unsatisfied.
	 */
	private int breakValue(Atom a) {
		return this.cost(a, true);
	}
	
	/**
	 * Chooses a literal in <code>c</code> according to different strategies
	 * and flip its value.
	 * @param c
	 * @return
	 */
	protected Atom chooseVariable(Clause c) {
		boolean greedy = (this.q < this.qRandom.nextDouble());
		return greedy ? this.greedySearch(c) : this.pick(c.getLiterals()).atom ;
	}
	
	/**
	 * <p>Perform either delta cost or break value of a variable, depending
	 * on the breakValue flag. See {@link #breakValue(Atom)} and
	 * {@link #deltaCost(Atom)}</p>
	 * 
	 * @param var Atom candidate to have its value changed
	 * @param breakValue flag to indicate whether to compute 
	 * the breakValue (true) or the deltaCost (false). 
	 * @return deltaCost or breakValue
	 */
	private int cost(Atom var, boolean breakValue) {
		if (Atom.TRUE == var) return 0;
		int satisfied = 0, broke = 0;		
	    boolean value = this.assignment.flip(var);
	    
	    // update values in satMap
	    List<Clause> cache = value ? this.trueLiterals.get(var) : this.falseLiterals.get(var);
	    List<Clause> check = value ? this.falseLiterals.get(var) : this.trueLiterals.get(var);
	    
	    if (cache != null) {
	    	for (Clause c : cache) {
	    		if (!this.satMap.get(c).booleanValue()) {
	    			satisfied++;
	    		}
	    	}
	    }
	    if (check != null) {
	    	for (Clause c : check) {
	    		if (!c.getValue(this.assignment)) {
	    			broke++;
	    		}
	    	}
	    }
	    
	    // flips back to original value
	    this.assignment.set(var, !value);
		return breakValue ? broke : broke - satisfied;
	}
	
	/**
	 * The delta cost is the difference between how many clauses were
	 * satisfied before, and how many are satisfied after changing 
	 * var's value.
	 */
	private int deltaCost(Atom a) {
		return this.cost(a, false);
	}
	
	/**
	 * Flips the value of <code>var</var>. Updates the truth value
	 * of all clauses to reflect the change in var's value.
	 */
	private void flip(Atom var) {
		if (Atom.TRUE == var) return;
	    boolean value = this.assignment.flip(var);
	    
	    // update values in satMap
	    List<Clause> cache = value ? this.trueLiterals.get(var) : this.falseLiterals.get(var);
	    List<Clause> check = value ? this.falseLiterals.get(var) : this.trueLiterals.get(var);
	    
	    if (cache != null) {
	    	for (Clause c : cache) {
	    		if (!this.satMap.get(c).booleanValue()) {
	    			this.satMap.put(c, true);
	    		}
	    	}
	    }
	    if (check != null) {
	    	for (Clause c : check) {
	    		if (!c.getValue(this.assignment)) {
	    			this.satMap.put(c, false);
	    		}
	    	}
	    }
	}
	
	/**
	 * Performs a greedy search, chooses the variable that has 
	 * the smaller {@link #breakValue(Atom)}. Often a negative number.
	 * @param clause
	 * @return
	 */
	private Atom greedySearch(Clause clause) {
	    List<Literal> literals = clause.getLiterals();
	    int size = literals.size();
	    int index = -1;
	    int max = Integer.MAX_VALUE;
	    for (int i = 0; i < size; i++) {
	        Atom a = literals.get(i).atom;
	        int breakValue = this.breakValue(a);
	        if (breakValue < max) {
	            max = breakValue;
	            index = i;
	        }
	    }
	    return literals.get(index).atom;
	}
	
	/**
	 * @return true if all clauses are satisfied by the current assignment
	 */
	private boolean isSatisfied() {
		boolean b = !this.satMap.values().contains(Boolean.FALSE);
		return b;
	}
	
	/**
	 * Maps each Atom to a List of Clauses where that atom appears.
	 * If it appears as a positive literal, store the combination
	 * in the trueLiterals map, else stores in falseLiteral.
	 */
	private void mapAtoms(List<Clause> clauses) {
		for (Clause c : clauses) {
			for (Literal l : c.getLiterals()) {
				
				Map<Atom, List<Clause>> map = l.signal ? this.trueLiterals : this.falseLiterals;
				List<Clause> mapClauses = map.get(l.atom);
				if (mapClauses == null) {
					mapClauses = new ArrayList<Clause>();
					map.put(l.atom, mapClauses);
				}
				mapClauses.add(c);
			}
		}
	}
	
	/**
	 * Performs a metropolis move
	 */
	private void metropolis() {
		Atom a = this.pick(this.variables);
		int delta = this.deltaCost(a);
		
		if (delta < 1) {
			this.flip(a);
		} else {
			double m = Math.exp(-delta/this.temperature);
			if (mRandom.nextDouble() < m) {
				this.flip(a);
			}
		}
	}
	
	/**
	 * Picks a random element from a List.
	 */
	private <T> T pick(List<T> list) {
		int size = list.size();
		int index = this.picker.nextInt(size);
		return list.get(index);
	}
	
	/**
	 * Gives a random assignment for the variable values
	 */
	private BinaryDB randomAssignment() {
		for (Atom a : this.variables) {
			boolean value = this.qRandom.nextBoolean();
			this.assignment.set(a, value);
		}
		return this.assignment;
	}
	
	/**
	 * Performs a randomWalk move
	 */
	private void randomWalk() {
		List<Clause> unsatisfied = this.unsatisfiedClauses();
		if (unsatisfied.isEmpty()) return;
		Clause clause = this.pick(unsatisfied);
		Atom var = this.chooseVariable(clause);
	    this.flip(var);
	}
	
	/**
	 * Remove all clauses of length one (recursively) and 
	 * add their literals to the constant list. All other atoms
	 * are added to the variables list.
	 * @param clauses clauses to be reduced.
	 * @return List of reduced clauses.
	 */
	private List<Clause> reduce(CNF cnf) {
		HashSet<Atom> atoms = new HashSet<Atom>(cnf.getAtoms());
		ReducedCNF reduced = cnf.reduce();
		for (Entry<Atom, Boolean> e : reduced.constants.entrySet()) {
			constants.add(new Literal(e.getKey(), e.getValue()));
			atoms.remove(e.getKey());
		}
		this.variables.addAll(atoms);
		return reduced.formula.getClauses();
	}
	
	/**
	 * Solves the boolean satisfiability problem.<br>
	 * Starts the local search with a random assignment.
	 */
	public BinaryDB sat() {
		for (int i = 0; i < this.maxTries; i++) {
			this.randomAssignment();
			this.updateSatMap();
			if (this.isSatisfied()) return this.assignment;
			BinaryDB assignment = this.sat(this.assignment);
			if (assignment != null) return assignment;
		}
		return null;
	}
	
	/**
	 * Solves the boolean satisfiability problem.<br>
	 * Starts the local search with the given assignment.<br>
	 * It does not check if the initialAssignment satisfies
	 * the formula before starting the search.
	 */
	public BinaryDB sat(BinaryDB initialAssignment) {
		if (this.assignment != initialAssignment) {
			this.assignment = this.assignConstants(initialAssignment);
			this.updateSatMap();
		}
		for (int i = 0; i < this.maxFlips; i++) {
            boolean randomWalk = (this.pRandom.nextDouble() < this.p);
            if (randomWalk) {
            	this.randomWalk();
            } else {
            	this.metropolis();
            }		    
			if (this.isSatisfied()) return this.assignment;
		}
		return null;
	}
	
	/**
	 * @return A List of all unsatisfied Clauses with the current assignment
	 */
	private List<Clause> unsatisfiedClauses() {
		List<Clause> clauses = new ArrayList<Clause>(this.clauses.size());
		for (Entry<Clause, Boolean> e : this.satMap.entrySet()) {
			if (!e.getValue().booleanValue()) {
				clauses.add(e.getKey());
			}
		}
		return clauses;
	}
	
	private void updateSatMap() {
		for (Clause c : this.clauses) {
			Boolean sat = Boolean.valueOf(c.getValue(this.assignment));
			this.satMap.put(c, sat);			
		}		
	}

}
