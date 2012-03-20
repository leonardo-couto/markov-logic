package stat.fol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.Subgraph;

import fol.Atom;
import fol.Constant;
import fol.Variable;
import fol.database.Database;
import fol.database.Groundings;

import stat.Distribution;
import stat.sampling.DefaultSampler;

public class AtomDistribution implements Distribution<Atom> {
	
	private final Database db;
	private final UndirectedGraph<Atom, DefaultEdge> graph;
	
	public AtomDistribution(Database db) {
		this.db = db;
		this.graph = new SimpleGraph<Atom, DefaultEdge>(DefaultEdge.class);
	}

	public AtomDistribution(Set<Atom> c, Database db) {
		this.db = db;
		this.graph = new SimpleGraph<Atom, DefaultEdge>(DefaultEdge.class);
		this.addAll(c);
	}
	
	/**
	 * Test if a and b both have a direct connection.
	 * ie.: if in the absence of any other data you are able
	 * to get (a, b) points.
	 * @return true if both RandomVariables are somehow connected.
	 */
	private static boolean isConnected(Atom a, Atom b) {
		for (Variable va : a.getVariables()) {
			for (Variable vb : b.getVariables()) {
				if (va == vb) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean add(Atom e) {
		if (!this.graph.addVertex(e)) { return false; }
		for (Atom vertex : this.graph.vertexSet()) {
			if (vertex != e && isConnected(vertex, e)) {
				this.graph.addEdge(vertex, e);
			}
		}
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends Atom> c) {
		boolean modified = false;
		for (Atom a : c) {
			boolean b = this.add(a);
			modified =  modified || b;
		}
		return modified;
	}

	@Override
	public boolean remove(Atom e) {
		return this.graph.removeVertex(e);
	}
	
	@Override
	public Iterator<Boolean> getDataIterator(Atom x) {
		final Iterator<Atom> iterator = new Groundings<Atom>(x);

		return new Iterator<Boolean>() {
			@Override
			public boolean hasNext() { return iterator.hasNext(); }
			@Override
			public Boolean next() { return db.valueOf(iterator.next()); }
			@Override
			public void remove() {}
		};
	}

//	/**
//	 * Get only groundings that have the same constants as x
//	 * @param x Atom with Constants
//	 * @param groundings grounded Atoms of predicate x.predicate
//	 * @return a subset of groundings that have the same constants as x
//	 */
//	private static Map<Atom, Double> filterGroundings(Atom x, Map<Atom, Double> groundings) {
//		Map<Atom, Double> result = new HashMap<Atom, Double>(groundings.size());
//		Map<Integer, Constant> filter = new HashMap<Integer, Constant>();
//		for (int i = 0; i < x.terms.length; i++) {
//			Term t = x.terms[i];
//			if (t instanceof Constant) {
//				filter.put(i, (Constant) t);
//			}
//		}
//		nextGrounding: for (Atom a : groundings.keySet()) {
//			for (Integer i : filter.keySet()) {
//				if (a.terms[i] != filter.get(i)) {
//					continue nextGrounding;
//				}
//			}
//			result.put(a, groundings.get(a));
//		}
//		return result;
//	}
	
	// se eles nao se conectam de maneira alguma, assumir que sao independentes
	//   - talvez deixar marcado para quando eles se conectarem
	// se eles estiverem conectados, mas nao diretamente, assumir que sao dependentes
	// se eles estiverem conectados diretamente, retornar os dados
	@Override
	public Iterator<boolean[]> getDataIterator(Atom x, Atom y) {
		if (isConnected(x, y)) {
			return getIterator(Arrays.asList(new Atom[] {x, y}), this.db);
		} else if (DijkstraShortestPath.findPathBetween(this.graph, x, y) == null) {
			return null;
		}
		return Collections.<boolean[]>emptyList().iterator();
	}

	/**
	 * TODO: refazer esse comentário!
	 * Devolve valores de TNodes na ordem Z_0, Z_1, ... , Z_n, X, Y. Onde
	 * X = this.
	 * @return This variable marginal data.
	 */
	@Override
	public Iterator<boolean[]> getDataIterator(Atom x, Atom y, List<Atom> z) {
		if (z.isEmpty()) {
			return this.getDataIterator(x, y);
		}
		Set<Atom> nodes = new HashSet<Atom>(z);
		nodes.add(x);
		nodes.add(y);
		Graph<Atom, DefaultEdge> subgraph = 
			new Subgraph<Atom, DefaultEdge, UndirectedGraph<Atom, DefaultEdge>>(
					this.graph, nodes);

		// se nao tiver conexao direta entre x e y ou conexao indireta atravez de z
		// devolve um iterador vazio
		if (DijkstraShortestPath.findPathBetween(subgraph, x, y) == null) {
			return Collections.<boolean[]>emptyList().iterator();
		}

		List<Atom> nodesList = new ArrayList<Atom>(z.size()+2);

		// remove as variaveis aleatorias em z que nao estao conectadas a x
		for (Atom node : z) {
			if (DijkstraShortestPath.findPathBetween(subgraph, x, node) != null) {
				nodesList.add(node);
			} else {
				nodesList.add(Atom.TRUE);
			}
		}
		nodesList.add(x);
		nodesList.add(y);
		return getIterator(nodesList, db);
	}
	
	private static Iterator<boolean[]> getIterator(List<Atom> nodes, final Database db) {
		final List<Variable> variables = new ArrayList<Variable>();
		final List<Atom> atoms = new ArrayList<Atom>(nodes);
		{
			Set<Variable> variablesSet = new HashSet<Variable>();
			for (Atom a : nodes) {
				variablesSet.addAll(a.getVariables());
			}
			variables.addAll(variablesSet);
		}

		List<List<Constant>> constants = new ArrayList<List<Constant>>(variables.size());
		final Map<Variable, Constant> groundings = new HashMap<Variable, Constant>();
		for (Variable v : variables) {
			groundings.put(v, null);
			List<Constant> list = v.getConstants();
			constants.add(list);
		}

		final DefaultSampler<Constant> sampler = new DefaultSampler<Constant>(constants);
		final Iterator<List<Constant>> iterator = sampler.iterator();
		
		return new Iterator<boolean[]>() {

			private boolean[] next = this.makeNext();
			
			private boolean[] makeNext() {
				boolean[] out = new boolean[atoms.size()];
					while (iterator.hasNext()) {
						List<Constant> grounds = iterator.next();
						for (int i = 0; i < variables.size(); i++) {
							groundings.put(variables.get(i), grounds.get(i));
						}
						int outIndex = 0;
						for (Atom a : atoms) {
							out[outIndex] = a.getValue(db, groundings);
							outIndex++;
						}
						return out;
					}
				return null;
			}

			@Override
			public boolean hasNext() {
				return (this.next != null);
			}

			@Override
			public boolean[] next() {
				boolean[] out = this.next;
				this.next = this.makeNext();
				return out;
			}

			@Override
			public void remove() {
				// do nothing				
			}

		};
	}

	@Override
	public Set<Atom> getRandomVariables() {
		return this.graph.vertexSet();
	}

}
