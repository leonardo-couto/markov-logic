package GSIMN;

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

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.SimpleGraph;

import stat.IndependenceTest;
import stat.RandomVariable;
import stat.WeightedRV;
import util.Util;

public class GSIMN<RV extends RandomVariable<RV>> {
	private UndirectedGraph<RV, DefaultEdge> graph;
	private final Set<RV> vars;
	private final GSITest<RV> test;
	private final Map<RV, List<WeightedRV<RV>>> pvalues;

	public GSIMN(Set<RV> V, IndependenceTest<RV> test) {
		this.graph = new SimpleGraph<RV, DefaultEdge>(DefaultEdge.class);
		this.vars = new HashSet<RV>(V);
		this.test = new GSITest<RV>(test);
		this.pvalues = new HashMap<RV, List<WeightedRV<RV>>>();
		this.run(false, null);
	}
	
	/**
	 * 
	 * @param target Set of RV that will have the p-value 
	 *   evaluated against all the others variables. 
	 */
	private void initPValues(Set<RV> target) {
		// TODO: PARALELIZAR? PROCURAR FOR PARALELO NO JAVA
		// TODO: remover prints;
		Set<RV> aux = new HashSet<RV>(target);
		int npv = (this.vars.size()*(this.vars.size()-1))/2;
		int blah = 0;
		for (RV X : this.vars) {
			aux.remove(X);
			for (RV Y : aux) {
				blah++;
				long time = System.currentTimeMillis();
				double d = this.test.pvalue(X, Y);
				System.out.println(d + " : " + blah + "/" + npv + " t = " + (System.currentTimeMillis()-time) +
						" X = " + X + " Y = " + Y);
				this.pvalues.get(X).add(new WeightedRV<RV>(Y, d));
				this.pvalues.get(Y).add(new WeightedRV<RV>(X, d));
			}
		}
	}
	
	/**
	 * 
	 * @param target Set of RV that will be updated
	 *  in the graph. 
	 */
	private void updateGraph(UndirectedGraph<RV, DefaultEdge> graph, Map<RV, Set<RV>> dependent) {
		Set<RV> aux = new HashSet<RV>(this.vars);
		for (RV X : this.vars) {
			aux.remove(X);
			for (RV Y : aux) {
				if(dependent.get(X).contains(Y)) {
					graph.addEdge(X, Y);
				}
			}
		}
		this.graph = graph;
	}
	
	private boolean test(RV x, RV y, Collection<RV> z, Collection<RV> independent, Collection<RV> dependent) {
		return this.test.test(x, y, z, independent, dependent);
	}
	
	/**
	 * 
	 * @return the independence graph
	 */
	public UndirectedGraph<RV, DefaultEdge> getGraph() {
		return this.graph;
	}
	
	public boolean addVariable(RV var) {
		if (this.vars.contains(var)) {
			return false;
		}
		this.run(true, var);
		this.vars.add(var);
		return true;
	}
	
	public boolean removeVariable(RV var) {
		if (!this.vars.contains(var)) {
			return false;
		}
		this.graph.removeVertex(var);
		this.pvalues.remove(var);
		for (Entry<RV, List<WeightedRV<RV>>> entry : this.pvalues.entrySet()) {
			Iterator<WeightedRV<RV>> it = entry.getValue().iterator();
			while (it.hasNext()) {
				if (it.next().rv.equals(var)) {
					it.remove();
					break;
				}
			}
		}
		this.vars.remove(var);
		return true;
	}
	
	private UndirectedGraph<RV, DefaultEdge> run(boolean target, RV targetVar) {
		
		Map<RV, Set<RV>> dependent = new HashMap<RV, Set<RV>>();
		Map<RV, Set<RV>> independent = new HashMap<RV, Set<RV>>();
		Map<RV, List<RV>> lambda = new HashMap<RV, List<RV>>();
		List<WeightedRV<RV>> piW = new ArrayList<WeightedRV<RV>>();
		UndirectedGraph<RV, DefaultEdge> graph = new SimpleGraph<RV, DefaultEdge>(DefaultEdge.class);

		for (RV x : this.vars) {
			dependent.put(x, new HashSet<RV>());
			independent.put(x, new HashSet<RV>());
			graph.addVertex(x);
		}
		
		if (target) {
			this.pvalues.put(targetVar, new ArrayList<WeightedRV<RV>>());
			dependent.put(targetVar, new HashSet<RV>());
			independent.put(targetVar, new HashSet<RV>());
			graph.addVertex(targetVar);
			this.initPValues(Collections.singleton(targetVar));
			this.vars.add(targetVar);
		} else {
			for (RV x : this.vars) {
				this.pvalues.put(x, new ArrayList<WeightedRV<RV>>());
			}
			this.initPValues(this.vars);
		}
		
		for (RV x : this.vars) {
			
			// Computes pi (variable test ordering)
			List<WeightedRV<RV>> wrv = this.pvalues.get(x);
			double avgLogP = 0.0;
			for (WeightedRV<RV> w : wrv) {
				avgLogP = avgLogP + Math.log(w.value);
			}
			piW.add(new WeightedRV<RV>(x, avgLogP));
			
			// Compute lambdaX
			Collections.sort(wrv, WeightedRV.valueComparator);
			lambda.put(x, WeightedRV.getRvList(wrv));
			
		}
		
		// Variables Ordering
		Collections.sort(piW, WeightedRV.valueComparator);
		List<RV> pi = WeightedRV.getRvList(piW);
		
		// Main Loop
		for (RV x : pi) {
			// Propagation phase
			List<RV> lambdaX = lambda.get(x);
			
			// Move the variables know to be (or not) in the Markov blanket of X to the end of lambda
			lambdaX.removeAll(dependent.get(x));
			lambdaX.removeAll(independent.get(x));
			lambdaX.addAll(dependent.get(x));
			lambdaX.addAll(independent.get(x));
			
			// Grown phase
			List<RV> S = new ArrayList<RV>();
			
			for (RV y : lambdaX) {
				
				List<RV> lambdaY = lambda.get(y);
				
				if (!this.test(x, y, Collections.<RV>emptySet(), independent.get(x), dependent.get(x))) {
					if(S.isEmpty() || !this.test(x, y, S, independent.get(x), dependent.get(x))) {
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
			ListIterator<RV> itr = S.listIterator(S.size());
			while(itr.hasPrevious()) {
				RV R = itr.previous();
				int i = pi.indexOf(x);
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
				if (this.test(x, Y, SY, independent.get(x), dependent.get(x))) {
					S = SY;
				}
			}
			
			// Markov Blanket of X equals S. Update the dependent and independent lists.
			for (RV y : this.vars) {
				if (S.contains(y)) {
					dependent.get(y).add(x);
					dependent.get(x).add(y);
				} else {
					independent.get(y).add(x);
					independent.get(x).add(y);
				}
			}
			
		}
		
		this.updateGraph(graph, dependent);
		return this.graph;
	}
	
}
