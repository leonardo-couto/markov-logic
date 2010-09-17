package fol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgraph.graph.DefaultEdge;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.SimpleGraph;

import stat.RandomVariable;
import structureLearner.FormulaGenerator;
import util.Util;

/**
 * @author Leonardo Castilho Couto
 *
 */
public class Predicate implements RandomVariable<Predicate> {
	// TODO: ASSURE UNIQUE PREDICATE NAMES.
	
	private String name;
	private List<Domain> argDomains;
	private boolean closedWorld;
	private Map<Atom, Double> groundings;
	private Set<Atom> neGroundings;

	public static final Predicate equals = new Predicate("equals", Domain.universe, Domain.universe);

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return a List of each argument Domain
	 */
	public List<Domain> getDomains() {
		return argDomains;
	}

	/**
	 * 
	 */
	public Predicate(String name, List<Domain> domains) {
		this.name = name;
		this.argDomains = domains;
		this.closedWorld = false;
		this.groundings = new HashMap<Atom, Double>();
		this.neGroundings = new HashSet<Atom>();
	}
	
	public Predicate(String name, Domain ... domains) {
		this.name = name;
		this.argDomains = Arrays.asList(domains);
		this.closedWorld = false;
		this.groundings = new HashMap<Atom, Double>();
		this.neGroundings = new HashSet<Atom>();
	}
	
	public String toString() {
		return name + "(" + Util.join(argDomains.toArray(), ",") + ")";
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
	    if ( !(o instanceof Predicate) ) return false;
	    
	    Predicate p = (Predicate) o;
	    return name.equals(p.getName());
	}
	
	public int hashCode() {
		return name.hashCode();
	}
	
	/**
	 * @return the closedWorld
	 */
	public boolean isClosedWorld() {
		return closedWorld;
	}

	/**
	 * @param closedWorld the closedWorld to set
	 */
	public void setClosedWorld(boolean b) {
		if (b == true && this.closedWorld == false) { 
			if (neGroundings.isEmpty()) {
				setGroundings();
			} else {
				for (Atom a : neGroundings) {
					groundings.put(a, a.value);
				}
			}
		}
		if (b == false && this.closedWorld == true) {
			for (Atom a : neGroundings) {
				groundings.remove(a);
			}
		}
		this.closedWorld = b;
	}
	
	/**
	 * @param p
	 * @return A Set of all groundings of p.
	 * Assumes closedWorld.
	 */
	private void setGroundings() {
		Set<Atom> neGroundings = new HashSet<Atom>();
		List<List<Constant>> cll = listGroundings();
		for (List<Constant> cList : cll) {
			Atom a = new Atom(this, 0.0, cList);
			if (!groundings.containsKey(a)) {
				neGroundings.add(a);
			}
		}
		this.neGroundings = neGroundings;
		for (Atom a : neGroundings) {
			groundings.put(a, a.value);
		}
	}	
	
	private List<List<Constant>> listGroundings() {
		List<List<Constant>> out = new ArrayList<List<Constant>>();
		List<List<Constant>> prev;
		boolean firstLoop = true;
		for (Domain d : argDomains) {
			prev = out;
			out = new ArrayList<List<Constant>>();
			if (firstLoop) {
				firstLoop = false;
				for (Constant c : d) {
					out.add(Collections.singletonList(c));
				}
				continue;
			}
			for (Constant c : d) {
				for (List<Constant> list : prev) {
					List<Constant> lc = new ArrayList<Constant>(list);
					lc.add(c);
					out.add(lc);
				}
			}
		}
		return out;
	}
	
	/**
	 * @return the groundings
	 */
	public Map<Atom, Double> getGroundings() {
		return groundings;
	}

	/**
	 * @return The total possible number of groundings.
	 */
	public long totalGroundsNumber() {
		long i = 1;		
		for (Domain d : argDomains) {
			i = i * (long) d.size();
		}
		return i;
	}

	@Override
	public double[] getData() {
		double[] out = new double[groundings.size()];
		int i = 0;
		for (Double d : groundings.values()) {
			out[i] = d.doubleValue();
			i++;
		}
		return out;
	}

	@Override
	public double[][] getData(Predicate Y, List<Predicate> Z) {
		UndirectedGraph<Predicate, DefaultEdge> graph = new SimpleGraph<Predicate, DefaultEdge>(DefaultEdge.class);
		graph.addVertex(this);
		graph.addVertex(Y);

		if (shareDomain(this, Y)) {
			graph.addEdge(this, Y);
		}
		
		for (Predicate p : Z) {
			graph.addVertex(p);
			if (shareDomain(this, p)) {
				graph.addEdge(this, p);
			}
			if (shareDomain(Y, p)) {
				graph.addEdge(Y, p);
			}
		}
		
		for (int i = 0; i < Z.size(); i++) {
			for (int j = i +1; j < Z.size(); j++) {
				if (shareDomain(Z.get(i), Z.get(j))) {
					graph.addEdge(Z.get(i), Z.get(j));
				}
			}
		}		
		
		if (DijkstraShortestPath.findPathBetween(graph, this, Y) == null) {
			return null;
		}
		List<Predicate> pList = new ArrayList<Predicate>(Z);
		pList.add(this);
		pList.add(Y);
		Map<Predicate, Boolean> connected = new HashMap<Predicate, Boolean>((pList.size())*2);
		connected.put(this, true);
		connected.put(Y, true);
		for (Predicate p : Z) {
			if (DijkstraShortestPath.findPathBetween(graph, this, p) == null) {
				connected.put(p, false);
			} else {
				connected.put(p, true);
			}
		}
		List<Variable> vList = new ArrayList<Variable>();
		List<Atom> aList = new ArrayList<Atom>();
		for (Predicate p : pList) {
			if (connected.get(p).booleanValue()) {
				Atom a = FormulaGenerator.generateAtom(p, vList);
				for (Variable v : a.getVariables()) {
					if (!vList.contains(v)) {
						vList.add(v);
					}
				}
				aList.add(a);
			} else {
				aList.add(null);
			}
		}
		
		Variable[] var = vList.toArray(new Variable[vList.size()]);
		List<Constant[]> constants = new ArrayList<Constant[]>();
		
		int[] length = new int[var.length];
		int[] counter = new int[var.length+1];
		long n = 1;
		for (int i = 0; i < var.length; i++) {
			counter[i] = 0;
			constants.add(var[i].getConstants().toArray(new Constant[0]));
			length[i] = constants.get(i).length;
			n = n * length[i];
		}
		
		Constant[] c = new Constant[var.length];
		List<double[]> out = new ArrayList<double[]>();
		
		constants:
		for (long i = 0; i < n; i++) {
			
			List<double[]> d = new ArrayList<double[]>();
			d.add(new double[aList.size()]);
			
			for (int j = 0; j < var.length; j++) {
				if (counter[j] == length[j]) {
					counter[j] = 0;
					counter[j+1]++;
				}
				c[j] = constants.get(j)[counter[j]];
			}
			// TODO: Sampler, colocar tudo num set, e escolher alguns.
			// nao, set muito grande, associar 'n' a uma escolha unica de constants
			// e fazer sample de n até os valores convergirem.
			counter[0]++;
			
			for (int j = 0; j < aList.size(); j++) {
				if (connected.get(pList.get(j)).booleanValue()) {
					Atom a = (Atom) aList.get(j).replaceVariables(Arrays.asList(var), Arrays.asList(c));
					if (Double.isNaN(a.value)) {
						continue constants;
					}
					for (double[] elem : d) {
						elem[j] = a.value;
					}
				} else {
					// duplicate array with 0 and 1 in the current position
					double[][] copy = new double[d.size()][];
					int k = 0;
					for (double[] elem : d) {
						copy[k] = Arrays.copyOf(elem, elem.length);
						copy[k][j] = 1;
						elem[j] = 0;
						k++;
					}
					for (double[] elem : copy) {
						d.add(elem);
					}
				}
			}
			out.addAll(d);			
		}
			
		return out.toArray(new double[out.size()][]);
	}
	
	private boolean shareDomain(Predicate x, Predicate y) {
		// TODO: Do not account for parent/child relationship between Domains.
		for (Domain d : x.argDomains) {
			for (Domain d1 : y.argDomains) {
				if (d.equals(d1)) {
					return true;
				}
			}
		}
		return false;
	}

}
