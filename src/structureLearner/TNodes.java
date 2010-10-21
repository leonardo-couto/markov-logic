/**
 * 
 */
package structureLearner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.Subgraph;

import stat.RandomVariable;

/**
 * This class represents a Set of TNodes, and maintain a graph where 
 * the TNodes X and Y are connected iff !X.isIndependent(Y).
 * Note that it does not represent the independence graph between variables.
 *
 */
public class TNodes<T extends RandomVariable<T>> implements Set<T> {

	private final UndirectedGraph<T, DefaultEdge> tNodes;

  public TNodes() {
		tNodes = new SimpleGraph<T, DefaultEdge>(DefaultEdge.class);
	}

	public TNodes(Set<T> c) {
		tNodes = new SimpleGraph<T, DefaultEdge>(DefaultEdge.class);
		this.addAll(c);
	}

	@Override
	public boolean add(T e) {
		Set<T> nodes = this.tNodes.vertexSet();
		if (!this.tNodes.addVertex(e)) {
			return false;
		}
		for (T node : nodes) {
			if (!e.isIndependent(node)) {
				this.tNodes.addEdge(e, node);
			}
		}
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		boolean modified = false;
		for (T element : c) {
			modified = modified || this.add(element);
		}
		return modified;
	}

	@Override
	public void clear() {
		this.tNodes.removeAllVertices(new HashSet<T>(this.tNodes.vertexSet()));
	}

	@Override
	public boolean contains(Object o) {
		return this.tNodes.vertexSet().contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return this.tNodes.vertexSet().containsAll(c);
	}

	@Override
	public boolean isEmpty() {
		return this.tNodes.vertexSet().isEmpty();
	}

	@Override
	public Iterator<T> iterator() {
		return this.tNodes.vertexSet().iterator();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(Object o) {
		try {
			return this.tNodes.removeVertex((T) o);
		} catch (ClassCastException e) {
			return false;
		}
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean modified = false;
		for (Object element : c) {
			modified = modified || this.remove(element);
		}
		return modified;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean modified = false;
		Set<T> nodes = new HashSet<T>(this.tNodes.vertexSet());
		for (T node : nodes) {
			if (!c.contains(node)) {
				modified = modified || this.remove(node);
			}
		}
		return modified;
	}

	@Override
	public int size() {
		return this.tNodes.vertexSet().size();
	}

	@Override
	public Object[] toArray() {
		return this.tNodes.vertexSet().toArray();
	}

	@Override
	public <P> P[] toArray(P[] a) {
		return this.tNodes.vertexSet().toArray(a);
	}
	
	public UndirectedGraph<T, DefaultEdge> getGraph() {
	    return tNodes;
	 }
	
	 public Iterator<double[]> getDataIterator(T x, T y, List<T> z) {
	    Set<T> nodes = new HashSet<T>(z);
	    nodes.add(x);
	    nodes.add(y);
	    Graph<T, DefaultEdge> graph = new Subgraph<T, DefaultEdge, UndirectedGraph<T, DefaultEdge>>(this.tNodes, nodes);
	    
	    if (DijkstraShortestPath.findPathBetween(graph, x, y) == null) {
	      return Collections.<double[]>emptyList().iterator();
	    }
	    
	    List<T> nodesList = new ArrayList<T>(z.size()+2);
	    
	    for (T node : nodes) {
	      if (DijkstraShortestPath.findPathBetween(graph, x, node) == null) {
	        nodesList.add(x.emptyVariable());
	      } else {
	        nodesList.add(node);
	      }
	    }
	    nodesList.add(x);
	    nodesList.add(y);
	    
	    
	    
	    
	    // TODO: PAREI AQUI!!!!
	    return null;
	  }

}
