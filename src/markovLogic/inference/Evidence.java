package markovLogic.inference;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import markovLogic.weightLearner.wpll.Count;

import fol.Atom;
import fol.Formula;
import fol.Predicate;
import fol.database.Database;
import fol.database.SimpleDB;;

public class Evidence implements Database {

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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<Atom> groundingIterator(Atom filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<Atom> groundingIterator(Atom filter, boolean value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int groundingCount(Atom filter) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int groundingCount(Atom filter, boolean value) {
		// TODO Auto-generated method stub
		return 0;
	}
	
}
