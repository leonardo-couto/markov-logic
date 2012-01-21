package weightLearner.wpll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import fol.Atom;
import fol.Constant;
import fol.Formula;
import fol.FormulaFactory;
import fol.Predicate;
import fol.Term;
import fol.Variable;
import fol.database.Database;

/**
 * This is an auxiliary class that stores a list of grounding atoms 
 * for a specific Predicate. For each grounding, it stores a list of
 * true counts for Formulas (given the grounding's Markov Blanket).
 * 
 * The information stored here is latter used to compute the Predicate
 * Pseudo-log-likelihood. 
 */
public class DataCount extends ArrayList<List<FormulaCount>> {

	private static final long serialVersionUID = 8714785841871940035L;
	
	private final LinkedList<FormulaData> formulas;
	private final List<Atom> atoms;
	private final Database db;
	public final Predicate predicate;
	private final int sampleSize;
	
	public DataCount(Predicate p, Database db, int sampleSize) {
		super();
		this.predicate = p;
		this.db = db;
		
		if (sampleSize < 1) { throw new IllegalArgumentException("Sample size must be greater than 0"); }
		sampleSize = (int) Math.min(p.totalGroundings(), sampleSize);
		this.atoms = new ArrayList<Atom>(sampleSize);
		
		// pega uma lista aleatoria de grounds do predicado p
		Iterator<Atom> it = this.db.groundingIterator(FormulaFactory.generateAtom(p));
		for (int i = 0; i < sampleSize; i++) { this.atoms.add(it.next()); }
		
		this.formulas = new LinkedList<FormulaData>();
		this.sampleSize = sampleSize;
		for (int i = 0; i < sampleSize; i++) { this.add(new ArrayList<FormulaCount>()); }
	}
	
	private DataCount(DataCount old) {
		super();
		this.formulas = new LinkedList<FormulaData>(old.formulas);
		this.atoms = old.atoms;
		this.predicate = old.predicate;
		this.sampleSize = old.sampleSize;
		this.db = old.db;
		for (List<FormulaCount> fc : old) {
			this.add(new ArrayList<FormulaCount>(fc));
		}
	}
	
	public int sampledAtoms() {
		return this.sampleSize;
	}
	
	private void addAtoms(FormulaData fd) {
		Formula formula = fd.f;
		Database localDB = this.db.getLocalCopy();
		
		for (int i = 0; i < this.sampleSize; i++) {
			Atom a = this.atoms.get(i);
			boolean value = db.valueOf(a);

			if (formula instanceof Atom) {
				Data d = new Data(1.0, 0.0, value ? 1.0 : 0.0);
				this.get(i).add(new FormulaCount(formula, d));
				i++;
				continue;
			}
			
			List<Variable> vars = fd.v;
			Map<Variable, Constant> groundings = new HashMap<Variable, Constant>();
			for (int j = 0; j < a.terms.length; j++) {
				groundings.put(vars.get(j), (Constant) a.terms[j]);
			}

			Formula grounded = formula.ground(groundings);
			localDB.flip(a);
			double trueCount = grounded.trueCount(value ? this.db : localDB);
			double falseCount = grounded.trueCount(value ? localDB : this.db);
			localDB.flip(a);
			double count = value ? trueCount : falseCount;
			//double value = a.value*trueCount + (1.0-a.value)*falseCount;

			Data d = new Data(trueCount, falseCount, count);
			this.get(i).add(new FormulaCount(formula, d));
			i++;
		}
	}


	public void addFormula(Formula formula) {
		List<Variable> lv = new ArrayList<Variable>();
		
		if (!(formula instanceof Atom)) { // init atomPosition and atomVariables
			List<Atom> atoms = formula.getAtoms();
			outer: for (int j = 0; j < atoms.size(); j++) {
				Atom a = atoms.get(j);
				if (a.predicate == this.predicate) {
					for (Term t : a.terms) {
						if (t instanceof Variable) {
							lv.add((Variable) t);
						} else {
							lv.clear();
							continue outer;
						}
					}
					break outer;
				}
			}
			if (lv.isEmpty()) throw new RuntimeException("Formula doesn't have a variable " + 
					"only Atom of Predicate " + this.predicate.toString());					
		}
		
		FormulaData fd = new FormulaData(formula, lv);
		this.formulas.add(fd);
		this.addAtoms(fd);
	}

	public boolean removeFormula(Formula f) {
		
		if(FormulaData.remove(this.formulas, f)) {
			for (List<FormulaCount> fc : this) {
				FormulaCount.remove(fc, f);
			}
			return true;
		}
		return false;
	}
	
	public DataCount copy() {
		DataCount copy = new DataCount(this);
		return copy;
	}
	
	private static final class FormulaData {
		public final Formula f;
		public final List<Variable> v;
		
		public FormulaData(Formula f, List<Variable> v) {
			this.f = f;
			this.v = v;
		}
		
		@Override
		public boolean equals(Object o) {
			return this.f == o;
		}
		
		public static boolean remove(LinkedList<FormulaData> l, Formula f) {
			ListIterator<FormulaData> it = l.listIterator(l.size());
			while (it.hasPrevious()) {
				if (it.previous().f == f) {
					it.remove();
					return true;
				}
			}
			return false;
		}
	}
	
}