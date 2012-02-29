package fol.sat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import stat.ElementPicker;
import fol.Atom;
import fol.Clause;
import fol.Literal;
import fol.database.CompositeKey;
import fol.database.Database;
import fol.database.SimpleDB;

public class WalkSAT {
	
	private static final List<Clause> FALSE = Collections.singletonList(Clause.FALSE);
	
	private final Comparator<Literal> comparator;
	private final List<Literal> constants;
	private final List<Clause> clauses;
	private final ElementPicker picker;
	private final Random random;
	private final Map<Clause, Boolean> satMap;
	private final List<Atom> variables;
	
	// Maps a Atom to all the clauses it appears with a true signal
	// and false signal in trueLiterals and falseLiterals respectively.
	private final Map<CompositeKey, List<Clause>> trueLiterals;
	private final Map<CompositeKey, List<Clause>> falseLiterals;
	
	
	private Database assignment;
	
	/**
	 * Probability of choosing a random walk approach rather
	 * than the greedy one.
	 */
	private final double p; // default = 0.6
	private final int maxTries; // < 10
	private final int maxFlips;
	
	public WalkSAT(Collection<? extends Clause> clauses) {
		this.comparator = new Literal.AtomComparator();
		this.constants = new ArrayList<Literal>();
		this.clauses = this.reduce(clauses);
		this.picker = new ElementPicker(new Random());
		this.random = new Random();
		this.satMap = new HashMap<Clause, Boolean>();
		this.trueLiterals = new HashMap<CompositeKey, List<Clause>>();
		this.falseLiterals = new HashMap<CompositeKey, List<Clause>>();
		this.variables = this.variables(this.clauses);
		this.assignment = this.assignConstants(new SimpleDB());
		
		this.p = 0.6;
		this.maxTries = 1;
		this.maxFlips = 1000; // 2 ^ numero de atomos distintos ??
	}
	
	private Database assignConstants(Database db) {
		for (Literal l : this.constants) {
			db.set(l.atom, l.signal);
		}
		return db;
	}
	
	private int deltaSatisfied(Atom a) {
		if (Atom.TRUE == a) return 0;
		int delta = 0;		
	    boolean value = this.assignment.flip(a);
	    
	    // update values in satMap
	    CompositeKey key = new CompositeKey(a);
	    List<Clause> cache = value ? this.trueLiterals.get(key) : this.falseLiterals.get(key);
	    List<Clause> check = value ? this.falseLiterals.get(key) : this.trueLiterals.get(key);
	    
	    if (cache != null) {
	    	for (Clause c : cache) {
	    		if (!this.satMap.get(c).booleanValue()) {
	    			delta++;
	    		}
	    	}
	    }
	    if (check != null) {
	    	for (Clause c : check) {
	    		if (!c.getValue(this.assignment)) {
	    			delta--;
	    		}
	    	}
	    }
	    
	    // flips back to original value
	    this.assignment.set(a, !value);
		return delta;
	}
	
	private void flip(Atom a) {
		if (Atom.TRUE == a) return;
	    boolean value = this.assignment.flip(a);
	    
	    // update values in satMap
	    CompositeKey key = new CompositeKey(a);
	    List<Clause> cache = value ? this.trueLiterals.get(key) : this.falseLiterals.get(key);
	    List<Clause> check = value ? this.falseLiterals.get(key) : this.trueLiterals.get(key);
	    
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
	
	private void greedySearch(Clause clause) {
	    List<Literal> literals = clause.getLiterals();
	    int size = literals.size();
	    int index = -1;
	    int max = Integer.MIN_VALUE;
	    for (int i = 0; i < size; i++) {
	        Atom a = literals.get(i).atom;
	        int delta = this.deltaSatisfied(a);
	        if (delta > max) {
	            max = delta;
	            index = i;
	        }
	    }
	    this.flip(literals.get(index).atom);
	}
	
	private boolean isSatisfied() {
		boolean b = !this.satMap.values().contains(Boolean.FALSE);
		return b;
	}
	
	private void updateSatMap() {
		for (Clause c : this.clauses) {
			Boolean sat = Boolean.valueOf(c.getValue(this.assignment));
			this.satMap.put(c, sat);			
		}		
	}
	
	private Database randomAssignment() {
		for (Atom a : this.variables) {
			boolean value = this.random.nextBoolean();
			this.assignment.set(a, value);
		}
		return this.assignment;
	}
	
	private void randomWalk(Clause clause) {
	    Atom a = this.picker.pick(clause.getLiterals()).atom;
	    this.flip(a);
	}
	
	/**
	 * Remove all clauses of length one (recursively) and 
	 * add their literals to the constant list.
	 * @param clauses clauses to be reduced.
	 * @return List of reduced clauses.
	 */
	private List<Clause> reduce(Collection<? extends Clause> clauses) {
		List<Clause> clauseList = new ArrayList<Clause>(clauses);
		boolean reduce = true;
		while (reduce) {
			reduce = false;
			for (int i = 0; i < clauseList.size(); i++) {
				Clause c = clauseList.get(i);
				if (c.length() == 1) {
					clauseList = this.reduce(clauseList, c);
					if (clauseList != FALSE) {
						reduce = true;
					}
					break;
				}
			}
		}
		return clauseList;
	}
	
	private List<Clause> reduce(List<Clause> clauses, Clause clause) {
		Literal literal = clause.getLiterals().get(0);
		this.constants.add(literal);
		List<Clause> reducedList = new ArrayList<Clause>(clauses.size());
		for (Clause c : clauses) {
			Clause reduced = this.reduceClause(c, literal);
			if (!Clause.TRUE.equals(reduced)) {
				if (Clause.FALSE.equals(reduced)) {
					return FALSE;
				}
				reducedList.add(reduced);
			}
		}
		
		return reducedList;
	}
	
	private Clause reduceClause(Clause clause, Literal literal) {
		List<Literal> literals = clause.getLiterals();
		int index = Collections.binarySearch(literals, literal, this.comparator);
		
		if (index > -1) { // clause contains literal
			Literal l = literals.get(index);
			
			if (literal.signal != l.signal) { // remove literal from clause
				literals.remove(index);
				return literals.isEmpty() ? Clause.FALSE : new Clause(literals);

			} else { // clause always true
				return Clause.TRUE;
			}
		}
		// does not contain literal, don't reduce it
		return clause;
	}
	
	public Database sat() {
		for (int i = 0; i < this.maxTries; i++) {
			this.randomAssignment();
			this.updateSatMap();
			if (this.isSatisfied()) return this.assignment;
			Database assignment = this.sat(this.assignment);
			if (assignment != null) return assignment;
		}
		return null;
	}
	
	public Database sat(Database initialAssignment) {
		if (this.assignment != initialAssignment) {
			this.assignment = this.assignConstants(initialAssignment);
			this.updateSatMap();
		}
		for (int i = 0; i < this.maxFlips; i++) {
			List<Clause> unsatisfied = this.unsatisfiedClauses();
			Clause choosen = this.picker.pick(unsatisfied);

			boolean randomWalk = (this.p > this.random.nextDouble());
			if(randomWalk) this.randomWalk(choosen);
			else this.greedySearch(choosen);
			if (this.isSatisfied()) return this.assignment;
		}
		return null;
	}
	
	private List<Clause> unsatisfiedClauses() {
		List<Clause> clauses = new ArrayList<Clause>(this.clauses.size());
		for (Entry<Clause, Boolean> e : this.satMap.entrySet()) {
			if (!e.getValue().booleanValue()) {
				clauses.add(e.getKey());
			}
		}
		return clauses;
	}
	
	private List<Atom> variables(List<Clause> clauses) {
		HashMap<CompositeKey, Atom> atoms = new HashMap<CompositeKey, Atom>();
		for (Clause c : clauses) {
			for (Literal l : c.getLiterals()) {
				CompositeKey key = new CompositeKey(l.atom);
				atoms.put(key, l.atom);
				
				Map<CompositeKey, List<Clause>> map = l.signal ? this.trueLiterals : this.falseLiterals;
				List<Clause> mapClauses = map.get(key);
				if (mapClauses == null) {
					mapClauses = new ArrayList<Clause>();
					map.put(key, mapClauses);
				}
				mapClauses.add(c);
			}
		}
		
		return new ArrayList<Atom>(atoms.values());
	}

}
