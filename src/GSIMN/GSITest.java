package GSIMN;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import stat.IndependenceTest;
import stat.RandomVariable;

public class GSITest<RV extends RandomVariable<RV>> implements IndependenceTest<RV> {
	// Maps a tuple of RandomVariable (order independent) into a Map of tests results
	// (X,Y) -> {(Z0 -> true/false), (Z1 -> true/false), ...}
	private final Map<Set<RV>, Map<Collection<RV>, Boolean>> knowledgeBase;
	private final IndependenceTest<RV> itest;
	private final Map<Set<RV>,Double> pvalueMap;
	
	public GSITest(IndependenceTest<RV> itest) {
		this.knowledgeBase = new HashMap<Set<RV>, Map<Collection<RV>,Boolean>>();
		this.itest = itest;
		pvalueMap = new HashMap<Set<RV>, Double>();
	}
	
	private Set<RV> getXYSet(RV x, RV y) {
		Set<RV> xy = new HashSet<RV>();
		xy.add(x);
		xy.add(y);
		return xy;
	}
	
	public void addPValue(RV x, RV y, double pvalue) {
		Set<RV> xy = this.getXYSet(x, y);
		this.pvalueMap.put(xy, pvalue);
	}
	
	/**
	 * Tests whether X and Y are independent given Z.
	 * @param x RandomVariable to be tested
	 * @param y RandomVariable to be tested
	 * @param z Evidence, Collection of RandonVariable.
	 * @param F Collection of RandomVariable known to be independent of X
	 * @param T Collection of RandomVariable known to be dependent of X
	 * @return true if X and Y are independent given Z
	 */
	public boolean test(RV x, RV y, Collection<RV> z, Collection<RV> F, Collection<RV> T) {
		Set<RV> xy = this.getXYSet(x, y);
		
		// check the p-value between 
		if (this.pvalueMap.containsKey(xy)) {
			if (itest.test(this.pvalueMap.get(xy))) {
				addKB(xy, z, true);
				imprime(x, y, z, true, "teste estatistico");
				return true;
			}
			// else
			if (z.isEmpty()) {
			  addKB(xy, z, false);
			  imprime(x, y, z, false, "teste estatistico");
			  return false;
			}
		}
		
		// Check the KB for this test
		if(this.knowledgeBase.containsKey(xy)) {
			Map<Collection<RV>,Boolean> kb = this.knowledgeBase.get(xy);
			for (Collection<RV> A : this.knowledgeBase.get(xy).keySet()) {
				if (A.containsAll(z)) {
					if (z.containsAll(A)) {
						return kb.get(A).booleanValue();
					}
				}
			}
		}
		
		// Attempt to infer dependence by propagation.
		if (T.contains(y)) {
			addKB(xy, z, false);
			imprime(x, y, z, false, "propagation");
			return false;
		}
		// Attempt to infer independence by propagation.
		if (F.contains(y)) {
			addKB(xy, z, true);
			imprime(x, y, z, true, "propagation");
			return true;
		}
		// Attempt to infer dependence by Strong Union.
		if(this.knowledgeBase.containsKey(xy)) {
			for (Collection<RV> A : this.knowledgeBase.get(xy).keySet()) {
				if(!this.knowledgeBase.get(xy).get(A).booleanValue() && A.containsAll(z)) {
					addKB(xy, z, false);
					imprime(x, y, z, false, "Strong Union");
					return false;
				}
			}
		}
		// Attempt to infer dependence by the D-triangle rule.
		for (RV w : z) {
			Set<RV> xw = this.getXYSet(x, w);
			Set<RV> wy = this.getXYSet(w, y);
			if(this.knowledgeBase.containsKey(xw) && this.knowledgeBase.containsKey(wy)) {
				for (Collection<RV> A : this.knowledgeBase.get(xw).keySet()) {
					if(!this.knowledgeBase.get(xw).get(A).booleanValue() &&  A.containsAll(z)) {
						for (Collection<RV> B : this.knowledgeBase.get(wy).keySet()) {
							if(!this.knowledgeBase.get(wy).get(B).booleanValue() && B.containsAll(z)) {
								Set<RV> C = new HashSet<RV>(A);
								C.retainAll(B);
								addKB(xy, C, false);
								addKB(xy, z, false);
								imprime(x, y, z, false, "D-triangle");
								return false;
							}
						}
					}
				}
			}
		}
		// Attempt to infer independence by Strong Union.
		if(this.knowledgeBase.containsKey(xy)) {
			for (Collection<RV> A : this.knowledgeBase.get(xy).keySet()) {
				if(this.knowledgeBase.get(xy).get(A).booleanValue() && z.containsAll(A)) {
					addKB(xy, z, true);
					imprime(x, y, z, true, "Strong Union");
					return true;
				}
			}
		}
		// Attempt to infer independence by the I-triangle rule.
		for (RV w : z) {
			Set<RV> xw = this.getXYSet(x, w);
			Set<RV> wy = this.getXYSet(w, y);
			if(this.knowledgeBase.containsKey(xw) && this.knowledgeBase.containsKey(wy)) {
				for (Collection<RV> A : this.knowledgeBase.get(xw).keySet()) {
					if(this.knowledgeBase.get(xw).get(A).booleanValue() &&  z.containsAll(A)) {
						for (Collection<RV> B : this.knowledgeBase.get(wy).keySet()) {
							if(!this.knowledgeBase.get(wy).get(B).booleanValue() && B.containsAll(A)) {
								addKB(xy, A, true);
								addKB(xy, z, true);
								imprime(x, y, z, true, "I-triangle");
								return true;
							}
						}
					}
				}
			}
		}
		if(itest.test(x, y, new HashSet<RV>(z))) {
			addKB(xy, z, true);
			imprime(x, y, z, true, "teste estatistico");
			return true;
		}
		// else
		addKB(xy, z, false);
		imprime(x, y, z, false, "teste estatistico");
		return false;
	}
	
	private void imprime(RV X, RV Y, Collection<RV> Z, boolean b, String desc) {
		// TODO: REMOVER METODO!!
		System.out.println("I( " + X.toString() + ", " + Y.toString() + "| " + Arrays.deepToString(Z.toArray()) + ") " + b + ". Por: " + desc);
	}
	
	private void addKB(Set<RV> xy, Collection<RV> Z, boolean b) {
		if(!this.knowledgeBase.containsKey(xy)) {
			this.knowledgeBase.put(xy, new HashMap<Collection<RV>, Boolean>());
		}
		this.knowledgeBase.get(xy).put(new ArrayList<RV>(Z), new Boolean(b));
	}

	@Override
	public boolean test(RV x, RV y, Set<RV> z) {
		return itest.test(x, y, z);
	}

	@Override
	public boolean test(double pvalue) {
		return itest.test(pvalue);
	}

	@Override
	public double pvalue(RV x, RV y) {
		Set<RV> xy = this.getXYSet(x, y);
		// if pvalue was already evaluated
		if (this.pvalueMap.containsKey(xy)) {
			return this.pvalueMap.get(xy);
		}
		double d = itest.pvalue(x, y);
		this.addPValue(x, y, d);
		return d;
	}
}
