package GSIMN;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import stat.IndependenceTest;

public class GSITest<T> implements IndependenceTest<T> {
	
	public static PrintStream out = System.out;
	
	// Maps a tuple of RandomVariable (order independent) into a Map of tests results
	// (X,Y) -> {(Z0 -> true/false), (Z1 -> true/false), ...}
	private final Map<Set<T>, Map<Collection<T>, Boolean>> knowledgeBase;
	private final IndependenceTest<T> itest;
	private final Map<Set<T>,Double> pvalueMap;
	
	public GSITest(IndependenceTest<T> itest) {
		this.knowledgeBase = new HashMap<Set<T>, Map<Collection<T>,Boolean>>();
		this.itest = itest;
		pvalueMap = new HashMap<Set<T>, Double>();
	}
	
	private Set<T> getXYSet(T x, T y) {
		Set<T> xy = new HashSet<T>();
		xy.add(x);
		xy.add(y);
		return xy;
	}
	
	public void addPValue(T x, T y, double pvalue) {
		Set<T> xy = this.getXYSet(x, y);
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
	public boolean test(T x, T y, Collection<T> z, Collection<T> F, Collection<T> T) {
		Set<T> xy = this.getXYSet(x, y);
		
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
			Map<Collection<T>,Boolean> kb = this.knowledgeBase.get(xy);
			for (Collection<T> A : this.knowledgeBase.get(xy).keySet()) {
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
			for (Collection<T> A : this.knowledgeBase.get(xy).keySet()) {
				if(!this.knowledgeBase.get(xy).get(A).booleanValue() && A.containsAll(z)) {
					addKB(xy, z, false);
					imprime(x, y, z, false, "Strong Union");
					return false;
				}
			}
		}
		// Attempt to infer dependence by the D-triangle rule.
		for (T w : z) {
			Set<T> xw = this.getXYSet(x, w);
			Set<T> wy = this.getXYSet(w, y);
			if(this.knowledgeBase.containsKey(xw) && this.knowledgeBase.containsKey(wy)) {
				for (Collection<T> A : this.knowledgeBase.get(xw).keySet()) {
					if(!this.knowledgeBase.get(xw).get(A).booleanValue() &&  A.containsAll(z)) {
						for (Collection<T> B : this.knowledgeBase.get(wy).keySet()) {
							if(!this.knowledgeBase.get(wy).get(B).booleanValue() && B.containsAll(z)) {
								Set<T> C = new HashSet<T>(A);
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
			for (Collection<T> A : this.knowledgeBase.get(xy).keySet()) {
				if(this.knowledgeBase.get(xy).get(A).booleanValue() && z.containsAll(A)) {
					addKB(xy, z, true);
					imprime(x, y, z, true, "Strong Union");
					return true;
				}
			}
		}
		// Attempt to infer independence by the I-triangle rule.
		for (T w : z) {
			Set<T> xw = this.getXYSet(x, w);
			Set<T> wy = this.getXYSet(w, y);
			if(this.knowledgeBase.containsKey(xw) && this.knowledgeBase.containsKey(wy)) {
				for (Collection<T> A : this.knowledgeBase.get(xw).keySet()) {
					if(this.knowledgeBase.get(xw).get(A).booleanValue() &&  z.containsAll(A)) {
						for (Collection<T> B : this.knowledgeBase.get(wy).keySet()) {
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
		if(itest.test(x, y, new HashSet<T>(z))) {
			addKB(xy, z, true);
			imprime(x, y, z, true, "teste estatistico");
			return true;
		}
		// else
		addKB(xy, z, false);
		imprime(x, y, z, false, "teste estatistico");
		return false;
	}
	
	private void imprime(T X, T Y, Collection<T> Z, boolean b, String desc) {
		// TODO: REMOVER METODO!!
		out.println("I( " + X.toString() + ", " + Y.toString() + "| " + Arrays.deepToString(Z.toArray()) + ") " + b + ". Por: " + desc);
	}
	
	private void addKB(Set<T> xy, Collection<T> Z, boolean b) {
		if(!this.knowledgeBase.containsKey(xy)) {
			this.knowledgeBase.put(xy, new HashMap<Collection<T>, Boolean>());
		}
		this.knowledgeBase.get(xy).put(new ArrayList<T>(Z), new Boolean(b));
	}

	@Override
	public boolean test(T x, T y, Set<T> z) {
		return itest.test(x, y, z);
	}

	@Override
	public boolean test(double pvalue) {
		return itest.test(pvalue);
	}

	@Override
	public double pvalue(T x, T y) {
		Set<T> xy = this.getXYSet(x, y);
		// if pvalue was already evaluated
		if (this.pvalueMap.containsKey(xy)) {
			return this.pvalueMap.get(xy);
		}
		double d = itest.pvalue(x, y);
		this.addPValue(x, y, d);
		return d;
	}
}
