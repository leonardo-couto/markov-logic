package fol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import fol.database.CompositeKey;

public class FormulaFactory {
	
	private final Set<Predicate> predicates;
	private final Map<CompositeKey, Atom> atoms;
	private final int maxVars;
	
	public FormulaFactory(Set<Predicate> predicates, int maxVars) {
		this.predicates = predicates;
		this.atoms = new HashMap<CompositeKey, Atom>();
		this.maxVars = maxVars;
	}
	
	public List<Formula> generateClauses(Collection<? extends Formula> seeds) {
		Queue<Clause> heap = new PriorityQueue<Clause>();
		
		for (Formula f : seeds) {
			Clause seed = (Clause) f; // TODO: arrumar!!
			Set<Variable> variables = seed.getVariables();
			for (Predicate p : this.predicates) {
				for (Atom atom : this.generateAtoms(p, variables)) {
					
					Clause positive = seed.addLiteral(new Literal(atom, true));
					if (positive == seed || positive.getVariables().size() > this.maxVars) continue;
					Clause negative = seed.addLiteral(new Literal(atom, false));
					
					heap.offer(positive.normalizeVariables());
					heap.offer(negative.normalizeVariables());
				}
			}
		}
//		for (ConjunctiveNormalForm clause : this.putEquals(heap)) {
//			heap.offer(clause.normalizeVariables());
//		}
		
		List<Formula> clauses = new ArrayList<Formula>(heap.size());
		Clause aux = heap.poll();
		clauses.add(aux);
		for (int i = heap.size(); i > 0 ; i--) {
			Clause clause = heap.poll();
			if (!clause.equals(aux)) {
				aux = clause;
				clauses.add(clause);
			}
		}
		
		return clauses;	
	}
	
	public List<Clause> generatePositiveClauses(Collection<Clause> seeds) {
		Queue<Clause> heap = new PriorityQueue<Clause>();
		
		for (Clause f : seeds) {
			
			// transform in positive only
			List<Literal> literals = f.getLiterals();
			for (int i = 0; i < literals.size(); i++) {
				Literal l = literals.get(i);
				if (!l.signal) {
					literals.set(i, new Literal(l.atom, true));
				}
			}
			Clause seed = new Clause(literals, true);
			
			Set<Variable> variables = seed.getVariables();
			for (Predicate p : this.predicates) {
				for (Atom atom : this.generateAtoms(p, variables)) {
					
					Clause positive = seed.addLiteral(new Literal(atom, true));
					if (positive == seed || positive.getVariables().size() > this.maxVars) continue;
					
					heap.offer(positive.normalizeVariables());
				}
			}
		}
		
		List<Clause> clauses = new ArrayList<Clause>(heap.size());
		Clause aux = heap.poll();
		clauses.add(aux);
		for (int i = heap.size(); i > 0 ; i--) {
			Clause clause = heap.poll();
			if (!clause.equals(aux)) {
				aux = clause;
				clauses.add(clause);
			}
		}
		
		return clauses;	
	}

	/**
	 * Generate all possible combinations of containing the same literals of
	 * given clause
	 * 
	 * @param clause
	 * @return
	 */
	public List<Clause> flipSigns(Clause clause) {
		List<Literal> literals = clause.getLiterals();
		ArrayList<Clause> aux;
		List<Clause> clauses = new ArrayList<Clause>(2);
		for (Literal literal : literals) {
			Literal inverted = new Literal(literal.atom, !literal.signal);
			if (clauses.isEmpty()) {
				clauses.add(new Clause(Collections.singletonList(literal)));
				clauses.add(new Clause(Collections.singletonList(inverted)));
			} else {
				aux = new ArrayList<Clause>(clauses.size()*2);
				for (Clause c : clauses) {
					aux.add(c.addLiteral(literal));
					aux.add(c.addLiteral(inverted));
				}
				clauses = aux;
			}			
		}
		
		return clauses;
	}
	
	/**
	 * Generate one Atom for each Predicate
	 * @return
	 */
	public List<Atom> getAtoms() {
		List<Atom> atoms = new ArrayList<Atom>(this.predicates.size());
		for (Predicate p : this.predicates) {
			Atom a = generateAtom(p);
			CompositeKey key = new CompositeKey(p, a.terms);
			Atom stored = this.atoms.get(key);
			if (stored == null) {
				this.atoms.put(key, a);
				stored = a;
			}
			atoms.add(stored);
		}
		return atoms;
	}	
	
	/**
	 * Generates unit clauses (Atoms) for each Predicate.
	 * Same as getAtoms, but encapsulates the Atom in
	 * a ConjunctiveNormalForm object.
	 * @return
	 */
	public List<Clause> getUnitClauses() {
		List<Atom> atoms = this.getAtoms();
		List<Clause> unitClauses = new ArrayList<Clause>(atoms.size());
		for (Atom a : atoms) {
			Literal literal = new Literal(a, true);
			unitClauses.add(new Clause(literal));
		}
		return unitClauses;
	}
	
	public void printCandidates(List<Clause> clauses) {
		for (Formula cnf : this.generateClauses(clauses)) {
			System.out.println(cnf);
		}
	}
	
	public List<Clause> putEquals(Collection<? extends Formula> clauses) {
		List<Clause> newClauses = new ArrayList<Clause>();
		
		for (Formula f : clauses) {
			Clause clause = (Clause) f;

			Set<Variable> variables = clause.getVariables();

			Term[] terms = new Term[2];
			Variable[] vars = variables.toArray(new Variable[variables.size()]);
			for (int i = 0; i < vars.length -1; i++) {
				for (int j = i+1; j < vars.length; j++) {
					if (vars[i].getDomain().equals(vars[j].getDomain())) {
						terms[0] = vars[i];
						terms[1] = vars[j];
						CompositeKey key = new CompositeKey(Predicate.EQUALS, terms);
						Atom equals = this.atoms.get(key);
						if (equals == null) {
							equals = new Atom(Predicate.EQUALS, terms[0], terms[1]);
							this.atoms.put(key, equals);
						}
						Clause positive = clause.addLiteral(new Literal(equals, true));
						if (positive == clause) continue;
//						ConjunctiveNormalForm negative = clause.addLiteral(new Literal(equals, false));
						newClauses.add(positive);
//						newClauses.add(negative);
					}
				}
			}
		}
		return newClauses;
	}
	
	/**
	 * Generates one Atom (not grounded) of Predicate p.
	 */
	public static Atom generateAtom(Predicate p) {

		List<Variable> var = new ArrayList<Variable>();
		for (Domain d : p.getDomains()) {
			var.add(newVariableNotIn(d, var));
		}
		return new Atom(p, var.toArray(new Term[var.size()]));
	}
	
//	// Generate one not grounded Atom of Predicate p with distinct Variables 
//	// tries to use the Variables passed, if it is not possible, use a new one.
//	@SuppressWarnings("unused")
//	private static Atom generateAtom(Predicate p, Collection<Variable> variables) {
//		List<Variable> vars = new ArrayList<Variable>(p.getDomains().size());
//		domain:
//		for (Domain d : p.getDomains()) {
//			for(Variable v : variables) {
//				if (!vars.contains(v) && Domain.in(v, d)) {
//					vars.add(v);
//					continue domain;
//				}
//			}
//			vars.add(newVariableNotIn(d, vars));
//		}
//		return new Atom(p, vars.toArray(new Term[vars.size()]));
//	}
	
	/** 
	 * Generate all possible (not grounded) Atoms of Predicate p with distinct Variables 
	 * that has at least one Variable in variables
	 */
	public Set<Atom> generateAtoms(Predicate p, Collection<Variable> variables) {
		// The boolean indicates whether the List of Variables created has at least
		// one Variable in variables.
		Map<List<Variable>, Boolean> out = new HashMap<List<Variable>, Boolean>();
		Map<List<Variable>, Boolean> prev;
		boolean firstStep = true;
		
		// first step:
		Domain fd = p.getDomains().get(0);
		Set<Variable> possibleVariables = new HashSet<Variable>();
		// Check if any of the variables passed can be used in this domain.
		for (Variable v : variables) {
			if (Domain.in(v, fd)) {
				possibleVariables.add(v);
				out.put(Collections.singletonList(v), Boolean.TRUE);
			}
		}
		out.put(Collections.singletonList(newVariableNotIn(fd, possibleVariables)), Boolean.FALSE);
		
		// main loop:
		for (Domain d : p.getDomains()) {
			if(firstStep) {
				firstStep = false;
				continue;
			}
			prev = out;
			out = new HashMap<List<Variable>, Boolean>(); 
			
			possibleVariables = new HashSet<Variable>();
			// Check if any of the variables passed can be used in this domain.
			for (Variable v : variables) {
				if (Domain.in(v, d)) {
					possibleVariables.add(v);
				}
			}
			
			for (List<Variable> lv : prev.keySet()) {
				
				// Put all possible given variables.
				for (Variable v : possibleVariables) {
					if (lv.contains(v)) {
						continue;
					}
					List<Variable> e = new ArrayList<Variable>(lv);
					e.add(v);
					out.put(e, Boolean.TRUE);
				}
				
				// Try to repeat any variable already used.
				Set<Variable> repeated = new HashSet<Variable>();
				for (Variable v : lv) {
					if (Domain.in(v, d) && !possibleVariables.contains(v)) {
						repeated.add(v);
					}
				}
				// Commented out: Do not repeat variables, use Equals(X,Y) Predicate instead.
				//for (Variable v : repeated) {
				//	List<Variable> e = new ArrayList<Variable>(lv);
				//	e.add(v);
				//	out.put(e, prev.get(lv));
				//}

				
				// Try a new variable
				List<Variable> element = new ArrayList<Variable>(lv);
				repeated.addAll(possibleVariables);
				element.add(newVariableNotIn(d, repeated));
				out.put(element, prev.get(lv));
				
			}
		}
		
		// Remove all Lists that do not use at least one given variable.
		Set<Atom> set = new HashSet<Atom>();
		for (List<Variable> l : out.keySet()) {
			if (out.get(l).booleanValue()) {
				Term[] terms = l.toArray(new Term[l.size()]);
				CompositeKey key = new CompositeKey(p, terms);
				Atom a = this.atoms.get(key);
				if (a == null) {
					a = new Atom(p, terms);
					this.atoms.put(key, a);
				}
				set.add(a);
			}
		}
		return set;
	}
	
	// Return a variable from Domain d not in c. If there is no Variable
	// in d that is not in c, creates a new one.
	private static Variable newVariableNotIn(Domain d, Collection<Variable> c) {
		List<Variable> variables = d.getVariables();
		for (Variable v : variables) {
			if (!c.contains(v)) {
				return v;
			}
		}
		return d.newVariable();
	}	
	
}
