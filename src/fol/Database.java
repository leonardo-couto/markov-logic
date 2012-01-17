package fol;

public interface Database {
	
	public boolean valueOf(Atom a);
//	public boolean isVariable(Atom a);
	public boolean flip(Atom a);
	public void set(Atom a, boolean value);
//	public void setVariable(Atom a, boolean initialValue);
	
}
