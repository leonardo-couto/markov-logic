package fol.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fol.Atom;
import fol.Domain;
import fol.Formula;
import fol.FormulaFactory;
import fol.Literal;
import fol.Predicate;
import fol.Term;
import fol.Variable;
import fol.operator.Biconditional;
import fol.operator.BinaryOperator;
import fol.operator.Conjunction;
import fol.operator.Disjunction;
import fol.operator.Negation;

public class FormulaParser {
	
	private final Map<String, Predicate> predicates;
	
	public FormulaParser(Set<Predicate> predicates) {
		this.predicates = new HashMap<String, Predicate>();
		for (Predicate p : predicates) {
			this.predicates.put(p.toString(), p);
		}
	}
	
	
	public Formula parse(String formula) {
		Map<String, Variable> variables = new HashMap<String, Variable>();
		char[] chars = formula.replaceAll("\\s","").toCharArray();
		Formula f = this.parse(chars, variables);
		if (f == null) throw new RuntimeException("Malformed formula " + formula);
		return f;		
	}
	
	private Formula parse(char[] chars, Map<String, Variable> vars) {
		List<Formula> formulas = new ArrayList<Formula>();
		List<BinaryOperator> operators = new ArrayList<BinaryOperator>();
		boolean neg = false;
		
		for (int i = 0; i < chars.length; i++) {
			switch (chars[i]) {
			
			// operators
			case '!': 
				neg = !neg; 
				break;
			case 'v' :
				if (neg) return null;
				if (operators.size() + 1 != formulas.size()) return null;
				operators.add(Disjunction.OPERATOR);
				break;
			case '^' :
				if (neg) return null;
				if (operators.size() + 1 != formulas.size()) return null;
				operators.add(Conjunction.OPERATOR);
				break;
			case '<' :
				if (chars[i+1] != '=' || chars[i+2] != '>') return null;
				if (neg) return null;
				if (operators.size() + 1 != formulas.size()) return null;
				operators.add(Disjunction.OPERATOR);
				i = i+2;
				break;
				
			// formula
			case '(':
				if (formulas.size() != operators.size()) return null;
				int closing = this.findClosingParenthesis(i, chars);
				if (closing == -1) return null;
				Formula formula = this.parse(Arrays.copyOfRange(chars, i+1, closing), vars);
				if (formula == null) return null;
				if (neg) {
					formula = Negation.OPERATOR.apply(formula);
					neg = false;
				}
				formulas.add(formula);
				i = closing;
				break;

			// literal
			default:
				if (formulas.size() != operators.size()) return null;
				int end = this.findClosingParenthesis(i, chars);
				Atom atom = this.parseAtom(new String(Arrays.copyOfRange(chars, i, end)), vars);
				if (atom == null) return null;
				formulas.add(neg ? new Literal(atom, false) : atom);
				neg = false;
				i = end;
				break;
			}			
		}
		
		if (formulas.isEmpty() || formulas.size() == operators.size()) return null;
		
		return this.applyPrecedence(formulas, operators);
	}
	
	/**
	 * <p>Reduce a list of <code>n</code> formulas and <code>n - 1</code> operators to 
	 * a single formula <code>(formula_1 operator_1 formula_2 ... operator_n-1 formula_n)</code>.</p>
	 * <p>Operators precedence:<br>
	 * 1 - negation<br>
	 * 2 - conjunction<br>
	 * 3 - disjunction<br>
	 * 4 - implication<br>
	 * 5 - biconditional</p>
	 * @param formulas
	 * @param operators
	 * @return reduced formula
	 */
	private Formula applyPrecedence(List<Formula> formulas, List<BinaryOperator> operators) {
		if (formulas.size() == 1) return formulas.get(0);
		
		RawFormula raw = this.applyPrecedence(formulas, operators, Conjunction.OPERATOR);
		raw = this.applyPrecedence(raw.formulas, raw.operators, Disjunction.OPERATOR);
		raw = this.applyPrecedence(raw.formulas, raw.operators, Biconditional.OPERATOR);
		
		return (raw.formulas.size() != 1) ? null : raw.formulas.get(0);
	}
	
	private RawFormula applyPrecedence(List<Formula> formulas, 
			List<BinaryOperator> operators, BinaryOperator operator) {
		
		Deque<Formula> reducedFormulas = new LinkedList<Formula>();
		Deque<BinaryOperator> reducedOperators = new LinkedList<BinaryOperator>();
		
		reducedFormulas.offer(formulas.get(0));
		
		for (int i = 1; i < formulas.size(); i++) {
			BinaryOperator op = operators.get(i-1);
			Formula f = formulas.get(i);
			
			if (op == operator) {
				Formula f1 = reducedFormulas.pollLast();
				reducedFormulas.offerLast(op.apply(f, f1));
			} else {
				reducedFormulas.offerLast(f);
				reducedOperators.offerLast(op);
			}
		}
		
		formulas = new ArrayList<Formula>(reducedFormulas);
		operators = new ArrayList<BinaryOperator>(reducedOperators);
		
		return new RawFormula(formulas, operators);
	}
	
	private Atom parseAtom(String atom, Map<String, Variable> vars) {
		String[] tokens = tokenizer(atom);
		Predicate p = this.predicates.get(tokens[0]);
		if (p == null) return null;
		List<Domain> domains = p.getDomains();
		if (tokens.length -1 != domains.size()) return null;
		
		Term[] terms = new Term[domains.size()];
		for (int i = 1; i < tokens.length; i++) {
			String token = tokens[i];
			Domain d = domains.get(i-1);

			Variable v = vars.get(token);
			if (v == null) {
				v = FormulaFactory.newVariableNotIn(d, vars.values());
				vars.put(token, v);
			} else {
				if (!Domain.contains(v.getDomain(), d)) return null;
			}
			terms[i-1] = v;
		}
		
		return new Atom(p, terms);
	}
	
	private int findClosingParenthesis(int begin, char[] chars) {
		
		int open = 0;
		
		for (int i = begin; i < chars.length; i++) {
			if (chars[i] == '(') open++;
			else if (chars[i] == ')') {
				open--;
				if (open == 0) return i;
			}
		}
		
		return -1;	
	}
	
	// Separates a String in the format "name0(name1,...,namen)"
	// in a array of names String.
	private static String[] tokenizer(String atom) {
		String[] tokens = atom.split("[(,)]");
		return tokens;
	}
	
	private static class RawFormula {
		
		public final List<Formula> formulas;
		public final List<BinaryOperator> operators;
		
		public RawFormula(List<Formula> formulas, List<BinaryOperator> operators) {
			this.formulas = formulas;
			this.operators = operators;
		}
		
	}
	
}
