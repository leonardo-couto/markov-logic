package fol.database;

import java.util.Iterator;

import fol.Atom;

public class WriteOnlyDB implements Database {

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean valueOf(Atom a) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean flip(Atom a) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void set(Atom a, boolean value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Database getLocalCopy() {
		throw new UnsupportedOperationException("WriteOnlyDB does not have a local copy");
	}

	@Override
	public Iterator<Atom> groundingIterator(Atom a) {
		throw new UnsupportedOperationException("Cannot Iterate a WriteOnlyDB");
	}

}
