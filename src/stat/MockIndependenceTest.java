package stat;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import fol.Domain;
import fol.Predicate;

public class MockIndependenceTest<RV extends RandomVariable<RV>> implements IndependenceTest<RV> {
	
	public MockIndependenceTest() {
		generateGraph();
	}

	UndirectedGraph<String, DefaultEdge> graph;

	@Override
	public double pvalue(RV X, RV Y) {
		// independentes = 1;
		// dependentes = 0;
		List<DefaultEdge> path = DijkstraShortestPath.findPathBetween(graph, X.getName(), Y.getName());
		if (path == null) {
			return 1.0;
		} else {
			return (1.0 - (1.0/path.size()));
		}
	}

	@Override
	public boolean test(RV X, RV Y, Set<RV> Z) {
		Set<String> s = new HashSet<String>(Z.size()*2);
		for (RV r : Z) {
			s.add(r.getName());
		}
 		return recursivePath(X.getName(), Y.getName(), s);
	}
	
	private boolean recursivePath(String X, String Y, Set<String> Z) {
		if (graph.containsEdge(X, Y)) {
			return false;
		}
		Set<String> visited = new HashSet<String>(Z);
		visited.add(X);
		for (String s : graph.vertexSet()) {
			if (graph.containsEdge(X, s)) {
				if (!Z.contains(s)) {
					if (!recursivePath(s, Y, visited)) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	public void generateGraph() {
		graph = new SimpleGraph<String, DefaultEdge>(DefaultEdge.class);
		String movie = "movie";
		String genre = "genre";
		String workedUnder = "workedUnder";
		String gender = "gender";
		String actor = "actor";
		String director = "director";
		String[] array = new String[6];
		array[0] = movie;
		array[1] = genre;
		array[2] = workedUnder;
		array[3] = gender;
		array[4] = actor;
		array[5] = director;		
		Random r = new Random();
		graph.addVertex(movie);
		graph.addVertex(genre);
		graph.addVertex(workedUnder);
		graph.addVertex(gender);
		graph.addVertex(actor);
		graph.addVertex(director);
		for (int i = 0; i < array.length; i++) {
			for (int j = i+1; j < array.length; j++) {
				if (r.nextBoolean()) {
					graph.addEdge(array[i], array[j]);
				}
			}
		}
		System.out.println(graph);		
	}
	
	
	public static void main(String[] args) {
		MockIndependenceTest<Predicate> mit = new MockIndependenceTest<Predicate>();
		Domain d = new Domain("d");
		Predicate movie = new Predicate("movie", d);
		Predicate genre = new Predicate("genre", d);
		Predicate workedUnder = new Predicate("workedUnder", d);
		Predicate gender = new Predicate("gender", d);
		Predicate actor = new Predicate("actor", d);
		Predicate director = new Predicate("director", d);
		Predicate[] array = new Predicate[6];
		array[0] = movie;
		array[1] = genre;
		array[2] = workedUnder;
		array[3] = gender;
		array[4] = actor;
		array[5] = director;
		for (int i = 0; i < array.length; i++) {
			for (int j = i+1; j < array.length; j++) {
				System.out.println("I(" + array[i] + ", " + array[j] + " | (/) ) = " + 
						mit.test(array[i], array[j], new HashSet<Predicate>()));

			} 
		}
		for (int i = 0; i < array.length; i++) {
			for (int j = i+1; j < array.length; j++) {
				for (int k = 0; k < array.length; k++) {
					if (!(k == i || k == j)) {
						System.out.println("I(" + array[i] + ", " + array[j] + " | " + array[k] + ") = " + 
								mit.test(array[i], array[j], Collections.singleton(array[k])));
					}
				}
			} 
		}
		for (int i = 0; i < array.length; i++) {
			for (int j = i+1; j < array.length; j++) {
				for (int k = 0; k < array.length; k++) {
					if (!(k == i || k == j)) {
						for (int l = k+1; l < array.length; l++) {
							if (!(l == i || l == j)) {
								Set<Predicate> set = new HashSet<Predicate>();
								set.add(array[k]);
								set.add(array[l]);
								System.out.println("I(" + array[i] + ", " + array[j] + " | " + 
										array[k] + ", " + array[l] + ") = " + 
										mit.test(array[i], array[j], set));
							}
						}
					}
				}
			} 
		}
	}

}
