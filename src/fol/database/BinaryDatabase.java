package fol.database;

import java.util.HashMap;

import fol.Atom;

public class BinaryDatabase implements BinaryDB {
	
	private final HashMap<Atom, Boolean> db;
	
	public BinaryDatabase() {
		this.db = new HashMap<Atom, Boolean>();
	}

	@Override
	public boolean valueOf(Atom key) {
		if (Atom.TRUE == key) return true;
		Boolean value = this.db.get(key);
		return value != null && value.booleanValue();
	}

	@Override
	public boolean flip(Atom key) {
		if (Atom.TRUE == key) return true;
		Boolean value = this.db.get(key);
		if (value == null || !value.booleanValue()) { 
			this.db.put(key, Boolean.TRUE);
			return true;
		} else if (value != null) {
			this.db.put(key, Boolean.FALSE);
		}
		return false;
	}

	@Override
	public void set(Atom key, boolean value) {
		if (Atom.TRUE == key) return;
		Boolean current = this.db.get(key);
		if (current == null) {
			if (value) {
				this.db.put(key, Boolean.TRUE);
			}
		} else if (value != current.booleanValue()) {
			this.db.put(key, Boolean.valueOf(value));
		}
	}

	@Override
	public BinaryDB getLocalCopy() {
		return new BinaryLocalDB(this);
	}
	
}
