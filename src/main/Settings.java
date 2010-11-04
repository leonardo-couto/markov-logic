package main;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.apache.commons.math.distribution.TDistribution;
import org.apache.commons.math.distribution.TDistributionImpl;

import stat.IndependenceTest;
import fol.Predicate;

public class Settings {
	// TODO: FAZER ESSES VALORES VIREM DE UM XML!!
	public static final double[] confidenceLevel = { .95, .95, .99};
	public static final double[] precision = {.05, .01, .01};
	private static final int tLimit = 100;
	public static final double[][] tStudent = new double[3][];
	public static final double[] normal = new double[3];
	private static final double[] tStudent95 = new double[tLimit];
	private static final double[] tStudent99 = new double[tLimit];

	// Formula.formulaCount:
	public static final int formulaCountMaxSamples = 1000;

	/**
	 * Calcula os tlimit primeiros elementos da distribuicao t de student
	 * para i=1 .. tlimit graus de liberdade. (i -> infinito tende a normal)
	 * com .95 e .99 de nivel de confianca.
	 * Por causa da simetria, se nivel de confianca = p, calcula-se
	 * inverseCumulativeProbability((p+1)/2)
	 * 
	 * TODO: fazer factorys, que pegam a configuracao de um XML, talvez usando
	 * spring. Setar dois ou tres niveis diferentes de nivel de confianca
	 * 99% e 95% por exemplo. Calcular essa array no factory e passar para a classe.
	 * Note que da maneira que estah, o confidenceLevel do constructor estah 
	 * sendo ignorado.
	 *  
	 */
	static {
		for (int i = 0; i < tLimit; i++) {
			TDistribution tStudentDist = new TDistributionImpl(i+1);
			try {
				tStudent95[i] = tStudentDist.inverseCumulativeProbability(0.975);
				tStudent99[i] = tStudentDist.inverseCumulativeProbability(0.995);
			} catch (MathException e) {
				throw new RuntimeException(e);
			}
		}
		tStudent[0] = tStudent95;
		tStudent[1] = tStudent95;
		tStudent[2] = tStudent99;
		NormalDistribution normalDist = new NormalDistributionImpl();
		try {
			normal[0] = normalDist.inverseCumulativeProbability(0.975);
			normal[1] = normalDist.inverseCumulativeProbability(0.975);
			normal[2] = normalDist.inverseCumulativeProbability(0.995);
		} catch (MathException e) {
			throw new RuntimeException(e);
		}
	}


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
	//	public IndependenceTest<Atom> itest; // TODO: usar esse!
	public IndependenceTest<Predicate> itest;

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
		//		itest = new DefaultTest<Atom>(alpha); TODO: usar esse!
		//itest = new DefaultTest<Predicate>(alpha);
		maxAtoms = 6;
		maxVar = 2;
	}


}
