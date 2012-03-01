/**
 * 
 */
package markovLogic.parse;

import java.util.Arrays;

import util.Util;

/**
 * @author leonardo
 *
 */
public class Parse {

	/**
	 * 
	 */
	public Parse() {
		// TODO Auto-generated constructor stub
	}
	
	// TODO: ARCHITECTURE!
	// Separates a String in the format "name0(name1,...,namen)"
	// in a array of names String.
	public static String[] predicateTokenizer(String line) {
		String strip = Util.strip(line);
		String[] tokens = strip.split("[(,)]");
		return tokens;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println(Arrays.toString(Parse.predicateTokenizer("	asdasd ( ghhh , asrwqe , oo , oppp ) 1.023\n")));
		System.out.println(Arrays.toString(Parse.predicateTokenizer("predicado(termo)")));
		System.out.println(Double.toString(Double.parseDouble("0.123456789012345678901")));
	}

}
