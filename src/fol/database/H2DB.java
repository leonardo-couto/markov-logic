package fol.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import fol.Atom;
import fol.Constant;
import fol.Domain;
import fol.Predicate;

public class H2DB implements Database {
	
	private final static String URL = "jdbc:h2:mem:";
	
	private final Connection conn;
	private final Map<Predicate, String> insertStatement;
	private final Map<Predicate, String> updateStatement;
	private final Map<Predicate, String> selectStatement;

	
	public H2DB() {
		try {
			Class.forName("org.h2.Driver");
			Connection conn = DriverManager.getConnection(URL);
			this.conn = conn;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		this.insertStatement = new HashMap<Predicate, String>();
		this.updateStatement = new HashMap<Predicate, String>();
		this.selectStatement = new HashMap<Predicate, String>();
	}
	
	private void prepareStatements(Predicate p) {
		try {
			String name = p.toString();
			
			// create table
			StringBuilder columns = new StringBuilder();
			StringBuilder values = new StringBuilder();
			StringBuilder columnsValues = new StringBuilder();
			for (int i = 0; i < p.getDomains().size(); i++) {
				columns.append("d").append(i).append(" VARCHAR(12), ");
				columnsValues.append("d").append(i).append("=? AND ");
				values.append("?,");
			}
			values.append("?");
			String columnValueStr = columnsValues.substring(0, columnsValues.length()-5);
			String create = String.format("CREATE TABLE %s (%svalue BOOLEAN)", 
					name, columns.toString());
			this.conn.prepareStatement(create).execute();

			// insert statement
			String insert = String.format("INSERT INTO %s VALUES (%s)", 
					name, values.toString());
			this.insertStatement.put(p, insert);
			
			// update value statement
			String update = String.format("UPDATE %s SET value=? WHERE %s", name, columnValueStr);
			this.updateStatement.put(p, update);
			
			// select value statement
			String select = String.format("SELECT value FROM %s WHERE %s", name, columnValueStr);
			this.selectStatement.put(p, select);

		} catch (SQLException e) {
			e.printStackTrace();
			try {
				this.conn.close();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean valueOf(Atom a) {
		Boolean value = this._valueOf(a);
		return value != null && value.booleanValue();
	}
	
	private Boolean _valueOf(Atom a) {
		String statement = this.selectStatement.get(a.predicate);
		if (statement == null) {
			this.prepareStatements(a.predicate);
			statement = this.selectStatement.get(a.predicate);
		}
		PreparedStatement st = null;
		ResultSet rs = null;
		try {
			st = this.conn.prepareStatement(statement);
			for (int i = 0; i < a.terms.length; i++) {
				st.setString(i+1, a.terms[i].toString());
			}
			rs = st.executeQuery();
			if (rs.next()) {
				return rs.getBoolean(1);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (st != null) {
					st.close();
				}
				if (rs != null) {
					rs.close();
				}				
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public boolean flip(Atom a) {
		Boolean b = this._valueOf(a);
		boolean value = (b == null) || !b.booleanValue();
		this.set(a, value, b != null);
		return value;
	}
	
	private void set(Atom a, boolean value, boolean update) {
		PreparedStatement st = null;
		try {
			if (update) {
				String statement = this.updateStatement.get(a.predicate);
				st = this.conn.prepareStatement(statement);
				st.setBoolean(1, value);
				for (int i = 0; i < a.terms.length; i++) {
					st.setString(i+2, a.terms[i].toString());
				}
			} else {
				String statement = this.insertStatement.get(a.predicate);
				st = this.conn.prepareStatement(statement);
				for (int i = 0; i < a.terms.length; i++) {
					st.setString(i+1, a.terms[i].toString());
				}
				st.setBoolean(a.terms.length +1, value);
			}
			st.execute();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (st != null) {
					st.close();
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	@Override
	public void set(Atom a, boolean value) {
		Boolean b = this._valueOf(a);
		if (b == null) { // insert
			if (value) {
				this.set(a, value, false);
			}
		} else if (b.booleanValue() != value) { // update
			this.set(a, value, true);
		}
		
	}

	@Override
	public Database getLocalCopy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<Atom> groundingIterator(Atom a) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() {
		try {
			this.conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		Database db = new H2DB();
		
		Domain d0 = new Domain("d0");
		Domain d1 = new Domain("d1");
		Predicate p0 = new Predicate("p0", d0);
		Predicate p1 = new Predicate("p1", d0, d1);
		Predicate p2 = new Predicate("p2", d1);
		Constant d0_c0 = new Constant("c0", d0);
		Constant d0_c1 = new Constant("c1", d0);
		Constant d1_c0 = new Constant("c2", d1);
		Constant d1_c1 = new Constant("c3", d1);
		Atom a0 = new Atom(p0, d0_c0);
		Atom a1 = new Atom(p1, d0_c1, d1_c0);
		Atom a2 = new Atom(p2, d1_c1);
		
		db.set(a0, true);
		db.set(a1, false);
		
		System.out.println(db.valueOf(a0));
		System.out.println(db.valueOf(a1));
		System.out.println(db.valueOf(a2));
		
		db.flip(a0);
		db.flip(a1);
		db.flip(a2);
		
		System.out.println("**********");
		
		System.out.println(db.valueOf(a0));
		System.out.println(db.valueOf(a1));
		System.out.println(db.valueOf(a2));
		
		db.close();
	}
	

}
