package fol;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import stat.Distribution;
import stat.RandomVariable;
import util.NameID;
import util.Util;

/**
 * @author Leonardo Castilho Couto
 *
 */
public final class Atom extends Formula implements NameID, RandomVariable<Atom> {
	
	public final Predicate predicate;
	public final Term[] terms;
	public final double value;	
	
  public static final Atom TRUE = new Atom(Predicate.empty, 1, Collections.<Term>emptyList());
  public static final Atom FALSE = new Atom(Predicate.empty, 0, Collections.<Term>emptyList());	

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
		if (this == Atom.FALSE) return "false";
		if (this == Atom.TRUE) return "true";
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
		Term[] newTerms = Arrays.copyOf(terms, terms.length);
		boolean replaced = false;
		boolean grounded = true;
		for (int i = 0; i < this.terms.length; i++) {
			if (terms[i] instanceof Variable) {
				for (int j = 0; j < x.size(); j++) {
					if (terms[i].equals(x.get(j))) {
						replaced = true;
						newTerms[i] = c.get(j);
						break;
					}
				}
				if (grounded && !(newTerms[i] instanceof Constant)) {
					grounded = false;
				}
			}
		}

		if (replaced) {
			Atom a = new Atom(predicate, newTerms);
			if(grounded) {
				// Look into the dataset for this grounding.
				if (predicate.getGroundings().containsKey(a)) {
					return new Atom(predicate, predicate.getGroundings().get(a), newTerms) ;
				}
			}
			return a;
		}
		return this;
	}

	/**
	 * Check whether this Atom is grounded (all terms are Constants)
	 * @return true if the Atom does not contain any Variables
	 */
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
	
	@Override
	public String getName() {
		return toString();
	}
	
	@Override
	public List<Atom> getAtoms() {
		return Collections.singletonList(this);
	}
	
	@Override
	public Class<? extends Distribution<Atom>> getDistributionClass() {
		return AtomDistribution.class;
	}

}
