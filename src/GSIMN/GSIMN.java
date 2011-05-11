package GSIMN;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.SimpleGraph;

import stat.IndependenceTest;
import stat.RandomVariable;
import stat.WeightedRV;
import util.Util;

public class GSIMN<RV extends RandomVariable<RV>> {
	private final UndirectedGraph<RV, DefaultEdge> graph;
	private final Set<RV> vars;
	private final IndependenceTest<RV> test;

	public GSIMN(Set<RV> V, IndependenceTest<RV> test) {
		this.graph = new SimpleGraph<RV, DefaultEdge>(DefaultEdge.class);
		this.vars = new HashSet<RV>(V);
		this.test = test;
	}
	
	public UndirectedGraph<RV, DefaultEdge> run() {
		
		// Initialization
		Set<RV> aux = new HashSet<RV>(this.vars);
		Map<RV, List<WeightedRV<RV>>> pvalues = new HashMap<RV, List<WeightedRV<RV>>>();
		Map<RV, Set<RV>> dependent = new HashMap<RV, Set<RV>>();
		Map<RV, Set<RV>> independent = new HashMap<RV, Set<RV>>();
		Map<RV, List<RV>> lambda = new HashMap<RV, List<RV>>();
		List<WeightedRV<RV>> piW = new ArrayList<WeightedRV<RV>>();
		List<RV> pi;
		GSIndependenceTest<RV> gsIndependence = new GSIndependenceTest<RV>(this.test);

		for (RV X : this.vars) {
			pvalues.put(X, new ArrayList<WeightedRV<RV>>());
			dependent.put(X, new HashSet<RV>());
			independent.put(X, new HashSet<RV>());
			graph.addVertex(X);
		}
		
		// TODO: PARALELIZAR? PROCURAR FOR PARALELO NO JAVA
		// TODO: remover prints;
		int npv = (vars.size()*(vars.size()-1))/2;
		int blah = 0;
		for (RV X : vars) {
			aux.remove(X);
			for (RV Y : aux) {
				blah++;
				long time = System.currentTimeMillis();
				double d = test.pvalue(X, Y);
				gsIndependence.addPValue(X, Y, d);
				System.out.println(d + " : " + blah + "/" + npv + " t = " + (System.currentTimeMillis()-time) +
						" X = " + X + " Y = " + Y);
				pvalues.get(X).add(new WeightedRV<RV>(Y, d));
				pvalues.get(Y).add(new WeightedRV<RV>(X, d));
			}
		}
		
		for (RV X : vars) {
			
			// Computes pi (variable test ordering)
			List<WeightedRV<RV>> wrv = pvalues.get(X);
			double avgLogP = 0.0;
			for (WeightedRV<RV> w : wrv) {
				avgLogP = avgLogP + Math.log(w.value);
			}
			piW.add(new WeightedRV<RV>(X, avgLogP));
			
			// Compute lambdaX
			Collections.sort(wrv, WeightedRV.valueComparator);
			lambda.put(X, WeightedRV.getRvList(wrv));
			
		}
		
		// Variables Ordering
		Collections.sort(piW, WeightedRV.valueComparator);
		pi = WeightedRV.getRvList(piW);
		
		// Main Loop
		for (RV X : pi) {
			// Propagation phase
			List<RV> lambdaX = lambda.get(X);
			
			// Move the variables know to be (or not) in the Markov blanket of X to the end of lambda
			lambdaX.removeAll(dependent.get(X));
			lambdaX.removeAll(independent.get(X));
			lambdaX.addAll(dependent.get(X));
			lambdaX.addAll(independent.get(X));
			
			// Grown phase
			List<RV> S = new ArrayList<RV>();
			
			for (RV Y : lambdaX) {
				
				List<RV> lambdaY = lambda.get(Y);
				
				if (!gsIndependence.test(X, Y, Collections.<RV>emptySet(), independent.get(X), dependent.get(X))) {
					if(!gsIndependence.test(X, Y, S, independent.get(X), dependent.get(X))) {
						// move X to the beginning of lambdaY
						lambdaY.remove(X);
						lambdaY.add(0, X);
						// reorders lambdaY
						lambdaY.removeAll(S);
						lambdaY.addAll(0, S);

						S.add(Y);
					}
				}
			}
			
			// Changes the examination order pi
			ListIterator<RV> itr = S.listIterator(S.size());
			while(itr.hasPrevious()) {
				RV R = itr.previous();
				int i = pi.indexOf(X);
				// if R has not been examined yet it will be the next
				if(pi.subList(i+1, pi.size()).contains(R)) {
					Util.reorder(pi.subList(i+1, pi.size()), R);
					break;
				}
			}
			
			// Shrink phase
			List<RV> SY;
			for (int i = S.size()-1; i >= 0; i--) {
				RV Y = S.get(i);
				SY = new ArrayList<RV>(S);
				SY.remove(i);
				if (gsIndependence.test(X, Y, SY, independent.get(X), dependent.get(X))) {
					S = SY;
				}
			}
			
			// Markov Blanket of X equals S. Actualizes the dependent and independent lists.
			for (RV Y : vars) {
				if (S.contains(Y)) {
					dependent.get(Y).add(X);
					dependent.get(X).add(Y);
				} else {
					independent.get(Y).add(X);
					independent.get(X).add(Y);
				}
			}
			
		}
		
		aux = new HashSet<RV>(vars);
		for (RV X : vars) {
			aux.remove(X);
			for (RV Y : aux) {
				if(dependent.get(X).contains(Y)) {
					graph.addEdge(X, Y);
				}
			}
		}
		
		return graph;
		
	}
	
}
