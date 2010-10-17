/**
 * 
 */
package structureLearner;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jgraph.graph.DefaultEdge;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.SimpleGraph;

import fol.Predicate;

import stat.RandomVariable;

/**
 * @author leonardo.couto
 *
 */
public class TNodes<T extends RandomVariable<RandomVariable<?>>> implements Set<T> {
  
  private final Set<T> nodes; // TODO: remover, utilizar o set dos vertices do grafo.
  private static UndirectedGraph<Predicate, DefaultEdge> TNodes = new SimpleGraph<Predicate, DefaultEdge>(DefaultEdge.class);

  
  public TNodes() {
    nodes = new HashSet<T>();
  }

  public TNodes(Set<T> c) {
    nodes = new HashSet<T>(c);
  }

  public TNodes(int initialCapacity, float loadFactor) {
    nodes = new HashSet<T>(initialCapacity, loadFactor);
  }

  public TNodes(int initialCapacity) {
    nodes = new HashSet<T>(initialCapacity);
  }
  
  @Override
  public boolean add(T e) {
    // TODO Auto-generated method stub
    return false;
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
    // TODO Auto-generated method stub
  }

  @Override
  public boolean contains(Object o) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isEmpty() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Iterator<T> iterator() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean remove(Object o) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public int size() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public Object[] toArray() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <T> T[] toArray(T[] a) {
    // TODO Auto-generated method stub
    return null;
  }
  
}
