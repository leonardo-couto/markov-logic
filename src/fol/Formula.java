package fol;

import java.util.List;
import java.util.Map;
import java.util.Set;

import fol.database.Database;

public interface Formula {
	
	public List<Atom> getAtoms();
	public List<FormulaComponent> getComponents();
	public Set<Predicate> getPredicates();
	public boolean getValue(Database db);
	public boolean getValue(Database db, Map<Variable,Constant> groundings);
	public Set<Variable> getVariables();
	public Formula ground(Map<Variable,Constant> groundings);
	public boolean hasPredicate(Predicate p);
	public boolean isGrounded();
	public int length();
	public Formula replace(Atom original, Literal replacement);
	public CNF toCNF();
	public double trueCount(Database db);

}
