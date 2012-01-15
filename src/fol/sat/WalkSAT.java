package fol.sat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import fol.Atom;
import fol.Formula;

public class WalkSAT {
	
	private final List<Formula> formulas;
	
	public WalkSAT(Collection<Formula> formulas) {
		this.formulas = new ArrayList<Formula>(formulas);
	}
	
	public void addConstraint(Atom a, boolean value) {
		
	}

}
