package util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.graph.Subgraph;

/**
 * @author Leonardo Castilho Couto
 *
 */
public class Util {

	// Remove white spaces, tabs, and returns.
	public static String strip(String string) {
		return Util.join(string.split("\\s"));
	}		
	
	// joins a array of strings into a single string.
	public static String join(String[] strings) {
	    StringBuilder sb = new StringBuilder();
	    for (int i=0; i < strings.length; i++) {
	  	    sb.append(strings[i]);
	  	}
	  	return sb.toString();
	}	
	
	// joins a array of strings into a single string.
	public static String join(Object[] objects) {
		StringBuilder sb = new StringBuilder();
	    for (int i=0; i < objects.length; i++) {
	  	    sb.append(objects[i].toString());
	  	}
	  	return sb.toString();
	}	
	
	// joins a array of strings into a single string
	// with a separator between then.
	public static String join(String[] strings, String separator) {
		StringBuilder sb = new StringBuilder();
		if (strings.length == 0) { return sb.toString(); }
		sb.append(strings[0]);
	    for (int i=1; i < strings.length; i++) {
	        sb.append(separator);
	  	    sb.append(strings[i]);
	  	}
	  	return sb.toString();
	}
	
	// joins a array of strings into a single string
	// with a separator between then.
	public static String join(Object[] objects, String separator) {
		StringBuilder sb = new StringBuilder();
		if (objects.length == 0) { return sb.toString(); }
  	    sb.append(objects[0].toString());
	    for (int i=1; i < objects.length; i++) {
	        sb.append(separator);
	  	    sb.append(objects[i].toString());
	  	}
	  	return sb.toString();
	}
	
	// Converts a Set<T extends NameID> into a Map of <String, T>
	public static <T extends NameID> Map<String, T> setToMap(Set<T> tSet) {
		Map<String, T> m = new HashMap<String, T>();
		for(T t : tSet) {
			m.put(t.getName(), t);
		}
		return m;
	}
	
	// put Element T in the List's first position without deleting and adding elements.
	public static <T> void reorder(List<T> tList, T element) {
		int idx = tList.indexOf(element);
		if(idx > 0) {
			Collections.swap(tList, 0, idx);
//			T aux = tList.get(0);
//			tList.set(0, element);
//			T next;
//			for(int i = 0; i < idx; i++) {
//				next = tList.get(i+1);
//				tList.set(i+1, aux);
//				aux = next;				
//			}
		}
	}
	
	public static double geometricMean(Collection<Integer> values) {
		long n = 1;
		for (Integer v : values) {
			n = n * v.longValue();
		}
		return Math.pow(n, (1/values.size()));
	}
	
	public static double geometricMean(int[] values) {
		long n = 1;
		for (int v : values) {
			n = n * (long) v;
		}
		return Math.pow(n, (1/values.length));
	}
	
	/**
	 * Gives a List of Strings, each representing a edge in the graph.
	 * It sorts the vertex so that the same graph are always represented
	 * in the same way. 
	 * @param <T> the vertex must implements the Comparable interface
	 * @param graph
	 * @return A List<String> representation of this graph edges. 
	 */
	public static <T extends Comparable<? super T>> List<String> getEdges(Graph<T, ?> graph) {
		List<String> out = new LinkedList<String>();
		List<T> list = new ArrayList<T>(graph.vertexSet());
		Collections.sort(list);
		for (int i = 0; i < list.size() -1; i++) {
			for (int j = i+1; j < list.size(); j++) {
				T x = list.get(i);
				T y = list.get(j);
				if (graph.containsEdge(x, y)) {
					out.add("(" + x.toString() + " : " + y.toString() + ")");
				}
			}
		}
		return out;
	}
	
	/**
	 * Given a <code>graph/code> and a <code>vertex/code>, returns a subset of 
	 * <code>graph/code> with all neighbors vertex of <code>vertex</code> and
	 * the <code>vertex</code> itself. All edges between theses nodes are maintained
	 * in the subgraph.
	 * @param graph 
	 * @param vertex
	 * @return subset of graph that includes <code>vertex/code> and its neighbors.
	 */
	public static <V, E> Graph<V, E> neighborsGraph(Graph<V, E> graph, V vertex) {
		Set<V> neighbors = new HashSet<V>();
		neighbors.add(vertex);
		for (E edge : graph.edgesOf(vertex)) {
			V source = graph.getEdgeSource(edge);
			if (source == vertex) {
				neighbors.add(graph.getEdgeTarget(edge));
			} else {
				neighbors.add(source);
			}
		}
		return new Subgraph<V, E, Graph<V, E>>(graph, neighbors);
	}
	
	/**
	 * This method computes the machine precision number as the smallest 
	 * floating point number such that 1 + number differs from 1.
	 * 
	 * <p>This method is based on the subroutine machar described in 
	 * 
	 * <p>W. J. Cody, 
	 * MACHAR: A subroutine to dynamically determine machine parameters, 
	 * ACM Transactions on Mathematical Software, 14, 1988, pages 303-311.
	 * 
	 * <p>MINPACK-2 Project. February 1991. 
	 * Argonne National Laboratory and University of Minnesota. 
	 * Brett M. Averick.
	 * 
	 * @return smallest floating point number such that 
	 *  1 + number differs from 1;
	 */
	public static double machinePrecision() {
	    long ibeta,irnd,it,itemp,negep;
	    double a,b,beta,betain,betah,temp,tempa,temp1,
	           zero=0.0,one=1.0,two=2.0;
	    double ddpmeps;
	 
	    //determine ibeta, beta ala malcolm.

	    a = one;
	    b = one;
	    
	    do {
		    a = a + a;
		    temp = a + one;
		    temp1 = temp - a;
	    } while (Double.compare(temp1 - one, zero) == 0);
	    
	    do {
			b = b + b;
			temp = a + b;
			itemp = (int) (temp - a);
		} while (itemp == 0);

	    ibeta = itemp;
	    beta = (double) itemp;
	    
	    //determine it, irnd.
	    
	    it = 0;
	    b = one;
	    
	    do {
			it++;
			b = b*beta;
			temp = b + one;
			temp1 = temp -b;
		} while (Double.compare(temp1 - one, zero) == 0);
	    
	    irnd = 0;
	    betah = beta/two;
	    temp = a + betah;
	    if (Double.compare(temp -a, zero) != 0) {
	    	irnd = 1;
	    }
	    tempa = a + beta;
	    temp = tempa + betah;
	    if ((irnd == 0) && (Double.compare(temp -tempa, zero) != 0)) {
	    	irnd = 2;
	    }
	    
	    //determine ddpmeps.
    
	    negep = it + 3;
	    betain = one/beta;
	    a = one;
	    for (int i = 1; i <= negep; i++) {
		      a = a*betain;
	    }
	    
	    while (true) {
	    	temp = one + a;
	    	if (Double.compare(temp -one, zero) != 0) {
	    		break;
	    	}
	    	a = a*beta;
	    }
	    
	    ddpmeps = a;
	    if ((ibeta == 2) || (irnd == 0)) {
	    	return ddpmeps;
	    }
	    a = (a*(one + a))/two;
	    temp = one + a;
	    if (Double.compare(temp -one, zero) != 0) { 
	    	ddpmeps = a;
	    }
	    
	    return ddpmeps;
	}
	
	public static PrintStream dummyOutput = new PrintStream(new OutputStream() {
		
		@Override
		public void write(int b) throws IOException {
			// TODO Auto-generated method stub
			
		}
	});

}