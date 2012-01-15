package fol;

public class Literal extends OldFormula {
	
	private final OldAtom atom;
	private final boolean negated;
	
	private Literal(OldAtom atom, boolean negated) {
		this.atom = atom;
		this.negated = negated;
	}
	
	
}
