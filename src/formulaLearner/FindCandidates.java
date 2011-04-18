package formulaLearner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import fol.Atom;
import fol.Formula;
import fol.Term;
import fol.Variable;
import fol.operator.Disjunction;

public class FindCandidates implements Runnable {
	
	private final Set<Atom> atoms;
	private final Queue<Formula> seeds;
	private final Queue<Formula> candidates;
	private final BlockingQueue<Formula> naiveCandidates;
	private final int threads;
	
	public static final Formula END = Atom.FALSE; 
	
	public FindCandidates(Set<Atom> atoms, List<Formula> seeds, Queue<Formula> candidates, int threads) {
		this.atoms = atoms;
		this.seeds = new ConcurrentLinkedQueue<Formula>(seeds);
		this.candidates = candidates;
		this.naiveCandidates = new LinkedBlockingQueue<Formula>();
		this.threads = threads;
	}
	
	@Override
	public void run() {
		int producers = Math.max(1, this.threads-1);
		CountDownLatch done = new CountDownLatch(producers);
		for (int i = 0; i < producers; i++) {
			new Thread(new NaiveCandidates(this.atoms, this.seeds, this.naiveCandidates, done)).start();
		}
		new Thread(new ProducersWatcher<Formula>(this.naiveCandidates, done, END)).start();
		this.checkCandidates();
	}
	
	/**
	 * Check the naiveCandidates for duplicates and store the ones that 
	 * does not contain the same atoms as any other candidate in the 
	 * candidates Queue.
	 * Neither atoms order or operators matter here.
	 * At the end, appends this.threads END formulas.
	 */
	private void checkCandidates() {
		LinkedList<List<Atom>> candidateAtoms = new LinkedList<List<Atom>>();
		Queue<Formula> naivePart = new LinkedList<Formula>();
		candidates: while (true) {
			this.naiveCandidates.drainTo(naivePart);
			formula: while (true) {
				Formula f = naivePart.poll();
				if (f == null) {
					break;
				} else if (f == END) {
					break candidates;
				}
				List<Atom> atoms = new ArrayList<Atom>(f.getAtoms());
				Collections.sort(atoms);
				final int size = atoms.size();
				atoms: for (List<Atom> g : candidateAtoms) {
					for (int i = 0; i < size; i++) {
						if (atoms.get(i) != g.get(i)) {
							continue atoms;
						}
					}
					continue formula;
				}
				candidateAtoms.add(atoms);
				this.candidates.offer(f);
			}
		}
		for (int i = 0; i < this.threads; i++) {
			this.candidates.offer(END);
		}
	}
	
	private class NaiveCandidates implements Runnable {
		
		private final Set<Atom> atoms;
		private final Queue<Formula> seeds;
		private final Queue<Formula> naiveCandidates;
		private final CountDownLatch done;
		
		public NaiveCandidates(Set<Atom> atoms, Queue<Formula> seeds, Queue<Formula> naiveCandidates, CountDownLatch done) {
			this.atoms = atoms;
			this.seeds = seeds;
			this.naiveCandidates = naiveCandidates;
			this.done = done;
		}
		
		private List<Formula> findCandidate(Formula f) {
			List<Formula> formulas = new LinkedList<Formula>();
			
			// list all atoms that may extend f
			Set<Variable> v = f.getVariables();
			List<Atom> possibleAtoms = EqualsFactory.get().getEquals(v);
			for (Atom a : this.atoms) {
				for (Term t : a.terms) {
					if (v.contains(t)) {
						possibleAtoms.add(a);
						break;
					}
				}
			}
			
			// remove duplicates atoms
			ListIterator<Atom> it = possibleAtoms.listIterator();
			List<Atom> fAtoms = f.getAtoms();
			while (it.hasNext()) {
				Atom a = it.next();
				for (Atom b : fAtoms) {
					if (a == b) {
						it.remove();
					}
				}
			}
			
			// generate formulas
			it = possibleAtoms.listIterator();
			while (it.hasNext()) {
				formulas.add(Disjunction.operator.getFormula(f, it.next()));
			}
			return formulas;
		}

		@Override
		public void run() {
			try {
				while(true) {
					Formula f = this.seeds.poll();
					if (f == null) {
						return;
					}
					for (Formula g : this.findCandidate(f)) {
						this.naiveCandidates.offer(g);
					}
				}
			} finally {
				this.done.countDown();
			}
		}
		
	}

}
