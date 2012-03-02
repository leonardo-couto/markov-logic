package fol;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class WeightedFormula<T extends Formula> {

	private final T formula;
	private final double weight;
	
	public WeightedFormula(T formula, double weight) {
		this.formula = formula;
		this.weight = weight;
	}
	
	public T getFormula() {
		return this.formula;
	}
	
	public double getWeight() {
		return this.weight;
	}
	
	public static <U extends Formula> List<WeightedFormula<U>> 
			toWeightedFormulas(List<? extends U> formulas, double[] weights) {
		
		List<WeightedFormula<U>> list = new ArrayList<WeightedFormula<U>>(weights.length);
		for (int i = 0; i < weights.length; i++) {
			list.add(new WeightedFormula<U>(formulas.get(i), weights[i]));
		}
		return list;
	}
	
	public static <U extends Formula> FormulasAndWeights<U> 
		toFormulasAndWeights(List<? extends WeightedFormula<? extends U>> wfs) {
		
		double[] weights = new double[wfs.size()];
		List<U> formulas = new ArrayList<U>(weights.length);
		int i = 0;
		for (WeightedFormula<? extends U> f : wfs) {
			weights[i] = f.weight;
			formulas.add(f.formula);
			i++;
		}
		return new FormulasAndWeights<U>(formulas, weights);
	}

	@Override
	public String toString() {
		return Double.toString(this.weight) + " : " + this.formula.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime + ((formula == null) ? 0 : formula.hashCode());
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
		WeightedFormula<?> other = (WeightedFormula<?>) obj;
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

	public static final class FormulasAndWeights<U extends Formula> {
		public final List<U> formulas;
		public final double[] weights;
		
		public FormulasAndWeights(List<U> formulas, double[] weights) {
			this.formulas = formulas;
			this.weights = weights;
		}
 	}
	
	public static class AbsoluteWeightComparator implements Comparator<WeightedFormula<?>> {
		
		private final boolean inverse;
		
		public AbsoluteWeightComparator() {
			this(false);
		}
		
		public AbsoluteWeightComparator(boolean inverse) {
			this.inverse = inverse;
		}

		@Override
		public int compare(WeightedFormula<?> o1, WeightedFormula<?> o2) {
			double d1 = Math.abs(o1.getWeight());
			double d2 = Math.abs(o2.getWeight());
			return this.inverse ? Double.compare(d2, d1) : Double.compare(d1, d2);
		}
		
	}

}
