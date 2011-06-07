package util;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import parse.ParseDataSet;
import parse.ParseDomain;
import stat.ContingencyTable;
import stat.Histogram;
import stat.PearsonChiSquare;
import fol.Domain;
import fol.Predicate;

@SuppressWarnings("unused")
public class Test {
		
	public static void clean(int[][] matriz) {
		matriz[0][0] = 0;
		matriz[0][1] = 0;
		matriz[1][0] = 0;
		matriz[1][1] = 0;		
	}
	
	public static double[][] add(int[][] matriz, double d) {
		double[][] out = new double[2][2];
		out[0][0] = matriz[0][0] + d;
		out[0][1] = matriz[0][1] + d;
		out[1][0] = matriz[1][0] + d;
		out[1][1] = matriz[1][1] + d;
		return out;
	}
	
	public static Random x = new Random();
	public static Random y = new Random();
	public static boolean[] getPointXY(float px, float py) {
		boolean xvalue = (Float.compare(x.nextFloat(), px) > 0);
		boolean yvalue = xvalue ? (Float.compare(y.nextFloat(), py) > 0) : (Float.compare(y.nextFloat(), py) < 0);
		return new boolean[] {xvalue, yvalue};
	}

	public static void main(String[] args) {
		
		Integer i1 = 1;
		Integer i2 = 2;
		
		Set<Integer> xy = new HashSet<Integer>();
		xy.add(i1);
		xy.add(i2);
		
		Set<Integer> yx = new HashSet<Integer>();
		yx.add(i2);
		yx.add(i1);
		
		Map<Set<Integer>, Boolean> map = new HashMap<Set<Integer>, Boolean>();
		map.put(xy, true);
		
		if (map.containsKey(yx)) {
			System.out.println(map.get(yx));
		} else {
			System.out.println("FALSOOO!!");
		}
		
		
		Random r = new Random();
		double random;
		int[][] matriz = new int[2][2];
		clean(matriz);
		long j = 0;
		Histogram h = new Histogram();
		float px = .51f;
		float py = .60f;
		
		while(true) {
			j++;
			for (int i = 0; i < 100; i++) {
				boolean[] point = getPointXY(px, py);
				if (point[0]) {
					if (point[1]) {
						matriz[1][1]++;
					} else {
						matriz[1][0]++;
					}
				} else {
					if (point[1]) {
						matriz[0][1]++;
					} else {
						matriz[0][0]++;
					}
				}
			}
			if (j > 10) {
			  ContingencyTable ct = new ContingencyTable(add(matriz,0.5));
			  PearsonChiSquare pearson = new PearsonChiSquare(ct);
			  double pvalue = pearson.pvalue();
			  h.add(pvalue);
			  System.out.println(Arrays.deepToString(matriz) + " : " + pvalue);
			  clean(matriz);
			  if (j > 10000) {
				  break;
			  }
			}
		}
		System.out.println(Arrays.toString(h.getHistogram(100)));
		
		
		
		
		
		
//		System.out.println("precision = " + machinePrecision());
		
		/*
		double d = Math.atan(-3.0/2.0);
		System.out.println("theta2: " + d);
		double sin = Math.sin(d);
		double cos = Math.cos(d);
		System.out.println("sin: " + Math.sin(d));
		System.out.println("cos: " + Math.cos(d));
		System.out.println((4.0/(Math.pow(Math.PI,2)*cos)));
		System.out.println((-6.0/(Math.pow(Math.PI,2)*sin)));
		

		d = Math.atan(2.0/3.0);
		System.out.println("theta1: " + d);
		sin = Math.sin(d);
		cos = Math.cos(d);
		System.out.println("sin: " + Math.sin(d));
		System.out.println("cos: " + Math.cos(d));
		System.out.println((6.0/cos));
		System.out.println((4.0/sin));

		
		int n = 10;
		while(n > 2) {
			n = (int) Math.ceil(n/2.0);
			System.out.println(n);
		}

		UndirectedGraph<Integer, DefaultEdge> graph = new SimpleGraph<Integer, DefaultEdge>(DefaultEdge.class);
		Integer i0 = new Integer(0);
		Integer i1 = new Integer(1);
		Integer i2 = new Integer(2);
		Integer i3 = new Integer(3);
		graph.addVertex(i0);
		graph.addVertex(i1);
		graph.addVertex(i2);
		graph.addVertex(i3);
		graph.addEdge(i0, i1);
		graph.addEdge(i0, i2);
		graph.addEdge(i0, i3);
		graph.addEdge(i2, i3);
		System.out.println(graph.toString());*/
		
	}

	/**
	 * @param args
	 */
	public static void main1(String[] args) {
		// TODO: Remove
		File mln = new File(
				"/home/leonardo/opt/alchemy/datasets/imdb/empty.mln");
		File db = new File("/home/leonardo/opt/alchemy/datasets/imdb/imdb.2.db");
		ParseDomain pmln = new ParseDomain();
		try {
			pmln.parse(mln);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// System.out.println("*************");
		// for(Predicate p : pmln.getPredicates()) {
		// System.out.println(p.toString());
		// }
		// System.out.println("*************");
		// for(Domain d : pmln.getDomains()) {
		// System.out.println(d.toString());
		// }
		System.out
				.println("************* NOW TESTING DB FILE ****************");
		ParseDataSet pdb = new ParseDataSet(pmln.getPredicates());
		try {
			pdb.parse(db);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// System.out.println("*************");
		// for(Atom gnd : pdb.getGroundings()) {
		// System.out.println(gnd.toString());
		// }
		// System.out.println("*************");
		// for(Constant c : pdb.getConstants()) {
		// System.out.println(c.toString());
		// }

		System.out
				.println("************* TESTANDO FORMULAS COUNT ****************");
		Domain film = null;
		Domain person = null;
		Domain dgender = null;
		Domain dgenre = null;
		Predicate movie = null;
		Predicate actor = null;
		Predicate director = null;
		Predicate gender = null;
		Predicate genre = null;
		Predicate workedUnder = null;
		Predicate sameMovie = null;
		Predicate sameGenre = null;
		Predicate sameGender = null;
		Predicate samePerson = null;
		for (Domain d : pmln.getDomains()) {
			if (d.getName().equals("film")) {
				film = d;
			}
			if (d.getName().equals("person")) {
				person = d;
			}
			if (d.getName().equals("dgender")) {
				dgender = d;
			}
			if (d.getName().equals("dgenre")) {
				dgenre = d;
			}
		}
		for (Predicate p : pmln.getPredicates()) {
			if (p.getName().equals("actor")) {
				actor = p;
				actor.setClosedWorld(true);
			}
			if (p.getName().equals("director")) {
				director = p;
				director.setClosedWorld(true);
			}
			if (p.getName().equals("movie")) {
				movie = p;
				movie.setClosedWorld(true);
			}
			if (p.getName().equals("gender")) {
				gender = p;
				gender.setClosedWorld(true);
			}
			if (p.getName().equals("genre")) {
				genre = p;
				genre.setClosedWorld(true);
			}
			if (p.getName().equals("workedUnder")) {
				workedUnder = p;
				workedUnder.setClosedWorld(true);
			}
			if (p.getName().equals("sameMovie")) {
				sameMovie = p;
				sameMovie.setClosedWorld(true);
			}
			if (p.getName().equals("sameGenre")) {
				sameGenre = p;
				sameGenre.setClosedWorld(true);
			}
			if (p.getName().equals("sameGender")) {
				sameGender = p;
				sameGender.setClosedWorld(true);
			}
			if (p.getName().equals("samePerson")) {
				samePerson = p;
				samePerson.setClosedWorld(true);
			}
		}
		// Variable X = new Variable("X", film);
		// Variable X1 = new Variable("X1", film);
		// Variable Y = new Variable("Y", person);
		// Variable Y1 = new Variable("Y1", person);
		// Variable Z = new Variable("Z", dgender);
		// Variable Z1 = new Variable("Z1", dgender);
		// Variable W = new Variable("W", dgenre);
		// Variable W1 = new Variable("W1", dgenre);
		// Formula a1 = new Atom(movie, Arrays.asList(X, Y));
		// Formula a2 = new Atom(actor, Collections.singletonList(Y));
		// Formula a3 = new Atom(director, Collections.singletonList(Y));
		// Formula f1 = new Disjunction(Arrays.asList(a1, a2));
		// Formula f2 = new Disjunction(Arrays.asList(a1, a3));
		// Formula f3 = a3;
		// Formula f4 = a2;
		// Formula f5 = a1;
		// Formula f6 = new Atom(gender, Arrays.asList(Y, Z));
		// Formula f7 = new Atom(genre, Arrays.asList(Y, W));
		// Formula f8 = new Atom(workedUnder, Arrays.asList(Y1, Y));
		// Formula f9 = new Atom(sameMovie, Arrays.asList(X, X1));
		// Formula f10 = new Atom(sameGenre, Arrays.asList(W, W1));
		// Formula f11 = new Atom(sameGender, Arrays.asList(Z, Z1));
		// Formula f12 = new Atom(samePerson, Arrays.asList(Y1, Y));
		// System.out.println(f1.toString());
		// FormulaCount fc = f1.trueCounts();
		// System.out.println("f1 total counts: " + fc.totalCounts);
		// System.out.println("f1 true counts: " + fc.trueCounts);
		// System.out.println("f1 NaN counts: " + fc.NaNCounts);
		// System.out.println(f2.toString());
		// fc = f2.trueCounts();
		// System.out.println("f1 total counts: " + fc.totalCounts);
		// System.out.println("f1 true counts: " + fc.trueCounts);
		// System.out.println("f1 NaN counts: " + fc.NaNCounts);

		// WeightedPseudoLogLikelihood wpll = new
		// WeightedPseudoLogLikelihood(new
		// HashSet<Predicate>(Arrays.asList(movie, actor, director)),
		// Arrays.asList(f1, f2));
		// WeightedPseudoLogLikelihood wpll = new
		// WeightedPseudoLogLikelihood(new
		// HashSet<Predicate>(pmln.getPredicates()), Arrays.asList(f3, f4, f5
		// ,f6 , f7, f8, f9, f10, f11, f12));
		// // double[] w = {-0.150809d, 0.0825088d, -2.86367d, 3.06488d,
		// -1.0963d, -0.135744d, -3.49263d, -4.16139d, -1.0991d, -1.78499d,
		// 0.0d, -4.13413d};
		//
		// double[] w = {0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d};
		// // double[] w = {0.525278d, -0.569452d, -1.27732d, 1.39469d,
		// -0.461523, 0.0689896, -3.60065, -4.11216, -1.09825, -1.78596, 0,
		// -4.06278};
		// System.out.println("************* TESTANDO LBFGS ****************");
		// AutomatedLBFGS albfgs = new AutomatedLBFGS();
		// try {
		// double[] q = albfgs.maxLbfgs(w, wpll, wpll.new WpllGradient());
		// System.out.println(Double.toString(wpll.wpll(q)));
		// System.out.println(Arrays.toString(q));
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		//
		System.out.println("************* TESTANDO APRENDIZADO DE FORMULAS ****************");
		//ParallelShortestFirst sfs = new ParallelShortestFirst(
		//		pmln.getPredicates());
		//Set<Formula> learnedF = sfs.learn();
		System.out.println("************* FORMULAS APRENDIDAS!!! ****************");
		//for (Formula f : learnedF) {
		//	System.out.println(f);
		//}

	}

}
