package markovLogic.inference;

import java.util.ArrayList;
import java.util.List;

import markovLogic.GroundedMarkovNetwork;
import markovLogic.Grounder;
import markovLogic.MarkovLogicNetwork;
import stat.sampling.CrossJoinSampler;
import stat.sampling.Sampler;
import fol.Atom;
import fol.database.BinaryDatabase;
import fol.database.BinaryDB;

public class ExactInference implements Inference {
	
	private final MarkovLogicNetwork mln;
	
	public ExactInference(MarkovLogicNetwork mln) {
		this.mln = mln;
	}
	
	// 1 - fazer primeiro para um Atom grounded - FEITO
	// 2 - depois para um Atom com variavel
	// 3 - depois para formulas mais complexas groundeds
	// 4 - depois para formulas mais complexas com variaveis
	
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
		BinaryDB world = new BinaryDatabase();
		
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
	
}
