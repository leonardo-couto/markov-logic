package fol.database;

import java.util.HashMap;

import fol.Atom;

public class RealDatabase implements RealDB {
	
	private final HashMap<Atom, Double> db;
	
	public RealDatabase() {
		this.db = new HashMap<Atom, Double>();
	}

	@Override
	public double valueOf(Atom key) {
		if (Atom.TRUE == key) return 1.0d;
		Double value = this.db.get(key);
		return value == null ? 0.0d : value.doubleValue();
	}

	@Override
	public void set(Atom key, double value) {
		if (Atom.TRUE == key) return;
		Double current = this.db.get(key);
		
		Double v;
		if (value == 0.0d) v = RealDB.FALSE;
		else if (value == 1.0d) v = RealDB.TRUE;
		else v = Double.valueOf(value);
		
		if (current == null) {
			if (v != RealDB.FALSE) {
				this.db.put(key, v);
			}
		} else if (v != current) {
			this.db.put(key, v);
		}
	}

	@Override
	public RealDB getLocalCopy() {
		return new RealLocalDB(this);
	}
	
}
