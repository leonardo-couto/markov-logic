package markovLogic.inference;

import java.util.ArrayList;
import java.util.List;

import markovLogic.GroundedMarkovNetwork;
import markovLogic.Grounder;
import markovLogic.MarkovLogicNetwork;
import stat.sampling.CrossJoinSampler;
import stat.sampling.Sampler;
import fol.Atom;
import fol.Constant;
import fol.Domain;
import fol.Formula;
import fol.Predicate;
import fol.Variable;
import fol.WeightedFormula;
import fol.database.Database;
import fol.database.SimpleDB;
import fol.operator.Conjunction;
import fol.operator.Disjunction;
import fol.operator.Negation;

public class ExactInference implements Inference {
	
	private final MarkovLogicNetwork mln;
	
	public ExactInference(MarkovLogicNetwork mln) {
		this.mln = mln;
	}
	
	// 1 - fazer primeiro para um Atom grounded - FEITO
	// 2 - depois para um Atom com variavel
	// 3 - depois para formulas mais complexas groundeds
	// 4 - depois para formulas mais complexas com variaveis
	
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
	
	// grounded atom a
	// TODO: ver o que fazer para o caso de o mesmo predicado aparecer duas vezes
	// na mesma formula, exemplo: P(X) && R(X,Y) && P(Y).
	
	/**
	 * <p>Computes <code>p(g | e) == p(g && e) / p(e)</code><br>
	 * <code> == p(g && e) / [p(g && e) + p(!g && e)]</code>,<br>
	 * where <code>g</code> stands for <code>ground</code> and 
	 * <code>e</code> for <code>evidence</code>.</p>
	 */
	@Override
	public double pr(Atom ground, Evidence evidence) {
		Grounder grounder = new Grounder(this.mln, evidence);
		GroundedMarkovNetwork mrf = grounder.ground(ground);
		
		List<Atom> variables = mrf.getGroundings();
		Sampler<Boolean> sampler = this.getSampler(variables.size());
		Database world = new SimpleDB();
		
		double sumP = 0;  // usar bigDecimal?
		double sumN = 0;
		
		for (List<Boolean> values : sampler) {
			boolean value = false;
			for (int i = 0; i < variables.size(); i++) {
				Atom atom = variables.get(i);
				boolean b = values.get(i).booleanValue();
				world.set(atom, b);
				if (atom == ground) value = b;
			}
			if (value) sumP += Math.exp(mrf.sumWeights(world));
			else sumN += Math.exp(mrf.sumWeights(world));
		}
		
		double pEvidence = sumP + sumN;
		if (Math.abs(pEvidence) < 1e-15) throw new ArithmeticException("Probability of evidence is zero");
		
		return sumP / (pEvidence);
	}
	
	public Sampler<Boolean> getSampler(int size) {
		List<Boolean> truthValues = new ArrayList<Boolean>(2);
		truthValues.add(Boolean.TRUE);
		truthValues.add(Boolean.FALSE);
		List<List<Boolean>> domains = new ArrayList<List<Boolean>>(size);
		for (int i = 0; i < size; i++) {
			domains.add(truthValues);
		}
		return new CrossJoinSampler<Boolean>(domains);
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
		
		List<WeightedFormula<?>> wfs = new ArrayList<WeightedFormula<?>>();
		wfs.add(new WeightedFormula<Formula>(f1, 0));
		wfs.add(new WeightedFormula<Formula>(f2, 0));
		wfs.add(new WeightedFormula<Formula>(f3, 0));
		wfs.add(new WeightedFormula<Formula>(f4, 0));
		wfs.add(new WeightedFormula<Formula>(f5, 0.6931));
		wfs.add(new WeightedFormula<Formula>(f6, 2.3026));
		MarkovLogicNetwork mln = new MarkovLogicNetwork(wfs);

		
		Inference infer = new ExactInference(mln);
		Constant c0 = new Constant("c0", da);
		Constant c1 = new Constant("c1", db);
		Constant c2 = new Constant("c2", dc);

		Atom e0 = new Atom(r, c0, c2);
		Atom e1 = new Atom(q, c0, c1);
		
		Database values = new SimpleDB();
		values.set(e0, false);
		values.set(e1, false);
		
		Evidence evidence = new Evidence(values);
		evidence.set(e0, true);
		evidence.set(e1, true);
		
		System.out.println("p(c0): " + infer.pr(new Atom(p, c0), evidence));
		System.out.println("q(c0,c1): " + infer.pr(new Atom(q, c0, c1), evidence));
		double pr = infer.pr(new Atom(s, c2), new Evidence(new SimpleDB()));
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
