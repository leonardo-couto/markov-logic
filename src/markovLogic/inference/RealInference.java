package markovLogic.inference;

import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import fol.Atom;
import fol.Predicate;
import fol.database.BinaryDB;
import fol.database.BinaryDatabase;
import fol.database.Groundings;
import fol.database.RealDB;
import markovLogic.MarkovLogicNetwork;

public class RealInference implements Inference {
	
	private final MarkovLogicNetwork mln;
	private final RealDB database;
	private final Inference inference;
	
	public RealInference(MarkovLogicNetwork mln, RealDB database, Inference inference) {
		this.mln = mln;
		this.database = database;
		this.inference = inference;
	}
	
	@Override
	public double pr(Atom ground, Evidence evidence) {

		double sum = 0.0d;
		double expected = 0.0d;
		
		int i = 0;
		while (sum < 0.5d && (i++ < 100)) {
			Sample sample = this.getSample(evidence);
			evidence.setDatabase(sample.values);
			double infer = this.inference.pr(ground, evidence);
			sum += sample.probability;
			expected += sample.probability*infer;
		}
		
		return expected / sum;
	}
	
	private Sample getSample(Evidence evidence) {
		Set<Predicate> predicates = this.mln.getPredicates();
		
		double pr = 1.0d;
		Random random = new Random();
		BinaryDB binaryValues = new BinaryDatabase();

		for (Predicate p : predicates) {
			Iterator<Atom> grounds = Groundings.iterator(p, true);
			if (evidence.isEvidence(p)) {
				while(grounds.hasNext()) {
					Atom atom = grounds.next();
					double prob = this.setEvidence(atom, binaryValues, random);
					if (prob != 1.0d) pr *= prob;	
				}				
			} else {
				while(grounds.hasNext()) {
					Atom atom = grounds.next();
					if (evidence.isEvidence(atom)) {
						double prob = this.setEvidence(atom, binaryValues, random);
						if (prob != 1.0d) pr *= prob;	
					}					
				}				
			}
		}
		
		return new Sample(pr, binaryValues);		
	}
	
	/**
	 * <p>Chooses a true boolean value for <code>atom</code> with probability
	 * equals to the double value of <code>atom</code> in <code>database</code>.</p>
	 * @param atom
	 * @param db
	 * @param r
	 * @return
	 */
	private double setEvidence(Atom atom, BinaryDB db, Random r) {
		double value = this.database.valueOf(atom);
		if (value == 1.0d) db.set(atom, true);
		else if (value != 0.0d) {
			boolean b = (r.nextDouble() < value);
			db.set(atom, b);
			return b ? value : 1.0d - value;
		}
		return 1.0d;
	}
	
	public static class Sample {
		
		private final double probability;
		private final BinaryDB values;
		
		public Sample(double probability, BinaryDB values) {
			this.probability = probability;
			this.values = values;
		}
		
	}

}
