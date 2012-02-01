package fol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fol.database.Database;
import fol.operator.Disjunction;
import fol.operator.Negation;

public class ConjunctiveNormalForm implements Formula, Comparable<ConjunctiveNormalForm> {
	
	private final Atom[] literals;
	private final boolean[] negated;
	private static final String DISJUNCTION_CONNECTOR = " v ";
	private static final Character NEGATION = '!';
	
	/**
	 * Represents a formula in conjunctive normal form (CNF). A CNF formula is 
	 * a disjunction of literals. A literal is an Atom, or its negation.
	 * @param literals 
	 * @param negated
	 */
	public ConjunctiveNormalForm(Atom[] atoms, boolean[] negated) {
		Literal[] sortedLiterals = Literal.sortLiterals(atoms, negated);
		for (int i = 0; i < sortedLiterals.length; i++) {
			Literal literal = sortedLiterals[i];
			atoms[i] = literal.atom;
			negated[i] = literal.negated;
		}
		this.literals = atoms;
		this.negated = negated;
	}
	
	/**
	 * Extends this clause by adding another literal.
	 * If this clause already contains a, return this clause unmodified.
	 * @param a Atom
	 * @param negated true for a negated literal
	 * @return
	 */
	public ConjunctiveNormalForm addLiteral(Atom a, boolean negated) {
		int i = Arrays.binarySearch(this.literals, a);
		if (i >= 0) {
			return this;
		}
		int length = this.literals.length+1;
		Atom[] literals = Arrays.copyOf(this.literals, length);
		boolean[] nliterals = Arrays.copyOf(this.negated, length);
		
		Atom aux;
		boolean b;
		for (int j = -i -1; j < length; j++) {
			aux = literals[j];
			b = nliterals[j];
			literals[j] = a;
			nliterals[j] = negated;
			a = aux;
			negated = b;
		}
		
		return new ConjunctiveNormalForm(literals, nliterals);
	}
	
	@Override
	public int compareTo(ConjunctiveNormalForm o) {
		int length = Math.min(this.literals.length, o.literals.length);
		for (int i = 0; i < length; i++) {
			if (!this.literals[i].equals(o.literals[i])) {
				return this.literals[i].compareTo(o.literals[i]);
			}
			if (this.negated[i] != o.negated[i]) {
				return this.negated[i] ? 1 : -1;
			}
		}
		if (this.literals.length == o.literals.length) {
			return 0;
		}
		return this.literals.length > o.literals.length ? 1 : -1;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o instanceof ConjunctiveNormalForm) {
			ConjunctiveNormalForm other = (ConjunctiveNormalForm) o;
			if (this.literals.length != other.literals.length) {
				return false;
			}
			for (int i = 0; i < this.literals.length; i++) {
				if ((this.negated[i] != other.negated[i]) || 
						!this.literals[i].equals(other.literals[i])) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public List<Atom> getAtoms() {
		return Arrays.asList(this.literals);
	}

	@Override
	public List<FormulaComponent> getComponents() {
		int length = this.literals.length;
		if(length > 1) {
			List<FormulaComponent> components = new ArrayList<FormulaComponent>(3*length-1);
			components.add(this.literals[0]);
			if (this.negated[0]) { components.add(Negation.OPERATOR); }
			for (int i = 1; i < length; i++) {
				components.add(this.literals[i]);
				if (this.negated[i]) { components.add(Negation.OPERATOR); }
				components.add(Disjunction.OPERATOR);
			}
			return components;
		}
		return Collections.<FormulaComponent>singletonList(this.literals[0]);
	}

	@Override
	public Set<Predicate> getPredicates() {
		Set<Predicate> predicates = new HashSet<Predicate>(this.literals.length*2);
		for (Atom a : this.literals) {
			predicates.add(a.predicate);
		}
		return predicates;
	}

	@Override
	public boolean getValue(Database db) {
		for (int i = 0; i < this.literals.length; i++) {
			if (db.valueOf(this.literals[i]) != this.negated[i]) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean getValue(Database db, Map<Variable, Constant> groundings) {
		Atom a;
		for (int i = 0; i < this.literals.length; i++) {
			a = this.literals[i];
			a = a.isGrounded() ? a : a.ground(groundings);
			if (db.valueOf(a) != this.negated[i]) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Set<Variable> getVariables() {
		Set<Variable> variables = new HashSet<Variable>();
		for (Atom a : this.literals) {
			variables.addAll(a.getVariables());
		}
		return variables;
	}

	@Override
	public ConjunctiveNormalForm ground(Map<Variable, Constant> groundings) {
		Atom[] groundLiterals = Arrays.copyOf(this.literals, this.literals.length);
		Atom a;
		Atom ground;
		boolean changed = false;
		for (int i = 0; i < groundLiterals.length; i++) {
			a = groundLiterals[i];
			if (!a.isGrounded() && (ground = a.ground(groundings)) != a) {
				changed = true;
				groundLiterals[i] = ground;
			}
		}
		return changed ? new ConjunctiveNormalForm(groundLiterals, this.negated) : this;
	}

	@Override
	public boolean hasPredicate(Predicate p) {
		for (Atom a : this.literals) {
			if (p == a.predicate) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isGrounded() {
		for (Atom a : this.literals) {
			if (!a.isGrounded()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int length() {
		return this.literals.length;
	}

	@Override
	public List<ConjunctiveNormalForm> toCNF() {
		return Collections.singletonList(this);
	}
	
	@Override
	public String toString() {
		Deque<StringBuilder> stack = new LinkedList<StringBuilder>();
		StringBuilder sb = new StringBuilder();
		this.literals[0].print(stack);
		if (this.negated[0]) {
			sb.append(NEGATION);
		}
		sb.append(stack.pop());
		
		for (int i = 1; i < this.literals.length; i++) {
			this.literals[i].print(stack);
			sb.append(DISJUNCTION_CONNECTOR);
			if (this.negated[i]) {
				sb.append(NEGATION);
			}
			sb.append(stack.pop());			
		}
		return sb.toString();
	}

	@Override
	public double trueCount(Database db) {
		// TODO: fazer o algoritmo otimizado 
		Set<Variable> vars = this.getVariables();
		Map<Variable, Constant> groundings = new HashMap<Variable, Constant>();
		long total = 1;
		for (Variable v : vars) {
			total = total * v.getConstants().size();
		}
		int sample = total < 250 ? 2*((int) total) : 1000; // 500 for a error of at most 5%
		// TODO tirar o 2* daqui e do SimpleDB, fazer as amostras serem exatas para < 100
		
		int count = 0;
		for (int i = 0; i < sample; i++) {
			for (Variable v : vars) {
				groundings.put(v, v.getRandomConstant());
			}
			if (this.ground(groundings).getValue(db)) {
				count++;
			}
		}
		
		double ratio = ((double) count) / sample;
		return ratio * total;
	}
	
	private static class Literal implements Comparable<Literal> {
		
		public final Atom atom;
		public final boolean negated;
		
		public Literal(Atom atom, boolean negated) {
			this.atom = atom;
			this.negated = negated;
		}
		
		public static Literal[] sortLiterals(Atom[] atoms, boolean[] negated) {
			Literal[] literals = new Literal[atoms.length];
			for (int i = 0; i < literals.length; i++) {
				literals[i] = new Literal(atoms[i], negated[i]);
			}
			Arrays.sort(literals);
			return literals;
		}

		@Override
		public int compareTo(Literal o) {
			return this.atom.compareTo(o.atom);
		}		
		
	}

}
