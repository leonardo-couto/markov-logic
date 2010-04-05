package main;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fol.Atom;
import fol.Predicate;

import stat.DefaultTest;
import stat.IndependenceTest;

public class Settings {
	
	// Independence test parameter
	public double alpha;
	
	// Parallel Shortest First parameters
	public int threads;
	public int fpt;
	public int maxAtoms;
	
	// Parse parameters
	public File mln;
	public List<File> db;
	public File out;
	
	// Predicate parameter
	public List<String> closedWorld;
	
	// GSIMN Parameter
	public IndependenceTest<Atom> itest;
	
	// TNODES max variables per domain.
	public int maxVar;
	
	public Settings(File mln, File db, File out) {
		this.mln = mln;
		this.db = new ArrayList<File>();
		this.db.add(db);
		this.out = out;
		defaultValues();
	}
	
	public Settings(File mln, List<File> db, File out) {
		this.mln = mln;
		this.db = db;
		this.out = out;
		defaultValues();
	}
	
	public void defaultValues() {
		alpha = 0.05d;
		threads = 2;
		fpt = 30;
		closedWorld = Collections.emptyList();
		itest = new DefaultTest<Atom>(alpha);
		maxAtoms = 6;
		maxVar = 2;
	}
	

}
