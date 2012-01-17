package inference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import markovLogic.GroundedMarkovNetwork;
import markovLogic.MarkovLogicNetwork;
import markovLogic.WeightedFormula;
import stat.sampling.CrossJoinSampler;
import stat.sampling.Sampler;
import fol.Atom;
import fol.Constant;
import fol.Domain;
import fol.Formula;
import fol.Predicate;
import fol.Variable;
import fol.operator.Conjunction;
import fol.operator.Disjunction;
import fol.operator.Negation;

public class ExactInference implements Inference {
	
	private final MarkovLogicNetwork mln;
	
	public ExactInference(MarkovLogicNetwork mln) {
		this.mln = mln;
	}
	
	// 1 - fazer primeiro para um Atom grounded
	// 2 - depois para um Atom com variavel
	// 3 - depois para formulas mais complexas groundeds
	// 4 - depois para formulas mais complexas com variaveis
	
	public double pr(Atom a, Set<Formula> given) {
		// TODO: se p(given) = 0, retornar excessão.
		// p(a && given) / p(given) == p (a && given) / [p(a && given) + p(!a && given)]

		
		// comecar com um exemplo simples:
		//
		// w1 : P(X,Y) && Q(Y,Z)
		// w2 : P(X,Y) && R(X)
		//
		// X = {A}, Y = {B}, Z = {C}
		//
		// pr(Q(B,C) | R(A)) = ?
		//
		// = p(q && r)* / [p(!q && r) + p(q && r)]
		// * = p (q && r && p) + p (q && r && !p) =
		// e(w1 x 1) x e(w2 x 1) +  e(w1 x 0) x e(w2 x 0)
		//
		return 0;
	}

	@Override
	public double pr(Formula f, Set<Atom> given) {
		if (f instanceof Atom) {
			Atom a = (Atom) f;
			if (a.isGrounded()) {
				return this.prAtom(a, given);
			}
		}
		// TODO Auto-generated method stub
		return 0;	
	}

	@Override
	public double pr(Formula f) {
		return this.pr(f, Collections.<Atom>emptySet());
	}
	
	// grounded atom a
	// TODO: ver o que fazer para o caso de o mesmo predicado aparecer duas vezes
	// na mesma formula, exemplo: P(X) && R(X,Y) && P(Y).
	private double prAtom(Atom a, Set<Atom> given) {
		GroundedMarkovNetwork groundedMln = this.mln.ground(a, given);
		int totalAtoms = groundedMln.getGroundings().size();
		
		
		// create sampler
		Sampler<Atom> sampler = getTFSampler(totalAtoms);
		List<Double> wt = new ArrayList<Double>();
		List<Double> wf = new ArrayList<Double>();	
		for (List<Atom> sample : sampler) {
			double wsum = groundedMln.sumWeights(sample);
			if (sample.get(0) == Atom.TRUE) {
				wt.add(wsum);
			} else {
				wf.add(wsum);
			}
		}
		
		Collections.sort(wt);
		Collections.sort(wf);
		double sumt = 0;
		double sumf = 0;
		for (Double d : wt) {
			sumt = sumt + Math.exp(d);
		}
		for (Double d : wf) {
			sumf = sumf + Math.exp(d);
		}

		return (sumt / (sumt + sumf));
	}
	
	private static Sampler<Atom> getTFSampler(int dimensions) {
		ArrayList<List<Atom>> domain = new ArrayList<List<Atom>>(dimensions);
		List<Atom> truefalse = Arrays.asList(new Atom[] {Atom.TRUE, Atom.FALSE});
		for (int i = 0; i < dimensions; i++) {
			domain.add(truefalse);
		}
		return new CrossJoinSampler<Atom>(domain);
	}
	
	public static void main (String[] args) {
		
//		Domain person = new Domain("person");
//		Domain title = new Domain("title");
//		Domain phase = new Domain("phase");
//		Domain position = new Domain("position");
//		
//		Predicate hasPosition = new Predicate("hasPosition", person, position);
//		Predicate advisedBy = new Predicate("advisedBy", person, person);
//		Predicate professor = new Predicate("professor", person);
//		Predicate publication = new Predicate("publication", title, person);
//		Predicate inPhase = new Predicate("inPhase", person, phase);
//		Predicate student = new Predicate("student", person);
		
		
		Domain da = new Domain("da");
		Domain db = new Domain("db");
		Domain dc = new Domain("dc");

		Predicate p = new Predicate("p", da);
		Predicate q = new Predicate("q", da, db);
		Predicate r = new Predicate("r", db, dc);
		Predicate s = new Predicate("s", dc);
		
		Variable x = da.newVariable();
		Variable y = db.newVariable();
		Variable z = dc.newVariable();
		Atom f1 = new Atom(p, x);
		Atom f2 = new Atom(q, x, y);
		Atom f3 = new Atom(r, y, z);
		Atom f4 = new Atom(s, z);
		Formula f5 = Conjunction.OPERATOR.apply(Disjunction.OPERATOR.apply(f2, f3), Negation.OPERATOR.apply(f1));
		Formula f6 = Disjunction.OPERATOR.apply(f3, f4);
		
		MarkovLogicNetwork mln = new MarkovLogicNetwork();
		mln.add(new WeightedFormula(f1, 0));
		mln.add(new WeightedFormula(f2, 0));
		mln.add(new WeightedFormula(f3, 0));
		mln.add(new WeightedFormula(f4, 0));
		mln.add(new WeightedFormula(f5, 0.6931));
		mln.add(new WeightedFormula(f6, 2.3026));
		
		Inference infer = new ExactInference(mln);
		Constant c0 = new Constant("c0", da);
		Constant c1 = new Constant("c1", db);
		Constant c2 = new Constant("c2", dc);

//		Atom e0 = new Atom(r, 0.0, c0, c2);
//		//Atom e1 = new Atom(q, 0.0, c0, c1);
//		Set<Atom> evidence = new HashSet<Atom>();
//		evidence.add(e0);
//		//evidence.add(e1);
//		System.out.println("p(c0): " + infer.pr(new Atom(p, c0), evidence));
//		System.out.println("q(c0,c1): " + infer.pr(new Atom(q, c0, c1), evidence));
		double pr = infer.pr(new Atom(s, c2));
		System.out.println("s(c2): " + pr);
		
//		Domain d0 = new Domain("d0");
//		Domain d1 = new Domain("d1");
//		Predicate p0 = new Predicate("p0", d0, d1);
//		Predicate p1 = new Predicate("p1", d0);
//		Predicate p2 = new Predicate("p2", d1);
//		Atom a0 = new Atom(p1, d0.newVariable());
//		Atom a1 = new Atom(p2, d1.newVariable());
//		Atom a2 = new Atom(p0, a0.terms[0], a1.terms[0]);
//		Atom a3 = new Atom(p0, d0.newVariable(), a1.terms[0]);
//		Atom a4 = new Atom(p1, a3.terms[0]);
//		Operator and = Conjunction.operator;
//		Operator or = Disjunction.operator;
//		Formula f = and.getFormula(and.getFormula(a0, a2),a1);
//		Formula f1 = and.getFormula(or.getFormula(a0, a2), or.getFormula(a3, a4));
//		MarkovLogicNetwork mln = new MarkovLogicNetwork();
//		mln.put(a0, 1.0d);		
//		mln.put(a1, 1.0d);
//		mln.put(a2, 1.0d);
//		mln.put(f, 100d);
//		mln.put(f1, 50.0);
//		Inference i = new ExactInference(mln);
//		i.pr(new Atom(p1, new Constant("c0", d0)));
	}

}
