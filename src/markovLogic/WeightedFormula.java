package markovLogic;

import java.util.ArrayList;
import java.util.List;

import fol.Formula;

public class WeightedFormula {

	private final Formula formula;
	private final double weight;
	
	public WeightedFormula(Formula formula, double weight) {
		this.formula = formula;
		this.weight = weight;
	}
	
	public Formula getFormula() {
		return this.formula;
	}
	
	public double getWeight() {
		return this.weight;
	}
	
	public static List<WeightedFormula> toWeightedFormulas(List<Formula> formulas, double[] weights) {
		ArrayList<WeightedFormula> list = new ArrayList<WeightedFormula>(weights.length);
		for (int i = 0; i < weights.length; i++) {
			list.add(new WeightedFormula(formulas.get(i), weights[i]));
		}
		return list;
	}
	
	public static FormulasAndWeights toFormulasAndWeights(List<? extends WeightedFormula> wfs) {
		double[] weights = new double[wfs.size()];
		ArrayList<Formula> formulas = new ArrayList<Formula>(weights.length);
		int i = 0;
		for (WeightedFormula f : wfs) {
			weights[i] = f.weight;
			formulas.add(f.formula);
			i++;
		}
		return new FormulasAndWeights(formulas, weights);
	}

	public static final class FormulasAndWeights {
		public final List<Formula> formulas;
		public final double[] weights;
		
		public FormulasAndWeights(List<Formula> formulas, double[] weights) {
			this.formulas = formulas;
			this.weights = weights;
		}
 	}
	
	@Override
	public String toString() {
		return Double.toString(this.weight) + " : " + this.formula.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((formula == null) ? 0 : formula.hashCode());
		long temp;
		temp = Double.doubleToLongBits(weight);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WeightedFormula other = (WeightedFormula) obj;
		if (formula == null) {
			if (other.formula != null)
				return false;
		} else if (!formula.equals(other.formula))
			return false;
		if (Double.doubleToLongBits(weight) != Double
				.doubleToLongBits(other.weight))
			return false;
		return true;
	}

}
