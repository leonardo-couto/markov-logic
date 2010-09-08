package weightLearner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import math.RnToRFunction;
import math.RnToRnFunction;

import util.ListPointer;
import util.Sampler;
import fol.Atom;
import fol.Constant;
import fol.Formula;
import fol.Predicate;
import fol.Term;
import fol.Variable;

// Weighted Pseudo Log Likelihood
public class WPLL {
	
	private Set<Predicate> predicates;
	private List<Formula> formulas;
	private Map<Predicate, Long> inversePllWeight; // Not formulas weight! TODO: citar referencia
	private Sampler sampler;
	private Map<Predicate, DataCount> dataCounts;
	private double[] grad;
 
	public WPLL(Set<Predicate> predicates) {
		init(predicates, new ArrayList<Formula>());
	}	
	
	public WPLL(Set<Predicate> predicates, List<Formula> formulas) {
		init(predicates, formulas);
		for (Predicate p : predicates) {
			updateCounts(p);
		}
	}
	
	public WPLL(WPLL wpll) {
		this.predicates = new HashSet<Predicate>(wpll.predicates);
		this.formulas = new ArrayList<Formula>(wpll.formulas);
		this.inversePllWeight = new HashMap<Predicate, Long>(wpll.inversePllWeight);
		this.sampler = new Sampler(wpll.sampler);
		this.dataCounts = new HashMap<Predicate, DataCount>(2*wpll.dataCounts.size());
		for (Predicate p : wpll.dataCounts.keySet()) {
			this.dataCounts.put(p, new DataCount(wpll.dataCounts.get(p)));
		}
	}
	
	private void init(Set<Predicate> predicates, List<Formula> formulas) {
		this.predicates = new HashSet<Predicate>(predicates);
		this.formulas = new ArrayList<Formula>(formulas);
		this.sampler = new Sampler(20);
		this.dataCounts = new HashMap<Predicate, DataCount>();
		this.inversePllWeight = new HashMap<Predicate, Long>();
		this.grad = new double[0];
		defaultWeights();
	}
	
	private double[] lastCall;
	
	@Override
	public double f(double[] x) {
		return getScore(x);
	}
	
	@Override
	public double[] g(double[] x) {
		if (Arrays.equals(x, this.lastCall)) {
			return this.grad;
		} else {
			this.getScore(x);
			return this.grad;
		}
	}
	
	public double getScore(double[] x) {
		lastCall = Arrays.copyOf(x, x.length);
		
		if (!(formulas.size() == x.length)) {
			throw new RuntimeException("Different number of formulas and weights");
		}
		double wpll = 0;
		

		Map<Formula, Double> wf = new HashMap<Formula, Double>();
		Map<Formula, Integer> idx = new HashMap<Formula, Integer>();
		int i = 0;
		for (Formula f: formulas) {
			wf.put(f, new Double(x[i]));
			idx.put(f, new Integer(i));
			i++;
		}
		
		grad = new double[x.length];
		Arrays.fill(grad, 0);
		for (Predicate p : predicates) {
			updateCounts(p);
			wpll = wpll + predicateWPll(p, wf, idx);
		}
		return wpll;
	}
	
	public void addFormula(Formula f) {
		formulas.add(f);
		for (Predicate p : predicates) {
			updateCounts(p);
		}
	}
	
	public boolean removeFormula(Formula f) {
		if (formulas.contains(f)) {
			formulas.remove(f);
			for (Predicate p : predicates) {
				if(f.hasPredicate(p)) {
					DataCount dc = dataCounts.get(p);
					dc.formulas.remove(f);
					for (Atom a : dc.keySet()) {
						dc.get(a).remove(f);
					}
				}
			}
			return true;
		}
		return false;
	}
	
	private void updateCounts(Predicate p) {
			
		// Use only formulas where the Predicate p appears.
		
		Set<Formula> pFormulas = new HashSet<Formula>();
		for (Formula f: formulas) {
			if (f.hasPredicate(p)) {
				pFormulas.add(f);
			}
		}
		
		DataCount dc;
		Set<Atom> sample;
		boolean hasCounts = false;
		
		if (dataCounts.containsKey(p)) {
			// Some counts have already been done.
			hasCounts = true;
			dc = dataCounts.get(p);
			sample = dc.keySet();
			pFormulas.removeAll(dc.formulas);
			dc.formulas.addAll(pFormulas);
			
		} else {
			// No counts done for this predicate
			sample = sampler.samplePredicate(p);
			dc = new DataCount(p.totalGroundsNumber(), sample.size(), pFormulas);
			
			// Store counts for this Predicate
			dataCounts.put(p, dc);
		}
		
		if (pFormulas.isEmpty()) {
			// Predicate has no Formula counts that has not been calculated.
			return; 
		}
		
		if (!hasCounts) {
			for(Atom a : sample) {
				dc.put(a, new HashMap<Formula, Data>());
			}
		}
		
		Atom[] atoms = sample.toArray(new Atom[sample.size()]);
		
		for (Formula f : pFormulas) {
			
			updateTFCounts(p, f, dc, atoms);

		}
		
	}
	
	private double predicateWPll(Predicate p, Map<Formula, Double> wf, Map<Formula, Integer> idx) {
		
		double predicatePll = 0.0d;
		DataCount d = dataCounts.get(p);
		double[] pGrad = new double[wf.size()];
		Arrays.fill(pGrad, 0.0d);
		
		for (Atom a : d.keySet()) {
			// Atom's probability of being equals atom.value (from database) given its Markov Blanket.
			double atomProb = 0.0d;

			Map<Formula, Data> map = d.get(a);
			double d0 = 0.0d;
			double d1 = 0.0d;
			for (Formula f : map.keySet()) {
				// TODO: MODIFICAR, pensar o que fazer com os NaN counts. Talvez ver a proporcao e normalizar.
				d0 = d0 + (((double) map.get(f).trueCount.trueCounts) * wf.get(f).doubleValue());
				d1 = d1 + (((double) map.get(f).falseCount.trueCounts) * wf.get(f).doubleValue());
			}
			d0 = Math.exp(d0);
			d1 = Math.exp(d1);
			
			// TODO: Explicar conta:
			boolean abs = true;
			double expr = a.value - (d0/(d0+d1));
			if (Double.compare(expr, 0.0d) > 0) {
				abs = false;
				atomProb = 1.0d - expr;
			} else {
				atomProb = 1.0d + expr;
			}
			
			boolean zero = false;
			if (Double.compare(atomProb, 0.000001) < 0) {
				zero = true;
				atomProb = 0.000001;
			}
			predicatePll = predicatePll + Math.log(atomProb);
			
			for (Formula f : map.keySet()) {
				// TODO: MODIFICAR, pensar o que fazer com os NaN counts. Talvez ver a proporcao e normalizar.
				double tc = (double) map.get(f).trueCount.trueCounts;
				double fc = (double) map.get(f).falseCount.trueCounts;
				int i = idx.get(f).intValue();
				
				if (zero) {
					if (abs) {
						pGrad[i] = pGrad[i] + fc - tc;
					} else {
						pGrad[i] = pGrad[i] + tc - fc;
					}
				} else {
					if (abs) {
						pGrad[i] = pGrad[i] + (d0*d1*(fc-tc)/(Math.pow((d0+d1), 2)*atomProb));
					} else {
						pGrad[i] = pGrad[i] + (d0*d1*(tc-fc)/(Math.pow((d0+d1), 2)*atomProb));
					}
				}
			}
			
		}
		
		if(!(d.totalGrounds == (long) d.sampledGrounds)) {
			
			// The default behavior is inversePllWheight == d.totalGrounds, so test it.
			if (inversePllWeight.get(p).longValue() == d.totalGrounds) {
				// Avoid multiplying and dividing by the same number.
				for (int i = 0; i < pGrad.length; i++) {
					grad[i] = grad[i] + (pGrad[i] / (double) d.sampledGrounds);
				}
				return (predicatePll / (double) d.sampledGrounds);
			}
			for (int i = 0; i < pGrad.length; i++) {
				grad[i] = grad[i] + (pGrad[i] * (double) d.totalGrounds) / (double) d.sampledGrounds;
			}
			predicatePll = (predicatePll * (double) d.totalGrounds) / (double) d.sampledGrounds;
		}
		
		for (int i = 0; i < pGrad.length; i++) {
			grad[i] = grad[i] + (pGrad[i] / (double) inversePllWeight.get(p));
		}
		return (predicatePll / (double) inversePllWeight.get(p));
	}
	
	private void defaultWeights() {
		// populate inversePllWeight with the number of groundings for each predicate. 
		for (Predicate p : predicates) {
			inversePllWeight.put(p, p.totalGroundsNumber());
		}
	}
	
	private void updateTFCounts(Predicate p, Formula f, DataCount dc, Atom[] atoms) {
		
		Formula fCopy = f.copy();
		ListPointer<Formula> pA = null;
		List<ListPointer<Formula>> apl = fCopy.getAtoms();
		for (ListPointer<Formula> pointer : apl) {
			if (((Atom) pointer.get()).predicate.equals(p)) {
				pA = pointer;
				break;
			}
		}
		
		if (pA == null) {
			if (fCopy instanceof Atom) {
				for (Atom a : atoms) {
					dc.get(a).put(f, new Data(new FormulaCount(1, 1, 0), new FormulaCount(0, 1, 0)));
				}
				return;
			}
			throw new RuntimeException("Formula \"" + f.toString() + 
					"\" contains no Predicate \"" + p.toString() + "."); 
			//		"\" with only Variables to be grounded.");
		}
		
		Set<Variable> vset = fCopy.getVariables();
		Variable[] var = new Variable[vset.size()];
		int termsLength = p.getDomains().size();
		Constant[] cons = new Constant[var.length];
		List<Term> terms = Arrays.asList(((Atom) pA.get()).terms);
		for (int i = 0; i < termsLength; i++) {
			var[i] = (Variable) terms.get(i);
		}

		int idx = termsLength;
		for (Variable v : vset) {
			if (!terms.contains(v)) {
				var[idx] = v;
				idx++;
			}
		}
		
		Constant[][] constants = new Constant[var.length][];
		long n = 1;
		int[] length = new int[var.length];
		for (int i = termsLength; i < var.length; i++) {
			Set<Constant> cset = var[i].getConstants();
			constants[i] = cset.toArray(new Constant[cset.size()]);
			length[i] = constants[i].length;
			n = n * length[i];
		}
		
		for (Atom a : atoms) {
			
			FormulaCount trueCount = new FormulaCount();
			FormulaCount falseCount = new FormulaCount();
			
			Atom trueAtom = new Atom(a, 1.0d);
			Atom falseAtom = new Atom(a, 0.0d);
			
			for (int i = 0; i < a.terms.length; i++) {
				cons[i] = (Constant) a.terms[i];
			}

			// TODO: criar um random, pegar algumas amostras, colocar um teste de convergencia.
			//       se convergiu, multiplicar a proporçao pelo total e retornar, caso contrario
			//       pegar mais amostras. 50 por vez?.
			Random[] r = new Random[var.length];
			for (int j = termsLength; j < var.length; j++) {
				r[j] = new Random();
			}
			for (int i = 0; i < Math.min(50, n); i++) {
				for (int j = termsLength; j < var.length; j++) {
					cons[j] = constants[j][r[j].nextInt(length[j])];
				}
				
				// True counts				
				for (ListPointer<Formula> pointer : apl) {
					pointer.set(pointer.original.replaceVariables(var, cons));
					if (pointer.get().equals(trueAtom)) {
						pointer.set(trueAtom);
					}
				}
				double d = fCopy.getValue();
				if (Double.isNaN(d)) {
					trueCount.addNaNCount();
				} else {
					trueCount.addTrueCounts(d);
				}
				
				// False counts
				for (ListPointer<Formula> pointer : apl) {
					if (pointer.get().equals(trueAtom)) {
						pointer.set(falseAtom);
					}
				}
				d = fCopy.getValue();
				if (Double.isNaN(d)) {
					falseCount.addNaNCount();
				} else {
					falseCount.addTrueCounts(d);
				}				
			}
			
			// TODO: ver proporcao de samples, multiplicar.
			// fazer as contas, ver se é realmente nescessario.
			
			dc.get(a).put(f, new Data(trueCount, falseCount));
			
		}
	}

	
	private class Data {
		public final FormulaCount trueCount;
		public final FormulaCount falseCount;
		
		public Data(FormulaCount trueCount, FormulaCount falseCount) {
			this.trueCount = trueCount;
			this.falseCount = falseCount;
		}
	}
	
	private class DataCount extends HashMap<Atom,Map<Formula,Data>> {
		
		private static final long serialVersionUID = 6082955694768695935L;
		public final long totalGrounds;
		public final int sampledGrounds;
		public final Set<Formula> formulas;
		
		public DataCount(DataCount old) {
			super();
			this.totalGrounds = old.totalGrounds;
			this.sampledGrounds = old.sampledGrounds;
			this.formulas = new HashSet<Formula>(old.formulas);
			for (Atom a : old.keySet()) {
				this.put(a, new HashMap<Formula, Data>(old.get(a)));
			}
		}
		
		public DataCount(long totalGrounds, int sampledGrounds, Set<Formula> formulas) {
			super();
			this.totalGrounds = totalGrounds;
			this.sampledGrounds = sampledGrounds;
			this.formulas = formulas;
		}
		
	}
	
	/**
	 * @return the predicates
	 */
	public Set<Predicate> getPredicates() {
		return new HashSet<Predicate>(predicates);
	}

	/**
	 * @return the formulas
	 */
	public List<Formula> getFormulas() {
		return new ArrayList<Formula>(formulas);
	}

	/**
	 * @return the sampler
	 */
	public Sampler getSampler() {
		return sampler;
	}

	/**
	 * @param sampler the sampler to set
	 */
	public void setSampler(Sampler sampler) {
		this.sampler = sampler;
	}

	/**
	 * @return the dataCounts
	 */
	public Map<Predicate, DataCount> getDataCounts() {
		return dataCounts;
	}

}