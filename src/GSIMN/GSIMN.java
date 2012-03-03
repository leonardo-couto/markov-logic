package GSIMN;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import stat.IndependenceTest;
import stat.WeightedRV;
import util.Util;

public class GSIMN<T> {
	public static PrintStream out = System.out;
	
	private UndirectedGraph<T, DefaultEdge> graph;
	private final Set<T> vars;
	private final GSITest<T> test;
	private final Map<T, List<WeightedRV<T>>> pvalues;
	private final Map<T, Set<T>> independent;

	public GSIMN(Set<T> V, IndependenceTest<T> test) {
		this.graph = new SimpleGraph<T, DefaultEdge>(DefaultEdge.class);
		this.vars = new HashSet<T>(V);
		this.test = new GSITest<T>(test);
		this.pvalues = new HashMap<T, List<WeightedRV<T>>>();
		this.independent = new HashMap<T, Set<T>>();
		this.run(false, null);
	}
	
	/**
	 * 
	 * @param target Set of T that will have the p-value 
	 *   evaluated against all the others variables. 
	 */
	private void initPValues(Set<T> target) {
		// TODO: PARALELIZAR? PROCURAR FOR PARALELO NO JAVA
		// TODO: remover prints;
		Set<T> aux = new HashSet<T>(target);
		int npv = (this.vars.size()*(this.vars.size()-1))/2;
		int blah = 0;
		for (T X : this.vars) {
			aux.remove(X);
			for (T Y : aux) {
				blah++;
				long time = System.currentTimeMillis();
				double d = this.test.pvalue(X, Y);
				out.println(d + " : " + blah + "/" + npv + " t = " + (System.currentTimeMillis()-time) +
						" X = " + X + " Y = " + Y);
				this.pvalues.get(X).add(new WeightedRV<T>(Y, d));
				this.pvalues.get(Y).add(new WeightedRV<T>(X, d));
			}
		}
	}
	
	/**
	 * 
	 * @param target Set of T that will be updated
	 *  in the graph. 
	 */
	private void updateGraph(UndirectedGraph<T, DefaultEdge> graph, Map<T, Set<T>> dependent) {
		Set<T> aux = new HashSet<T>(this.vars);
		for (T X : this.vars) {
			aux.remove(X);
			for (T Y : aux) {
				if(dependent.get(X).contains(Y)) {
					graph.addEdge(X, Y);
				}
			}
		}
		this.graph = graph;
	}
	
	private boolean test(T x, T y, Collection<T> z, Collection<T> independent, Collection<T> dependent) {
		return this.test.test(x, y, z, independent, dependent);
	}
	
	/**
	 * 
	 * @return the independence graph
	 */
	public UndirectedGraph<T, DefaultEdge> getGraph() {
		return this.graph;
	}
	
	public boolean addVariable(T var) {
		if (this.vars.contains(var)) {
			return false;
		}
		this.run(true, var);
		return true;
	}
	
	public boolean removeVariable(T var) {
		if (!this.vars.contains(var)) {
			return false;
		}
		this.graph.removeVertex(var);
		this.pvalues.remove(var);
		for (Entry<T, List<WeightedRV<T>>> entry : this.pvalues.entrySet()) {
			Iterator<WeightedRV<T>> it = entry.getValue().iterator();
			while (it.hasNext()) {
				if (it.next().rv.equals(var)) {
					it.remove();
					break;
				}
			}
		}
		this.independent.remove(var);
		for (Entry<T, Set<T>> entry : this.independent.entrySet()) {
			entry.getValue().remove(var);
		}
		this.vars.remove(var);
		return true;
	}
	
	private UndirectedGraph<T, DefaultEdge> run(boolean target, T targetVar) {
		
		Map<T, Set<T>> dependent = new HashMap<T, Set<T>>();
		Map<T, List<T>> lambda = new HashMap<T, List<T>>();
		List<WeightedRV<T>> piW = new ArrayList<WeightedRV<T>>();
		UndirectedGraph<T, DefaultEdge> graph = new SimpleGraph<T, DefaultEdge>(DefaultEdge.class);

		for (T x : this.vars) {
			dependent.put(x, new HashSet<T>());
			graph.addVertex(x);
		}
		
		if (target) {
			this.pvalues.put(targetVar, new ArrayList<WeightedRV<T>>());
			this.independent.put(targetVar, new HashSet<T>());
			dependent.put(targetVar, new HashSet<T>());
			graph.addVertex(targetVar);
			this.initPValues(Collections.singleton(targetVar));
			this.vars.add(targetVar);
		} else {
			for (T x : this.vars) {
				this.pvalues.put(x, new ArrayList<WeightedRV<T>>());
				this.independent.put(x, new HashSet<T>());
			}
			this.initPValues(this.vars);
		}
		
		for (T x : this.vars) {
			
			// Computes pi (variable test ordering)
			List<WeightedRV<T>> wrv = this.pvalues.get(x);
			double avgLogP = 0.0;
			for (WeightedRV<T> w : wrv) {
				avgLogP = avgLogP + Math.log(w.value);
			}
			piW.add(new WeightedRV<T>(x, avgLogP));
			
			// Compute lambdaX
			Collections.sort(wrv, WeightedRV.valueComparator);
			lambda.put(x, WeightedRV.getRvList(wrv));
			
		}
		
		// Variables Ordering
		Collections.sort(piW, WeightedRV.valueComparator);
		List<T> pi = WeightedRV.getRvList(piW);
		
		// Main Loop
		for (T x : pi) {
			// Propagation phase
			List<T> lambdaX = lambda.get(x);
			
			// Move the variables know to be (or not) in the Markov blanket of X to the end of lambda
			lambdaX.removeAll(dependent.get(x));
			lambdaX.removeAll(this.independent.get(x));
			lambdaX.addAll(dependent.get(x));
			lambdaX.addAll(this.independent.get(x));
			
			// Grown phase
			List<T> S = new ArrayList<T>();
			
			for (T y : lambdaX) {
				
				List<T> lambdaY = lambda.get(y);
				
				if (!this.test(x, y, Collections.<T>emptySet(), this.independent.get(x), dependent.get(x))) {
					if(S.isEmpty() || !this.test(x, y, S, this.independent.get(x), dependent.get(x))) {
						// move X to the beginning of lambdaY
						lambdaY.remove(x);
						lambdaY.add(0, x);
						// reorders lambdaY
						lambdaY.removeAll(S);
						lambdaY.addAll(0, S);

						S.add(y);
					}
				}
			}
			
			// Changes the examination order pi
			ListIterator<T> itr = S.listIterator(S.size());
			while(itr.hasPrevious()) {
				T R = itr.previous();
				int i = pi.indexOf(x);
				// if R has not been examined yet it will be the next
				if(pi.subList(i+1, pi.size()).contains(R)) {
					Util.reorder(pi.subList(i+1, pi.size()), R);
					break;
				}
			}
			
			// Shrink phase
			List<T> SY;
			for (int i = S.size()-1; i >= 0; i--) {
				T Y = S.get(i);
				SY = new ArrayList<T>(S);
				SY.remove(i);
				if (this.test(x, Y, SY, this.independent.get(x), dependent.get(x))) {
					S = SY;
				}
			}
			
			// Markov Blanket of X equals S. Update the dependent and independent lists.
			for (T y : this.vars) {
				if (S.contains(y)) {
					dependent.get(y).add(x);
					dependent.get(x).add(y);
				} else {
					this.independent.get(y).add(x);
					this.independent.get(x).add(y);
				}
			}
			
		}
		
		this.updateGraph(graph, dependent);
		return this.graph;
	}
	
}
