package formulaLearner;

import java.util.Collection;
import java.util.Set;

import fol.Atom;

public interface FormulaLearnerBuilder {
	
	public FormulaLearner build();
	public FormulaLearnerBuilder setAtoms(Collection<Atom> atoms);
	public Set<Atom> getAtoms();

}
