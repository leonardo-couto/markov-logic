package markovLogic.weightLearner.wpll;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import fol.Atom;
import fol.Constant;
import fol.Formula;
import fol.Predicate;
import fol.Variable;
import fol.database.Database;
import fol.database.Groundings;

public class CountCache {
	
	private final Database db;
	private final ConcurrentHashMap<Formula, CountCache.CountData> cache;
	private final ConcurrentHashMap<Predicate, List<Atom>> groundings;
	
	public CountCache(Database db) {
		this.db = db;
		this.cache = new ConcurrentHashMap<Formula, CountCache.CountData>();
		this.groundings = new ConcurrentHashMap<Predicate, List<Atom>>();
	}
	
	public List<Count> getCounts(Formula formula, int sampleSize) {
		CountData data = this.cache.get(formula);
		if (data == null) {
			data = new CountData(formula);
			this.cache.put(formula, data);
		}
		
		Set<Predicate> predicates = data.counts.keySet();
		List<Count> counts = new ArrayList<Count>(predicates.size()*sampleSize);
		for (Predicate p : predicates) {
			if (p == Predicate.EQUALS) continue;
			this.updateCache(p, data, formula, sampleSize);
			List<Count> pCounts = data.counts.get(p);
			for (int i = 0; i < Math.min(sampleSize,pCounts.size()); i++) {
				counts.add(pCounts.get(i));
			}
		}
		data.samples = sampleSize;
		return counts;
	}
	
	public Database getDatabase() {
		return this.db;
	}
	
	private void updateCache(Predicate p, CountData data, Formula formula, int sampleSize) {

		int samples = Math.min(p.totalGroundings(), sampleSize);
		if (samples > data.samples) {
			
			List<Atom> atoms = updateGroundings(p, samples);			
			Database localDB = this.db.getLocalCopy();
			
			// target atom and its variables
			Atom target = this.getAtom(p, formula);
			Variable[] vars = Arrays.copyOf(target.terms, target.terms.length, Variable[].class);
			Map<Variable, Constant> groundings = new HashMap<Variable, Constant>();
			
			// list to be updated
			List<Count> counts = data.counts.get(p);
			
			// add formula counts for more samples (groundings)
			for (int i = data.samples; i < samples; i++) {
				
				// grounds atoms and formula
				Atom groundedAtom = atoms.get(i);
				for (int j = 0; j < vars.length; j++) {
					Variable v = vars[j];
					Constant c = (Constant) groundedAtom.terms[j];
					groundings.put(v, c);
				}
				Formula grounded = formula.ground(groundings);
				
				// make counts
				boolean value = this.db.valueOf(groundedAtom);
				localDB.flip(groundedAtom);
				double trueCount = grounded.trueCount(value ? this.db : localDB);
				double falseCount = grounded.trueCount(value ? localDB : this.db);
				localDB.flip(groundedAtom);
				double count = value ? trueCount : falseCount;
				
				// add to cache
				counts.add(new Count(groundedAtom, formula, falseCount, trueCount, count));
			}
			
		}
	}
	
	private List<Atom> updateGroundings(Predicate p, int samples) {
		List<Atom> atoms = this.groundings.get(p);
		if (atoms == null) {
			atoms = new ArrayList<Atom>();
			this.groundings.put(p, atoms);
		}
		if (samples > atoms.size()) {
			Iterator<Atom> iterator = Groundings.iterator(p);
			for (int i = atoms.size(); i < samples; i++) {
				atoms.add(iterator.next());
			}
		}
		return atoms;
	}
	
	private Atom getAtom(Predicate p, Formula formula) {
		for (Atom a : formula.getAtoms()) {
			if (a.predicate == p) {
				return a;
			}
		}
		throw new RuntimeException();
	}
	
	public void clear() {
		this.cache.clear();
	}
	

	private static final class CountData {
		
		final Map<Predicate, List<Count>> counts;
		int samples;
		
		public CountData(Formula formula) {
			this.counts = new HashMap<Predicate, List<Count>>();
			this.samples = 0;
			Set<Predicate> predicates = formula.getPredicates();
			for (Predicate p : predicates) {
				this.counts.put(p, new ArrayList<Count>());
			}
		}

	}


}
