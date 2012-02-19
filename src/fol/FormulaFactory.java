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
		Queue<ConjunctiveNormalForm> heap = new PriorityQueue<ConjunctiveNormalForm>();
		
		for (Formula f : seeds) {
			ConjunctiveNormalForm seed = (ConjunctiveNormalForm) f; // TODO: arrumar!!
			Set<Variable> variables = seed.getVariables();
			if (variables.size() >= this.maxVars) continue;
			for (Predicate p : this.predicates) {
				for (Atom literal : this.generateAtoms(p, variables)) {
					
					// TODO contar quantas variáveis NOVAS tem, ver se passa do limite
					
					ConjunctiveNormalForm positive = seed.addLiteral(literal, false);
					if (positive == seed) continue;
					ConjunctiveNormalForm negative = seed.addLiteral(literal, true);
					
					heap.offer(positive.normalizeVariables());
					heap.offer(negative.normalizeVariables());
				}
			}
		}
		for (ConjunctiveNormalForm clause : this.putEquals(seeds)) {
			heap.offer(clause.normalizeVariables());
		}
		
		List<Formula> clauses = new ArrayList<Formula>(heap.size());
		ConjunctiveNormalForm aux = heap.poll();
		clauses.add(aux);
		for (int i = heap.size(); i > 0 ; i--) {
			ConjunctiveNormalForm clause = heap.poll();
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
	public List<ConjunctiveNormalForm> flipSigns(ConjunctiveNormalForm clause) {
		List<Atom> literals = clause.getAtoms();
		ArrayList<ConjunctiveNormalForm> aux;
		List<ConjunctiveNormalForm> clauses = new ArrayList<ConjunctiveNormalForm>(2);
		for (Atom literal : literals) {
			if (clauses.isEmpty()) {
				clauses.add(new ConjunctiveNormalForm(
						new Atom[] { literal }, 
						new boolean[] { false })
				);
				clauses.add(new ConjunctiveNormalForm(
						new Atom[] { literal }, 
						new boolean[] { true  })
				);
			} else {
				aux = new ArrayList<ConjunctiveNormalForm>(clauses.size()*2);
				for (ConjunctiveNormalForm c : clauses) {
					aux.add(c.addLiteral(literal, false));
					aux.add(c.addLiteral(literal, true ));
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
	public List<ConjunctiveNormalForm> getUnitClauses() {
		List<Atom> atoms = this.getAtoms();
		List<ConjunctiveNormalForm> unitClauses = new ArrayList<ConjunctiveNormalForm>(atoms.size());
		for (Atom a : atoms) {
			unitClauses.add(a.toCNF().get(0));
		}
		return unitClauses;
	}
	
	public void printCandidates(List<ConjunctiveNormalForm> clauses) {
		for (Formula cnf : this.generateClauses(clauses)) {
			System.out.println(cnf);
		}
	}
	
	public List<ConjunctiveNormalForm> putEquals(Collection<? extends Formula> clauses) {
		List<ConjunctiveNormalForm> newClauses = new ArrayList<ConjunctiveNormalForm>();
		
		for (Formula f : clauses) {
			ConjunctiveNormalForm clause = (ConjunctiveNormalForm) f;

			Set<Variable> variables = clause.getVariables();

			Term[] terms = new Term[2];
			Variable[] vars = variables.toArray(new Variable[variables.size()]);
			for (int i = 0; i < vars.length -1; i++) {
				for (int j = i+1; j < vars.length; j++) {
					if (vars[i].getDomain().equals(vars[j].getDomain())) {
						terms[0] = vars[i];
						terms[1] = vars[j];
						CompositeKey key = new CompositeKey(Predicate.equals, terms);
						Atom equals = this.atoms.get(key);
						if (equals == null) {
							equals = new Atom(Predicate.equals, terms[0], terms[1]);
							this.atoms.put(key, equals);
						}
						ConjunctiveNormalForm positive = clause.addLiteral(equals, false);
						if (positive == clause) continue;
						ConjunctiveNormalForm negative = clause.addLiteral(equals, true);
						newClauses.add(positive);
						newClauses.add(negative);
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
		Collections.sort(d.getVariables());
		for (Variable v : d.getVariables()) {
			if (!c.contains(v)) {
				return v;
			}
		}
		return d.newVariable();
	}	
	
}
