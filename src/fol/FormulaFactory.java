package fol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fol.operator.Biconditional;
import fol.operator.Conjunction;
import fol.operator.Disjunction;
import fol.operator.Negation;

/**
 * @author Leonardo Castilho Couto
 *
 */
public class FormulaFactory {
	
	protected final Set<Atom> atoms;

	private static final Disjunction DISJUNCTION = Disjunction.OPERATOR;
	private static final Conjunction CONJUNCTION = Conjunction.OPERATOR;
	private static final Biconditional BICONDITIONAL = Biconditional.OPERATOR;
	private static final Negation NEGATION = Negation.OPERATOR;
	
	public FormulaFactory(Collection<Atom> atoms) {
		this.atoms = new HashSet<Atom>(atoms);
	}
	
	public static List<Formula> extendsFormula(Formula f0, Formula f1) {
		List<Formula> out = new ArrayList<Formula>(5);
		
		if(f0 != f1) {
			// Disjunction
			out.add(DISJUNCTION.apply(f0,f1));
			// Implication
			out.add(DISJUNCTION.apply(NEGATION.apply(f0),f1));
			// Inverse Implication
			out.add(DISJUNCTION.apply(f0,NEGATION.apply(f1)));
			// Conjunction
			out.add(CONJUNCTION.apply(f0,f1));
			// Biconditional
			out.add(BICONDITIONAL.apply(f0,f1));			
		}
		
		return out;
	}
	
	public Set<Formula> generateFormulas(Collection<Formula> clauses, int maxAtoms, int maxVars) {
		Set<Formula> newFormulas = new HashSet<Formula>();
		
		for (Formula f : clauses) {
			 if (f.length() >= maxAtoms ) {
				 continue; 
			 }
			Set<Variable> variables = f.getVariables();
			if (variables.size() > maxVars) {
				continue;
			}
			nextAtom: for (Atom a : this.atoms) {
				for (Atom fa : f.getAtoms()) {
					if (fa == a) {
						continue nextAtom;
					}
				}
				for (Term t : a.terms) {
					if (variables.contains(t)) {
						newFormulas.addAll(extendsFormula(f, a));
						continue nextAtom;
					}
				}
			}
		}
		newFormulas.addAll(putEquals(clauses));
		return newFormulas;
	}
	
	public Set<Formula> putEquals(Collection<Formula> clauses) {
		Set<Formula> newFormulas = new HashSet<Formula>();
		
		for (Formula f : clauses) {

			Set<Variable> variables = f.getVariables();

			Variable[] vars = variables.toArray(new Variable[variables.size()]);
			for (int i = 0; i < vars.length -1; i++) {
				for (int j = i+1; j < vars.length; j++) {
					if (vars[i].getDomain().equals(vars[j].getDomain())) {						
						Atom equals = new Atom(Predicate.equals, vars[i], vars[j]);
						for (Atom atom : f.getAtoms()) {
							if (!(atom.predicate == Predicate.equals) || !atom.equals(equals)) {
								newFormulas.addAll(extendsFormula(f, equals));								
							}
						}
					}
				}
			}
		}
		return newFormulas;
	}
	
	public static Set<Atom> generateAtoms(Predicate p, int n) {
		
		Map<Domain, List<Variable>> mapVar = new HashMap<Domain, List<Variable>>();
		List<List<Variable>> terms = new ArrayList<List<Variable>>();
		Map<Domain, Integer> mapInt = new HashMap<Domain, Integer>();
		
		// Counts domain repetition in predicate arguments
		for (Domain d : p.getDomains()) {
			if (mapInt.containsKey(d)) {
				mapInt.put(d, mapInt.get(d).intValue() + 1);
			} else {
				mapInt.put(d, 1);
			}
		}
		
		for (Domain d : p.getDomains()) {
			if (!mapVar.containsKey(d)) {
				List<Variable> used = new ArrayList<Variable>(n);
				for (int i = 0; i < Math.max(n,mapInt.get(d)); i++) {
					used.add(newVariableNotIn(d, used));
				}
				mapVar.put(d, used);
			}
		}
		
		boolean first = true;
		for (Domain d : p.getDomains()) {
			if (first) {
				for (Variable v : mapVar.get(d)) {
					terms.add(Collections.singletonList(v));				
				}
				first = false;
				continue;
			}
			List<List<Variable>> aux = new ArrayList<List<Variable>>();
			for (List<Variable> lvar : terms) {
				for (Variable v : mapVar.get(d)) {
					if (!lvar.contains(v)) {
						List<Variable> element = new ArrayList<Variable>(lvar);
						element.add(v);
						aux.add(element);
					}
				}
			}
			terms = aux;
		}
		
		Set<Atom> out = new HashSet<Atom>();
		for (List<Variable> lvar : terms) {
			out.add(new Atom(p, lvar.toArray(new Term[lvar.size()])));
		}
		
		return out;
		
	}
	
	// Generate all possible (not grounded) Atoms of Predicate p with distinct Variables 
	// that has at least one Variable in variables
	public static Set<Atom> generateAtoms(Predicate p, Collection<Variable> variables) {
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
				set.add(new Atom(p, l.toArray(new Term[l.size()])));
			}
		}
		return set;
	}

	// Generate one not grounded Atom of Predicate p with distinct Variables 
	// tries to use the Variables passed, if it is not possible, use a new one.
	public static Atom generateAtom(Predicate p, Collection<Variable> variables) {
		List<Variable> vars = new ArrayList<Variable>(p.getDomains().size());
		domain:
		for (Domain d : p.getDomains()) {
			for(Variable v : variables) {
				if (!vars.contains(v) && Domain.in(v, d)) {
					vars.add(v);
					continue domain;
				}
			}
			vars.add(newVariableNotIn(d, vars));
		}
		return new Atom(p, vars.toArray(new Term[vars.size()]));
	}

	
	// Generates one Atom (not grounded) of Predicate p.
	public static Atom generateAtom(Predicate p) {

		List<Variable> var = new ArrayList<Variable>();
		for (Domain d : p.getDomains()) {
			var.add(newVariableNotIn(d, var));
		}
		return new Atom(p, var.toArray(new Term[var.size()]));
	}
	
	// Generates one Atom (not grounded) for each Predicate p.
	public static List<Atom> getUnitClauses(Collection<Predicate> predicates) {
		List<Atom> atoms = new ArrayList<Atom>();
		for (Predicate p : predicates) {
			atoms.add(generateAtom(p));
		}
		return atoms;
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
