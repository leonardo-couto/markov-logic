package fol.database;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import fol.Atom;
import fol.Predicate;

public class WriteOnlyDB {
	
	private final Set<CompositeKey> db;
	private final Map<Predicate, List<PriorityQueue<CompositeKey>>> indexedValues;
	
	public WriteOnlyDB(Set<Predicate> predicates) {
		this.db = new HashSet<CompositeKey>();
		this.indexedValues = new HashMap<Predicate, List<PriorityQueue<CompositeKey>>>();
		for (Predicate p : predicates) {
			int domains = p.getDomains().size();
			List<PriorityQueue<CompositeKey>> indexes = new ArrayList<PriorityQueue<CompositeKey>>(domains);
			this.indexedValues.put(p, indexes);
			for (int i = 0; i < domains; i++) {
				PriorityQueue<CompositeKey> pqueue;
				Comparator<CompositeKey> comparator = CompositeKey.getComparator(i);
				pqueue = new PriorityQueue<CompositeKey>(100, comparator);
				indexes.add(pqueue);
			}
		}
	}
	
	private void addIndexed(Predicate p, CompositeKey key) {
		List<PriorityQueue<CompositeKey>> indexes = this.indexedValues.get(p);
		for (PriorityQueue<CompositeKey> index : indexes) {
			index.offer(key);
		}
	}
	
	private void removeIndexed(Predicate p, CompositeKey key) {
		List<PriorityQueue<CompositeKey>> indexes = this.indexedValues.get(p);
		for (PriorityQueue<CompositeKey> index : indexes) {
			index.remove(key);
		}
	}

	public void set(Atom a, boolean value) {
		CompositeKey key = new CompositeKey(a.predicate, a.terms, value);
		boolean storedKey = this.db.contains(key);
		if (storedKey != value) {
			if (value) {
				this.db.add(key);
				this.addIndexed(a.predicate, key);				
			} else {
				this.db.remove(key);
				this.removeIndexed(a.predicate, key);
			}
		}
	}
	
	public ReadOnlyDB getReadDatabase() {
		Map<Predicate, CompositeKey[][]> indexedValues = new HashMap<Predicate, CompositeKey[][]>();
		for (Predicate p : this.indexedValues.keySet()) {
			List<PriorityQueue<CompositeKey>> list = this.indexedValues.get(p);
			CompositeKey[][] keys = new CompositeKey[list.size()][];
			for (int i = 0; i < list.size(); i++) {
				PriorityQueue<CompositeKey> queue = list.get(i);
				keys[i] = new CompositeKey[queue.size()];
				for (int j = 0; j < queue.size(); j++) {
					keys[i][j] = queue.poll();
				}
			}
			indexedValues.put(p, keys);
		}
		return new ReadOnlyDB(new HashSet<CompositeKey>(this.db), indexedValues);
	}


}
