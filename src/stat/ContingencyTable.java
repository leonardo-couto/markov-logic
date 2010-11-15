package stat;

import java.text.DecimalFormat;
import java.util.Arrays;

/**
 * <strong>Contingency table preconditions</strong>: <ul>
 * <li>Observed counts must be non-negative.</li>
 * <li>Observed counts for a specific sample must not all be 0.</li>
 * <li>The arrays length must be at least 2.</li>
 * 
 * If any of the preconditions are not met, an
 * <code>IllegalArgumentException</code> is thrown.</p>
 */
public class ContingencyTable { // TODO: EXTENDER Array2DRowRealMatrix
	
	// Checa a tabela
	// cria uma expected table
	// faz o teste para ver se serve para Pearson
	// guarda rowSum e colSum
	// guarda a tabela como double
	
	public final double[][] table;
	private double[] rowSum, colSum;
	private double sum;
	public final int m, n;
	private boolean pearson;
	private boolean fisher;
	
	public ContingencyTable(int[][] table) {
		this.table = init(table);
		m = this.table.length;
		n = this.table[0].length;
		this.fisher = ((m == 2) && (n == 2));
	}
	
	public ContingencyTable(double[][] table) {
		initd(table);
		this.table = table;
		m = this.table.length;
		n = this.table[0].length;
		this.fisher = ((m == 2) && (n == 2));
	}
	
	protected ContingencyTable(double[][] table, ContingencyTable ct) {
		this.table = table;
		this.rowSum = ct.rowSum;
		this.colSum = ct.colSum;
		this.sum = ct.sum;
		m = ct.m;
		n = ct.n;
	}
	
	private double[][] init(int[][] table) {
		if (table == null) {
			throw new IllegalArgumentException("Table is null.");
		}
		if (table.length < 2) {
			throw new IllegalArgumentException("Must have at least 2 rows.");
		}
		if (table[0] == null) {
			throw new IllegalArgumentException("First row is null.");
		}
		int colDimension = table[0].length;
		if (colDimension < 2) {
			throw new IllegalArgumentException("Must have at least 2 columns.");
		}
		boolean pearson = true;
		int sum = 0;
		double[] colSum = new double[colDimension];
		double[] rowSum = new double[table.length];
		double[][] dtable = new double[table.length][colDimension];
		Arrays.fill(colSum, 0);
		Arrays.fill(rowSum, 0);
		for (int i = 0; i < table.length; i++) {
			int[] row = table[i];
			if (row == null) {
				throw new IllegalArgumentException(i + "-th row is null.");
			}
			if (row.length != colDimension) {
				throw new IllegalArgumentException("Matrix is not retangular.");
			}
			for (int j = 0; j < colDimension; j++) {
				if (row[j] < 0) {
					throw new IllegalArgumentException("All matrix entries must be nonnegative and finite.");
				}
				if (row[j] < 6) {
					pearson = false;
				}
				rowSum[i] = rowSum[i] + row[j];
				colSum[j] = colSum[j] + row[j];
				dtable[i][j] = row[j];
				sum = sum + row[j];
			}
		}
		this.rowSum = rowSum;
		this.colSum = colSum;
		this.sum = sum;
		this.pearson = pearson;
		return dtable;	
	}

	private void initd(double[][] table) {
		if (table == null) {
			throw new IllegalArgumentException("Table is null.");
		}
		if (table.length < 2) {
			throw new IllegalArgumentException("Must have at least 2 rows.");
		}
		if (table[0] == null) {
			throw new IllegalArgumentException("First row is null.");
		}
		int colDimension = table[0].length;
		if (colDimension < 2) {
			throw new IllegalArgumentException("Must have at least 2 columns.");
		}
		boolean pearson = true;
		double sum = 0;
		double[] colSum = new double[colDimension];
		double[] rowSum = new double[table.length];
		Arrays.fill(colSum, 0);
		Arrays.fill(rowSum, 0);
		for (int i = 0; i < table.length; i++) {
			double[] row = table[i];
			if (row == null) {
				throw new IllegalArgumentException(i + "-th row is null.");
			}
			if (row.length != colDimension) {
				throw new IllegalArgumentException("Matrix is not retangular.");
			}
			for (int j = 0; j < colDimension; j++) {
				if (Double.compare(row[j], 0.0) < 0) {
					throw new IllegalArgumentException("All matrix entries must be nonnegative and finite.");
				}
				if (Double.compare(row[j], 5.0) < 1) {
					pearson = false;
				}
				rowSum[i] = rowSum[i] + row[j];
				colSum[j] = colSum[j] + row[j];
				sum = sum + row[j];
			}
		}
		this.rowSum = rowSum;
		this.colSum = colSum;
		this.sum = sum;
		this.pearson = pearson;
	}
	
	public ContingencyTable expected() {
		double[][] expected = new double[table.length][table[0].length];
		for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++) {
				expected[i][j] = ((rowSum[i]*colSum[j])/sum);
			}
		}
		return new ContingencyTable(expected, this);
	}
	
	/**
	 * Check if conditions to apply the Pearson Chi Squared independence
	 * test are met.
	 * The recommendation is that every cell as more than five counts.
	 * TODO: E NUMERO DE ELEMENTOS MENOR QUE X, CASO CONTRARIO APLICAR PEARSON COM
	 * YATTES CORRECAO!!
	 * @return true if conditions to apply pearson test are met.
	 */
	public boolean pearson() {
		return this.pearson;
	}
	
	/**
	 * Check if conditions to apply the Fisher exact test are met.
	 * We only apply this test to 2x2 matrices.
	 * @return true if the contingency table is 2x2.
	 */
	public boolean fisher() {
		return this.fisher;
	}
	
	/**
	 * @return the rowSum
	 */
	public double[] getRowSum() {
		return Arrays.copyOf(rowSum, rowSum.length);
	}

	/**
	 * @return the colSum
	 */
	public double[] getColSum() {
		return Arrays.copyOf(colSum, colSum.length);
	}
	
	public double getSum() {
		return sum;
	}
	
	@Override
	public String toString() {
		String s = "";
		DecimalFormat df = new DecimalFormat("#.##");
		for (double[] row : table) {
			s = s + "[ ";
			for (double e : row) {
				s = s + df.format(e) + " ";
			}
			s = s + "]\n";
		}
		return s.substring(0, s.length()-1);
	}


}
