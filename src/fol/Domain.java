package fol;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import util.NameID;

/**
 * Represents the domain for <code>Terms</code>. eg. the constants Ana and Bob
 * may belong to a domain named person. The variable x may represent a movie
 * in a domain named movie.
 * 
 * @author Leonardo Castilho Couto
 *
 */
public class Domain extends HashSet<Constant> implements NameID {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L; // TODO: generate serial id;
	private String name;
	private List<Function> functions;
	private List<Variable> variables;
	private Domain parent;
	private int varcount;
	
	public static final Domain universe = new Domain("universe", null);

	/**
	 * 
	 */
	public Domain(String name) {
		this.name = name;
		this.functions = new ArrayList<Function>();
		this.variables = new ArrayList<Variable>();
		this.parent = universe;
		this.varcount = 0;
	}
	
	public Domain(String name, Domain parent) {
		this.name = name;
		this.functions = new ArrayList<Function>();
		this.variables = new ArrayList<Variable>();
		this.parent = parent;
		this.varcount = 0;
	}
	
	public Variable newVariable() {
		varcount++;
		return new Variable("v" + this.getName() + varcount, this);
	}
	
	public Constant newConstant() {
		varcount++;
		return new Constant("c" + this.getName() + this.size(), this);
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the functions
	 */
	public List<Function> getFunctions() {
		return functions;
	}

	/**
	 * @return the variables
	 */
	public List<Variable> getVariables() {
		return variables;
	}

	/**
	 * @return the parent
	 */
	public Domain getParent() {
		return parent;
	}
	
	public boolean add(Function f) {
		return functions.add(f);
	}
	
	public boolean add(Variable v) {
		return variables.add(v);
	}
	
	public boolean remove(Function f) {
		return functions.remove(f);
	}
	
	public boolean remove(Variable v) {
		return functions.remove(v);
	}
	
	public String toString() {
		return name;
	}
	
	public boolean equals(Object o) {
		// TODO: Check domain, predicates, and formulas equals for case sensitive
		//       compare with .mln specification rules.
		if (this == o) return true;
	    if ( !(o instanceof Domain) ) return false;
	    
	    Domain d = (Domain) o;
	    return name.equals(d.getName());
	}
	
	public int hashCode() {
		return name.hashCode();
	}
	
	// Returns true if Term t is in Domain d
	public static boolean in(Term t, Domain d) {
		for(Domain td: t.getDomain()) {
			if(contains(td, d)) {
				return true;
			}
		}
		return false;
	}
	
	// Return true if d0 is a subset of d1 or d0 equals d1.
	public static boolean contains(Domain d0, Domain d1) {
		if(d0.equals(d1)) {
			return true;
		} else if (d0.parent != null) {
			return Domain.contains(d0.parent, d1);
		}
		return false;
	}
	

}
