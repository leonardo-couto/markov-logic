package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import parse.ParseDataSet;
import parse.ParseDomain;
import GSIMN.GSIMN;
import fol.Predicate;

public class Main {
	
	private Settings settings;
	private ParseDomain domain;
	private ParseDataSet dataSet;
	
	public Main(Settings settings) {
		super();
		this.settings = settings;
		this.domain = new ParseDomain(settings.mln);
	}
	
	public void run() {
		System.out.println("parsing...");
		parse();
		System.out.println("setting Closed World...");
		setCW();
		System.out.println("generating tnodes...");
//		Set<Atom> tnodes = FormulaGenerator.getTNodes(domain.getPredicates(), settings.maxVar);
		System.out.println("running GSIMN...");
//		GSIMN<Atom> gs = new GSIMN<Atom>(tnodes, settings.itest, settings.alpha); // TODO: USAR ESSE!!
		GSIMN<Predicate> gs = new GSIMN<Predicate>(domain.getPredicates(), settings.itest, settings.alpha);
		//GSIMN<Predicate> gs = new GSIMN<Predicate>(domain.getPredicates(), new DefaultTest<Predicate>(settings.alpha), settings.alpha);
		System.out.println(gs.run());
	}
	
	private void parse() {
		try {
			domain.parse();
			for (File db : settings.db) {
				dataSet = new ParseDataSet(db, domain.getPredicates());
				dataSet.parse();
			}
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}
	
	private void setCW() {
		// TODO: Fazer baseado no settings closed world
		for (Predicate p : domain.getPredicates()) {
			p.setClosedWorld(true);
		}
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		List<File> db = new ArrayList<File>();
		db.add(new File("/home/leonardo/opt/alchemy/datasets/imdb/imdb.1.db"));
		//db.add(new File("/home/leonardo/opt/alchemy/datasets/imdb/imdb.2.db"));
		//db.add(new File("/home/leonardo/opt/alchemy/datasets/imdb/imdb.3.db"));
		//db.add(new File("/home/leonardo/opt/alchemy/datasets/imdb/imdb.4.db"));
		//db.add(new File("/home/leonardo/opt/alchemy/datasets/imdb/imdb.5.db"));		
		Settings settings = new Settings(new File("/home/leonardo/opt/alchemy/datasets/imdb/empty.mln"), db , null);
		//settings.itest = new MockIndependenceTest<Predicate>();
		Main m = new Main(settings);
		m.run();
				

	}

}
