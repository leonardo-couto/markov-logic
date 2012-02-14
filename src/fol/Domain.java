package fol;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Represents the domain for <code>Terms</code>. eg. the constants Ana and Bob
 * may belong to a domain named person. The variable x may represent a movie
 * in a domain named movie.
 *
 */
public class Domain extends HashSet<Constant> {
	
	private static final long serialVersionUID = -6910763430777620405L;
	
	private final String name;
	private final List<Function> functions;
	private final List<Variable> variables;
	private final Domain parent;
	private int varcount;
	
	public static final Domain universe = new Domain("universe", null);

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
		this.varcount++;
		return new Variable("v" + this.name + this.varcount, this);
	}
	
	public Constant newConstant() {
		return new Constant("c" + this.name + this.size(), this);
	}

	/**
	 * @return the functions
	 */
	public List<Function> getFunctions() {
		return this.functions;
	}

	/**
	 * @return the variables in this domain
	 */
	public List<Variable> getVariables() {
		return this.variables;
	}

	/**
	 * @return the parent
	 */
	public Domain getParent() {
		return this.parent;
	}
	
	public boolean add(Function f) {
		return this.functions.add(f);
	}
	
	public boolean add(Variable v) {
		return this.variables.add(v);
	}
	
	public boolean remove(Function f) {
		return this.functions.remove(f);
	}
	
	public boolean remove(Variable v) {
		return this.functions.remove(v);
	}
	
	@Override
	public String toString() {
		return this.name;
	}
	
	/**
	 * Checks if the Term t are in Domain d
	 * @param t Term
	 * @param d Domain
	 * @return returns true if Term t is in Domain d
	 */
	public static boolean in(Term t, Domain d) {
		Domain td = t.getDomain();
		return contains(td, d);
	}
	
	/**
	 * Checks if d1 contains/equals d0
	 * @param d0
	 * @param d1
	 * @return true if d0 is a subset of d1 or d0 equals d1.
	 */
	public static boolean contains(Domain d0, Domain d1) {
		return d0 == d1 || (d0 != null && Domain.contains(d0.parent, d1));
	}
	
	@Override
	public int hashCode() {
		return this.name.hashCode();		
	}
	

}
