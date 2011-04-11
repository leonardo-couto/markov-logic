package weightLearner.wpll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import stat.convergence.DummyTester;
import stat.convergence.SequentialTester;
import util.ListPointer;
import util.MyException;
import fol.Atom;
import fol.Constant;
import fol.Formula;
import fol.Predicate;
import fol.Term;
import fol.Variable;

public class DataCount extends ArrayList<List<FormulaCount>> {

	private static final long serialVersionUID = 8714785841871940035L;
	private final LinkedList<FormulaData> formulas;
	private final List<Atom> atoms;
	public final Predicate predicate;
	
	private int sampleSize;
	private SequentialTester tester;
	
	public DataCount(Predicate p) {
		this(p, -1);
	}
	
	public DataCount(Predicate p, int sampleSize) {
		super();
		this.predicate = p;
		this.atoms = new ArrayList<Atom>(p.getGroundings().keySet());
		Collections.shuffle(this.atoms);
		this.formulas = new LinkedList<FormulaData>();
		sampleSize = (sampleSize > 0) ? Math.min(sampleSize, this.atoms.size()) : this.atoms.size();
		this.sampleSize = sampleSize;
		for (int i = 0; i < sampleSize; i++) { this.add(new ArrayList<FormulaCount>()); }
		this.tester = new DummyTester(-1);
	}
	
	private DataCount(DataCount old) {
		super();
		this.formulas = new LinkedList<FormulaData>(old.formulas);
		this.atoms = old.atoms;
		this.predicate = old.predicate;
		this.sampleSize = old.sampleSize;
		for (List<FormulaCount> fc : old) {
			this.add(new ArrayList<FormulaCount>(fc));
		}
		this.tester = old.tester.copy();
	}
	
	/**
	 * Sample size can only be set before adding any formula.
	 * Trying to change the sample size after a formula has been added
	 * will throw an UnsupportedOperationException.
	 * @param size the size of the sample
	 */
	public void setSampleSize(int size) {
		if (!this.formulas.isEmpty()) {
			throw new UnsupportedOperationException("Cannot change sample size after a formula has been added.");
		}
		size = Math.min(size, this.atoms.size());
		if (size > this.sampleSize) {
			final int diff = size - this.sampleSize;
			for (int i = 0; i < diff; i++) {
				this.add(new ArrayList<FormulaCount>());
			}
		} else if (size != this.sampleSize) { // size < sampleSize
			for (int i = this.size(); i > size; i--) {
				this.remove(i-1);
			}
		}
		this.sampleSize = size;
	}
	
	public void setTester(SequentialTester tester) {
		this.tester = tester.copy();
	}

	public int sampledAtoms() {
		return this.sampleSize;
	}

	private ListPointer<Atom> getPointer(Formula f) {
		ListPointer<Atom> out = f.getAtomPointer(this.predicate);
		if (out == null) {
			throw new MyException("Formula \"" + f.toString() + 
					"\" contains no Predicate \"" + this.predicate.toString() + 
			"\" with Variables only.");
		}
		return out;
	}
	
	private List<Data> addAtoms() {
		FormulaData fd = this.formulas.getLast();
		Formula formula = fd.f;
		
		List<Data> out = new ArrayList<Data>(this.sampleSize);
		for (int i = 0; i < this.sampleSize; i++) {
			Atom a = this.atoms.get(i);

			if (formula instanceof Atom) {
				Data d = new Data(1.0, 0.0, a.getValue());
				this.get(i).add(new FormulaCount(formula, d));
				out.add(d);
				i++;
				continue;
			}
			
			int idx = fd.i;
			List<Variable> vars = fd.v;
			List<Constant> cons = new ArrayList<Constant>(vars.size());
			for (Term t : a.terms) { cons.add((Constant) t); }
			Formula grounded = formula.replaceVariables(vars, cons);
			List<Atom> fAtom = grounded.getAtoms();

			fAtom.set(idx, Atom.TRUE);
			double trueCount = grounded.trueCounts();
			fAtom.set(idx, Atom.FALSE);
			double falseCount = grounded.trueCounts();
			double value = a.value*trueCount + (1.0-a.value)*falseCount;

			Data d = new Data(trueCount, falseCount, value);
			this.get(i).add(new FormulaCount(formula, d));
			out.add(d);
			i++;
		}
		return out;
	}


	public void addFormula(Formula formula) {
		int i = -1;
		List<Variable> lv = null;
		
		if (!(formula instanceof Atom)) { // init atomPosition and atomVariables
			ListPointer<Atom> p = this.getPointer(formula);
			i =  p.i;
			Term[] terms = p.get().terms;
			List<Variable> vars = new ArrayList<Variable>(terms.length);
			for (Term t : terms) {
				vars.add((Variable) t);
			}
			lv = vars;
		}
		
		this.formulas.add(new FormulaData(formula, i, lv));
		this.addAtoms();
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
		public final int i;
		public final List<Variable> v;
		
		public FormulaData(Formula f, int i, List<Variable> v) {
			this.f = f;
			this.i = i;
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