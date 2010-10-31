package fol;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import util.NameID;
import util.Util;

/**
 * @author Leonardo Castilho Couto
 *
 */
public final class Atom extends Formula implements NameID { // implements RandomVariable<Atom> {
	
	public final Predicate predicate;
	public final Term[] terms;
	public final double value;	
	
  public static final Atom TRUE = new Atom(null, 1, Collections.<Term>emptyList());
  public static final Atom FALSE = new Atom(null, 0, Collections.<Term>emptyList());	

	/**
	 * @param formulas
	 * @param operators
	 */
	public Atom(Predicate predicate, List<? extends Term> terms) {
		super();
		this.predicate = predicate;
		this.terms = terms.toArray(new Term[0]);
		checkArguments(predicate, this.terms);
		this.value = Double.NaN;
	}
	
	public Atom(Predicate predicate, Term ... terms) {
		super();
		this.predicate = predicate;
		this.terms = terms;
		checkArguments(predicate, terms);
		this.value = Double.NaN;
	}

	public Atom(Predicate predicate, double value, List<? extends Term> terms) {
		super();
		this.predicate = predicate;
		this.terms = terms.toArray(new Term[0]);
		checkArguments(predicate, this.terms);
		this.value = value;
	}
	
	public Atom(Predicate predicate, double value, Term ... terms) {
		super();
		this.predicate = predicate;
		this.terms = terms;
		checkArguments(predicate, terms);
		this.value = value;
	}
	
	public Atom(Atom a, double value) {
		super();
		this.predicate = a.predicate;
		this.terms = Arrays.copyOf(a.terms, a.terms.length);
		this.value = value;
	}
	
	private static void checkArguments(Predicate p, Term[] args) {
		if (args.length != p.getDomains().size()) {
			throw new IllegalArgumentException("Wrong number of arguments creating an Atom of Predicate \"" + p.toString() + "\" with arguments: " + Util.join(args, ",") + ".");
		}
		int i = 0;
		for (Term t : args) {
			if (!Domain.in(t, p.getDomains().get(i))) {
				throw new IllegalArgumentException("Incompatible Domains. Cannot put Term \"" + t.toString() + "\" with Domain(s) {" + Util.join(t.getDomain().toArray(), ",") + "} into Domain \"" + p.getDomains().get(i).toString() + "\" of Predicate \"" + p.toString() + "\".");
			}
			i++;
		}
	}

	@Override
	public String toString() {
		return predicate.getName() + "(" + Util.join(terms, ",") + ")";
	}
	
	/**
	 * @return the value
	 */
	@Override
	public double getValue() {
		if (predicate.equals(Predicate.equals)) {
			if (terms[0].equals(terms[1])) {
				return 1.0d;
			}
			return 0.0d;
		}
		return value;
	}

	/* (non-Javadoc)
	 * @see fol.Formula#getPredicates()
	 */
	@Override
	public Set<Predicate> getPredicates() {
		return Collections.singleton(predicate);
	}
	
	/* (non-Javadoc)
	 * @see fol.Formula#hasPredicate(fol.Predicate)
	 */
	@Override
	public boolean hasPredicate(Predicate p) {
		return predicate.equals(p);
	}
	
	@Override
	public Atom replaceVariables(List<Variable> x, List<Constant> c) {
		Term[] newterms = Arrays.copyOf(terms, terms.length);
		boolean replaced = false;
		boolean grounded = true;
		for (int i = 0; i < x.size(); i++) {
			for (int j = 0; j < terms.length; j++) {
				if (x.get(i).equals(newterms[j])) {
					replaced = true;
					newterms[j] = c.get(i);
				}
				if (grounded && !(newterms[j] instanceof Constant)) {
					grounded = false;
				}
			}
		}
		if (replaced) {
			Atom a = new Atom(predicate, newterms);
			if(grounded) {
				// Look into the dataset for this grounding.
				if (predicate.getGroundings().containsKey(a)) {
					return new Atom(predicate, predicate.getGroundings().get(a), newterms) ;
				}
			}
			return a;
		}
		return this;
	}

	public boolean isGround() {
		for (Term t : terms) {
			if (!(t instanceof Constant)) {
				return false;
			}
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see fol.Formula#getVariables()
	 */
	@Override
	public Set<Variable> getVariables() {
		Set<Variable> set = new HashSet<Variable>();
		for (Term t : terms) {
			if(t instanceof Variable) {
				set.add((Variable) t);
			}
		}
		return set;
	}
	
	public boolean variablesOnly() {
		for (Term t : terms) {
			if(!(t instanceof Variable)) {
				return false;
			}
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see fol.Formula#length()
	 */
	@Override
	public int length() {
		if (predicate.equals(Predicate.equals)) {
			return 0;
		}
		return 1;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		// Ignore Value
		if (obj.toString().equals(toString())) {
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public Atom copy() {
		return new Atom(this.predicate, this.value, Arrays.copyOf(this.terms,this.terms.length));
	}

//	@Override 
//	public double[] getData() {
//		Map<Atom, Double> groundings = predicate.getGroundings();
//		if (!this.variablesOnly()) {
//			throw new MyException("Cannot handle TNodes with constants yet.");
//		}
//		double[] out = new double[groundings.size()];
//		int i = 0;
//		for (Double d : groundings.values()) {
//			out[i] = d.doubleValue();
//			i++;
//		}
//		return out;
//
//	}

//	@Override
//	public double[][] getData(Atom Y, List<Atom> Z) {
//		
//		// Make a graph of all Atoms as vertices, where the atoms that share a variable
//		// are connected with a edge
//		UndirectedGraph<Atom, DefaultEdge> graph = new SimpleGraph<Atom, DefaultEdge>(DefaultEdge.class);
//		graph.addVertex(this);
//		graph.addVertex(Y);
//		if (shareVariable(this, Y)) {
//			graph.addEdge(this, Y);
//		}
//		
//		for (Atom a : Z) {
//			graph.addVertex(a);
//			if (shareVariable(this, a)) {
//				graph.addEdge(this, a);
//			}
//			if (shareVariable(Y, a)) {
//				graph.addEdge(Y, a);
//			}
//		}
//		
//		for (int i = 0; i < Z.size(); i++) {
//			for (int j = i +1; j < Z.size(); j++) {
//				if (shareVariable(Z.get(i), Z.get(j))) {
//					graph.addEdge(Z.get(i), Z.get(j));
//				}
//			}
//		}
//		// End construction of graph
//
//		// If there is no path between X and Y, there is no data for then
//		if (DijkstraShortestPath.findPathBetween(graph, this, Y) == null) {
//			return null;
//		}
//		
//		Map<Variable, Constant> ground = new HashMap<Variable,Constant>();
//		Map<Atom, Variable[]> aVars = new HashMap<Atom, Variable[]>();
//		Map<Atom, Constant[]> aCons = new HashMap<Atom, Constant[]>();
//		Map<Atom, Double> lastValue = new HashMap<Atom, Double>();
//		
//		List<Atom> atoms = new ArrayList<Atom>(Z);
//		atoms.add(this);
//		atoms.add(Y);
//		Map<Atom, Boolean> connected = new HashMap<Atom, Boolean>((int) (Math.ceil(atoms.size()*1.4)));
//		connected.put(this, true);
//		connected.put(Y, true);
//		Set<Variable> vSet = new HashSet<Variable>(getVariables());
//		aVars.put(this, vSet.toArray(new Variable[vSet.size()]));
//		aCons.put(this, new Constant[vSet.size()]);
//		{
//			Set<Variable> v = Y.getVariables();
//			vSet.addAll(v);
//			aVars.put(Y, v.toArray(new Variable[v.size()]));
//			aCons.put(Y, new Constant[v.size()]);
//		}
//		for (Atom a : Z) {
//			if (DijkstraShortestPath.findPathBetween(graph, this, a) == null) {
//				connected.put(a, false);
//			} else {
//				connected.put(a, true);
//				Set<Variable> v = a.getVariables();
//				vSet.addAll(v);
//				aVars.put(a, v.toArray(new Variable[v.size()]));
//				aCons.put(a, new Constant[v.size()]);
//			}
//		}
//		
//		// initialize sampler
//		List<Variable> variables = new ArrayList<Variable>(vSet);
//		List<Set<Constant>> cList = new ArrayList<Set<Constant>>();
//		for (Variable v : variables) {
//			cList.add(v.getConstants());
//		}
//		Sampler<Constant> sampler = new Sampler<Constant>(cList);
//		
//		//Constant[] c = new Constant[var.length];
//		List<double[]> out = new ArrayList<double[]>();
//		boolean converged = false;
//
//		constants:
//		for (List<Constant> constants : sampler) {
//			// map between the Variable and the sampled Constant
//			for (int i = 0; i < constants.size(); i++) {
//				ground.put(variables.get(i), constants.get(i));
//			}
//			
//			for (Atom a : atoms) {
//				if (connected.get(a)) {
//					
//				} else { // not connected
//					
//				}
//			}
//			
//		}
//
//		constants:
//		for (long i = 0; i < n; i++) {
//			
//			List<double[]> d = new ArrayList<double[]>();
//			d.add(new double[atoms.size()]);
//			
//			for (int j = 0; j < variables.length; j++) {
//				if (counter[j] == length[j]) {
//					counter[j] = 0;
//					counter[j+1]++;
//				}
//				//c[j] = constants.get(j)[counter[j]];
//				ground.put(variables[j], constants.get(j)[counter[j]]);
//			}
//			counter[0]++;
//			
//			for (int j = 0; j < atoms.size(); j++) {
//				if (connected.get(atoms.get(j)).booleanValue()) {
//					Atom a = atoms.get(j);
//					Variable[] av = aVars.get(a);
//					Constant[] ac = aCons.get(a);
//					boolean changed = false;
//					for (int k = 0; k < av.length; k++) {
//						if (ground.get(av[k]) != ac[k]) {
//							changed = true;
//							ac[k] = ground.get(av[k]);
//						}
//					}
//					double value = 0;
//					if (changed) {
//						value = ((Atom) a.replaceVariables(Arrays.asList(av), Arrays.asList(ac))).value;
//						lastValue.put(a, value);
//					} else {
//						value = lastValue.get(a);
//					}
//					if (Double.isNaN(value)) {
//						continue constants;
//					}
//					for (double[] elem : d) {
//						elem[j] = value;
//					}
//				} else {
//					// duplicate array with 0 and 1 in the current position
//					double[][] copy = new double[d.size()][];
//					int k = 0;
//					for (double[] elem : d) {
//						copy[k] = Arrays.copyOf(elem, elem.length);
//						copy[k][j] = 1;
//						elem[j] = 0;
//						k++;
//					}
//					for (double[] elem : copy) {
//						d.add(elem);
//					}
//				}
//			}
//			out.addAll(d);			
//		}
//			
//		return out.toArray(new double[out.size()][]);
//	}
	
	@SuppressWarnings("unused")
	private static boolean shareVariable(Atom a0, Atom a1) {
		for (Variable v0 : a0.getVariables()) {
			for (Variable v1 : a1.getVariables()) {
				if (v0.equals(v1)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public String getName() {
		return toString();
	}

}
