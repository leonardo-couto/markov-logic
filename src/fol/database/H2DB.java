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
import fol.Predicate;

public class H2DB implements Database {
	
	private final static String URL = "jdbc:h2:mem:";
	
	private final Connection conn;
	private final Map<Predicate, PreparedStatement> insertStatement;
	private final Map<Predicate, PreparedStatement> updateStatement;
	private final Map<Predicate, PreparedStatement> selectStatement;

	
	public H2DB() {
		try {
			Class.forName("org.h2.Driver");
			Connection conn = DriverManager.getConnection(URL);
			this.conn = conn;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		this.insertStatement = new HashMap<Predicate, PreparedStatement>();
		this.updateStatement = new HashMap<Predicate, PreparedStatement>();
		this.selectStatement = new HashMap<Predicate, PreparedStatement>();
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
			this.insertStatement.put(p, this.conn.prepareStatement(insert));
			
			// update value statement
			String update = String.format("UPDATE %s SET value=? WHERE %s", name, columnValueStr);
			this.updateStatement.put(p, this.conn.prepareStatement(update));
			
			// select value statement
			String select = String.format("SELECT value FROM %s WHERE %s", name, columnValueStr);
			this.selectStatement.put(p, this.conn.prepareStatement(select));

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
		PreparedStatement st = this.selectStatement.get(a.predicate);
		if (st == null) {
			this.prepareStatements(a.predicate);
			st = this.selectStatement.get(a.predicate);
		}
		ResultSet rs = null;
		try {
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
				st = this.updateStatement.get(a.predicate);
				st.setBoolean(1, value);
				for (int i = 0; i < a.terms.length; i++) {
					st.setString(i+2, a.terms[i].toString());
				}
			} else {
				st = this.insertStatement.get(a.predicate);
				for (int i = 0; i < a.terms.length; i++) {
					st.setString(i+1, a.terms[i].toString());
				}
				st.setBoolean(a.terms.length +1, value);
			}
			st.execute();
		} catch (SQLException e) {
			throw new RuntimeException(e);
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
	

}
