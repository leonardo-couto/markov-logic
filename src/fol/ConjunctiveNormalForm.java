package fol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fol.database.Database;
import fol.operator.Disjunction;
import fol.operator.Negation;

public class ConjunctiveNormalForm implements Formula {
	
	private final Atom[] literals;
	private final boolean[] negated;
	
	/**
	 * Represents a formula in conjunctive normal form (CNF). A CNF formula is 
	 * a disjunction of literals. A literal is an Atom, or its negation.
	 * @param literals 
	 * @param negated
	 */
	public ConjunctiveNormalForm(Atom[] literals, boolean[] negated) {
		this.literals = literals;
		this.negated = negated;
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
	

}
