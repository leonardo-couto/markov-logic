package fol;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fol.database.Database;
import fol.operator.Operator;

public final class Atom implements Formula, FormulaComponent, Comparable<Atom> {

	private final boolean grounded;
	public final Predicate predicate;
	public final Term[] terms;
	
	private static final Character COMMA = ',';
	
	public Atom(Predicate predicate, Term ... terms) {
		this.predicate = predicate;
		this.terms = terms;
		for (Term t : terms) {
			if (!(t instanceof Constant)) {
				this.grounded = false;
				return;
			}
		}
		this.grounded = true;
	}
	
	@Override
	public int compareTo(Atom o) {
		if (this.predicate == o.predicate) {
			Term t1;
			Term t2;
			for (int i = 0; i < this.terms.length; i++) {
				t1 = this.terms[i];
				t2 = o.terms[i];
				if (t1 != t2) {
					return t1.toString().compareTo(t2.toString());
				}
			}
			return 0;
		}
		return this.predicate.toString().compareTo(o.predicate.toString());
	}
	
	@Override
	public void evaluate(Deque<Boolean> stack, Database db) {
		stack.push(this.getValue(db));
	}

	@Override
	public List<Atom> getAtoms() {
		return Collections.singletonList(this);
	}
	
	@Override
	public List<FormulaComponent> getComponents() {
		return Collections.<FormulaComponent>singletonList(this);
	}
	
	@Override
	public Set<Predicate> getPredicates() {
		return Collections.singleton(this.predicate);
	}
	
	public static Set<Predicate> getPredicates(Collection<Atom> atoms) {
		Set<Predicate> set = new HashSet<Predicate>();
		for (Atom a : atoms) {
			set.add(a.predicate);
		}
		return set;
	}
	
	@Override
	public boolean getValue(Database db) {
		if (!this.grounded) {
			throw new RuntimeException("getValue of non-grounded Atom " + this.toString());
		}
		return db.valueOf(this);
	}

	@Override
	public boolean getValue(Database db, Map<Variable, Constant> groundings) {
		Atom a = this.ground(groundings);
		return a.getValue(db);
	}

	@Override
	public Set<Variable> getVariables() {
		Set<Variable> v = new HashSet<Variable>();
		for (Term t : this.terms) {
			if (t instanceof Variable) {
				v.add((Variable) t);
			}
		}
		return v;
	}

	@Override
	public boolean hasPredicate(Predicate p) {
		return this.predicate == p;
	}

	@Override
	public boolean isGrounded() {
		return this.grounded;
	}

	@Override
	public int length() {
		return 1;
	}
	
	@Override
	public void print(Deque<StringBuilder> stack) {
		StringBuilder b = new StringBuilder();
		b.append(this.predicate.toString()).append(Operator.LEFTP);
		for (Term t : terms) {
			b.append(t.toString());
			b.append(COMMA);
		}
		b.setCharAt(b.length()-1, Operator.RIGHTP);
		stack.push(b);
	}

	@Override
	public Atom ground(Map<Variable, Constant> groundings) {
		if (this.grounded) return this;
		int length = this.terms.length;
		boolean modified = false;
		
		Term[] groundedTerms = new Term[length];
		for (int i = 0; i < length; i++) {
			Term t = this.terms[i];
			Constant candidate = groundings.get(t);
			boolean grounded = (candidate != null);
			groundedTerms[i] = grounded ? candidate : t;
			modified = modified || grounded;
		}
		
		return modified ? new Atom(this.predicate, groundedTerms) : this;
	}
	
	@Override
	public List<ConjunctiveNormalForm> toCNF() {
		ConjunctiveNormalForm formula = new ConjunctiveNormalForm(
				new Atom[] {this}, 
				new boolean[] {false}
				);
		return Collections.singletonList(formula);
	}
	
	public static void main(String[] args) {
		for (int i = 0; i < 11; i++) {
			System.out.println(1 << i);
		}
	}

}
