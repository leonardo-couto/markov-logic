package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import markovLogic.parse.ParseDataSet;
import markovLogic.parse.ParseDomain;

public class Main {
	
	private Settings settings;
	private ParseDomain domain;
	private ParseDataSet dataSet;
	
	public Main(Settings settings) {
		super();
		this.settings = settings;
		this.domain = new ParseDomain();
	}
	
	public void run() {
		System.out.println("parsing...");
		parse(settings.mln);
//		System.out.println("generating tnodes...");
//		Set<Atom> atoms = new HashSet<Atom>(FormulaGenerator.unitClauses(domain.getPredicates()));
//		System.out.println("running GSIMN...");
//		IndependenceTest<Atom> iTest = new DefaultTest<Atom>(0.01, atoms);
//		GSIMN<Atom> gs = new GSIMN<Atom>(atoms, iTest);
//		System.out.println(gs.run());
//		System.out.println("runing busl...");
//		Busl busl = new Busl(this.domain.getPredicates(), this.dataSet.getDatabase());
//		System.out.println(busl.learn());
	}
	
	private void parse(File f) {
		try {
			dataSet = new ParseDataSet(domain.getPredicates());
			dataSet.parse(this.settings.db.toArray(new File[this.settings.db.size()]));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		List<File> db = new ArrayList<File>();
		db.add(new File("/home/leonardo/opt/alchemy/datasets/imdb/imdb.1.db"));
		db.add(new File("/home/leonardo/opt/alchemy/datasets/imdb/imdb.2.db"));
		db.add(new File("/home/leonardo/opt/alchemy/datasets/imdb/imdb.3.db"));
		db.add(new File("/home/leonardo/opt/alchemy/datasets/imdb/imdb.4.db"));
		db.add(new File("/home/leonardo/opt/alchemy/datasets/imdb/imdb.5.db"));		
		Settings settings = new Settings(new File("/home/leonardo/opt/alchemy/datasets/imdb/empty.mln"), db , null);
		//settings.itest = new MockIndependenceTest<Predicate>();
		Main m = new Main(settings);
		m.run();
	}

}
