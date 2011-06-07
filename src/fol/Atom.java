package fol;

import java.util.Arrays;
import java.util.Collection;
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
	
	private final String toString;
	private final int hash;

	public static final Atom TRUE = new Atom(Predicate.empty, 1, Collections.<Term>emptyList());
	public static final Atom FALSE = new Atom(Predicate.empty, 0, Collections.<Term>emptyList());	

	/**
	 * @param formulas
	 * @param operators
	 */
	public Atom(Predicate predicate, List<? extends Term> terms) {
		super();
		this.predicate = predicate;
		this.terms = terms.toArray(new Term[terms.size()]);
		this.value = Double.NaN;
		this.toString = this._toString();
		this.hash = this.toString.hashCode();
	}

	public Atom(Predicate predicate, Term ... terms) {
		super();
		this.predicate = predicate;
		this.terms = terms;
		this.value = Double.NaN;
		this.toString = this._toString();
		this.hash = this.toString.hashCode();
	}

	public Atom(Predicate predicate, double value, List<? extends Term> terms) {
		super();
		this.predicate = predicate;
		this.terms = terms.toArray(new Term[terms.size()]);
		this.value = value;
		this.toString = this._toString();
		this.hash = this.toString.hashCode();
	}

	public Atom(Predicate predicate, double value, Term ... terms) {
		super();
		this.predicate = predicate;
		this.terms = terms;
		this.value = value;
		this.toString = this._toString();
		this.hash = this.toString.hashCode();
	}

	public Atom(Atom a, double value) {
		super();
		this.predicate = a.predicate;
		this.terms = Arrays.copyOf(a.terms, a.terms.length);
		this.value = value;
		this.toString = this._toString();
		this.hash = this.toString.hashCode();
	}

//	private static void checkArguments(Predicate p, Term[] args) {
//		if (args.length != p.getDomains().size()) {
//			throw new IllegalArgumentException("Wrong number of arguments creating an Atom of Predicate \"" + p.toString() + "\" with arguments: " + Util.join(args, ",") + ".");
//		}
//		int i = 0;
//		for (Term t : args) {
//			if (!Domain.in(t, p.getDomains().get(i))) {
//				throw new IllegalArgumentException("Incompatible Domains. Cannot put Term \"" + t.toString() + "\" with Domain(s) {" + Util.join(t.getDomain().toArray(), ",") + "} into Domain \"" + p.getDomains().get(i).toString() + "\" of Predicate \"" + p.toString() + "\".");
//			}
//			i++;
//		}
//	}
	
	private String _toString() {
		if (this == Atom.FALSE) return "false";
		if (this == Atom.TRUE) return "true";
		return this.predicate.getName() + "(" + Util.join(this.terms, ",") + ")";
	}

	@Override
	public String toString() {
		return this.toString;
	}

	/**
	 * @return the value
	 */
	@Override
	public double getValue() {
		if (this.predicate == Predicate.equals) {
			if (this.terms[0] == this.terms[1]) {
				return 1.0d;
			}
			return 0.0d;
		}
		return this.value;
	}

	/* (non-Javadoc)
	 * @see fol.Formula#getPredicates()
	 */
	@Override
	public Set<Predicate> getPredicates() {
		return Collections.singleton(this.predicate);
	}

	/* (non-Javadoc)
	 * @see fol.Formula#hasPredicate(fol.Predicate)
	 */
	@Override
	public boolean hasPredicate(Predicate p) {
		return this.predicate.equals(p);
	}

	@Override
	public Atom replaceVariables(List<Variable> x, List<Constant> c) {
		int length = this.terms.length;
		Term[] newTerms = Arrays.copyOf(this.terms, length);
		boolean replaced = false;
		for (int i = 0; i < length; i++) {
			if (this.terms[i] instanceof Variable) {
				for (int j = 0; j < x.size(); j++) {
					if (this.terms[i].equals(x.get(j))) {
						replaced = true;
						newTerms[i] = c.get(j);
						break;
					}
				}
			}
		}

		return replaced ? this.predicate.getGrounding(newTerms) : this;
	}

	/**
	 * Check whether this Atom is grounded (all terms are Constants)
	 * @return true if the Atom does not contain any Variables
	 */
	public boolean isGround() {
		for (Term t : this.terms) {
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
		for (Term t : this.terms) {
			if(t instanceof Variable) {
				set.add((Variable) t);
			}
		}
		return set;
	}

	public boolean variablesOnly() {
		for (Term t : this.terms) {
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
		if (this.predicate == Predicate.empty) {
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
		if (obj != null && obj instanceof Atom) {
			Atom o = (Atom) obj;
			if (this.predicate.equals(o.predicate)) {
				return this.hash == o.hash;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return this.hash;
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
	
	public static Set<Predicate> getPredicates(Collection<Atom> atoms) {
		Set<Predicate> set = new HashSet<Predicate>();
		for (Atom a : atoms) {
			set.add(a.predicate);
		}
		return set;
	}

}
