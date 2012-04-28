package markovLogic.parse;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Parse {
	
	
	
	public static <T> Map<String, T> toMap(Collection<T> c) {
		Map<String, T> map = new HashMap<String, T>();
		for (T t : c) {
			map.put(t.toString(), t);
		}
		return map;
	}


}
