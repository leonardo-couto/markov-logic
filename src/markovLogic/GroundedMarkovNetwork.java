package markovLogic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import fol.Atom;
import fol.WeightedFormula;
import fol.database.Database;

public class GroundedMarkovNetwork {
	
	private final List<WeightedFormula<?>> formulas;
	private List<Atom> groundings;
	
	public GroundedMarkovNetwork(List<WeightedFormula<?>> formulas) {
		this.formulas = formulas;
		this.groundings = null;
	}
	
	GroundedMarkovNetwork(List<WeightedFormula<?>> formulas, Collection<Atom> groundings) {
		this.formulas = formulas;
		this.groundings = new ArrayList<Atom>(groundings);
	}
	
	/**
	 * @return a List of all grounded atoms in the mln
	 */
	public List<Atom> getGroundings() {
		if (this.groundings == null) this.groundings = new ArrayList<Atom>(); // TODO implementar
		return new ArrayList<Atom>(this.groundings);
	}
	
	public List<WeightedFormula<?>> getformulas() {
		return this.formulas;
	}
	
	/**
	 * Get the sum of weights of the satisfied formulas given
	 * the possible world <code>world</code>
	 * @return the sum of weights of all formulas that are true
	 *         in the given world.
	 */
	public double sumWeights(Database world) {
		double sum = 0d;
		for (WeightedFormula<?> wf : this.formulas) {
			boolean value = wf.getFormula().getValue(world);
			if (value) sum += wf.getWeight();			
		}
		return sum;
	}
	
}
