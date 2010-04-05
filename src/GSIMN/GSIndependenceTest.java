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

public class GSIndependenceTest<RV extends RandomVariable<RV>> {
	// Maps a tuple of RandomVariable (order independent) into a Map of tests results
	// (X,Y) -> {(Z0 -> true/false), (Z1 -> true/false), ...}
	private Map<Set<RV>, Map<Collection<RV>, Boolean>> knowledgeBase;
	private IndependenceTest<RV> itest;
	
	public GSIndependenceTest(IndependenceTest<RV> itest) {
		this.knowledgeBase = new HashMap<Set<RV>, Map<Collection<RV>,Boolean>>();
		this.itest = itest;
	}
	
	/**
	 * Tests whether X and Y are independent given Z.
	 * @param X RandomVariable to be tested
	 * @param Y RandomVariable to be tested
	 * @param Z Evidence, Collection of RandonVariable.
	 * @param F Collection of RandomVariable known to be independent of X
	 * @param T Collection of RandomVariable known to be dependent of X
	 * @return true if X and Y are independent given Z
	 */
	public boolean test(RV X, RV Y, Collection<RV> Z, Collection<RV> F, Collection<RV> T) {
		Set<RV> xy = new HashSet<RV>();
		xy.add(X);
		xy.add(Y);
		
		// Check the KB for this test
		if(this.knowledgeBase.containsKey(xy)) {
			Map<Collection<RV>,Boolean> kb = this.knowledgeBase.get(xy);
			for (Collection<RV> A : this.knowledgeBase.get(xy).keySet()) {
				if (A.containsAll(Z)) {
					if (Z.containsAll(A)) {
						return kb.get(A).booleanValue();
					}
				}
			}
		}
		
		// Attempt to infer dependence by propagation.
		if (T.contains(Y)) {
			addKB(xy, Z, false);
			imprime(X, Y, Z, false, "propagation");
			return false;
		}
		// Attempt to infer independence by propagation.
		if (F.contains(Y)) {
			addKB(xy, Z, true);
			imprime(X, Y, Z, true, "propagation");
			return true;
		}
		// Attempt to infer dependence by Strong Union.
		if(this.knowledgeBase.containsKey(xy)) {
			for (Collection<RV> A : this.knowledgeBase.get(xy).keySet()) {
				if(!this.knowledgeBase.get(xy).get(A).booleanValue() && A.containsAll(Z)) {
					addKB(xy, Z, false);
					imprime(X, Y, Z, false, "Strong Union");
					return false;
				}
			}
		}
		// Attempt to infer dependence by the D-triangle rule.
		for (RV W : Z) {
			Set<RV> xw = new HashSet<RV>();
			xw.add(X);
			xw.add(W);
			Set<RV> wy = new HashSet<RV>();
			wy.add(W);
			wy.add(Y);			
			if(this.knowledgeBase.containsKey(xw) && this.knowledgeBase.containsKey(wy)) {
				for (Collection<RV> A : this.knowledgeBase.get(xw).keySet()) {
					if(!this.knowledgeBase.get(xw).get(A).booleanValue() &&  A.containsAll(Z)) {
						for (Collection<RV> B : this.knowledgeBase.get(wy).keySet()) {
							if(!this.knowledgeBase.get(wy).get(B).booleanValue() && B.containsAll(Z)) {
								Set<RV> C = new HashSet<RV>(A);
								C.retainAll(B);
								addKB(xy, C, false);
								addKB(xy, Z, false);
								imprime(X, Y, Z, false, "D-triangle");
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
				if(this.knowledgeBase.get(xy).get(A).booleanValue() && Z.containsAll(A)) {
					addKB(xy, Z, true);
					imprime(X, Y, Z, true, "Strong Union");
					return true;
				}
			}
		}
		// Attempt to infer independence by the I-triangle rule.
		for (RV W : Z) {
			Set<RV> xw = new HashSet<RV>();
			xw.add(X);
			xw.add(W);
			Set<RV> wy = new HashSet<RV>();
			wy.add(W);
			wy.add(Y);			
			if(this.knowledgeBase.containsKey(xw) && this.knowledgeBase.containsKey(wy)) {
				for (Collection<RV> A : this.knowledgeBase.get(xw).keySet()) {
					if(this.knowledgeBase.get(xw).get(A).booleanValue() &&  Z.containsAll(A)) {
						for (Collection<RV> B : this.knowledgeBase.get(wy).keySet()) {
							if(!this.knowledgeBase.get(wy).get(B).booleanValue() && B.containsAll(A)) {
								addKB(xy, A, true);
								addKB(xy, Z, true);
								imprime(X, Y, Z, true, "I-triangle");
								return true;
							}
						}
					}
				}
			}
		}
		if(itest.test(X, Y, new HashSet<RV>(Z))) {
			addKB(xy, Z, true);
			imprime(X, Y, Z, true, "teste estatistico");
			return true;
		}
		// else
		addKB(xy, Z, false);
		imprime(X, Y, Z, false, "teste estatistico");
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
}
