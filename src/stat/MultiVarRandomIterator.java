package stat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class MultiVarRandomIterator<T> implements Iterator<List<T>> {
	
	private final List<List<T>> domains;
	private final List<T> availableElements;
	private final Map<T, TreeNode> children;
	private static final Random random = new Random();
	private final boolean leaf;
	private List<T> nextElement;
	private int maxSamples = -1;
	private boolean hasSampleLimit = false;
	private int i = 0;
	
	public MultiVarRandomIterator(List<List<T>> domains) {
		if(domains == null || domains.isEmpty()) {
			throw new IllegalArgumentException("No Domains.");
		}
		for (List<T> domain : domains) {
			if (domain.isEmpty()) {
				throw new IllegalArgumentException("Domains has an empty domain.");
			}
		}
		this.domains = domains;
		this.availableElements = new LinkedList<T>(domains.get(0));
		this.children = new HashMap<T, TreeNode>((int) Math.ceil(this.availableElements.size()*1.4));
		if (domains.size() == 1) {
			this.leaf = true;
		} else {
			this.leaf = false;
		}
		this.nextElement = this.makeNext();
	}

	private List<T> makeNext() {
		if (availableElements.isEmpty()) {
			return null;
		}
		int i = random.nextInt(availableElements.size());
		T randomChild = this.availableElements.get(i);

		if (leaf) {
			availableElements.remove(i);
			return Collections.singletonList(randomChild);
		}
		
		TreeNode tn;
		if (this.children.containsKey(randomChild)) {
			tn = this.children.get(randomChild);
		} else {
			tn = new TreeNode(1, randomChild);
			this.children.put(randomChild, tn);
		}
		
		List<T> path = children.get(randomChild).getRandomPath();
		if (path == null) {
			this.availableElements.remove(i);
			return this.makeNext();
		}
		return path;
	}
	
	@Override
	public boolean hasNext() {
		if (hasSampleLimit) {
			if (i > maxSamples) {
				return false;
			}
		}
		return (nextElement != null);
	}

	@Override
	public List<T> next() {
		i++;
		List<T> out = nextElement;
		nextElement = makeNext();
		return out;
	}

	@Override
	public void remove() {
		// do nothing		
	}
	
	private class TreeNode {

		private final int depth;
		private final T element;
		private final List<T> availableElements;
		private final Map<T, TreeNode> children;
		private final boolean leaf;

		public TreeNode(int depth, T element) {
			this.depth = depth;
			this.element = element;
			this.availableElements = new LinkedList<T>(domains.get(depth));
			this.children = new HashMap<T, TreeNode>((int) Math.ceil(this.availableElements.size()*1.4));
			if (depth == domains.size()-1) {
				this.leaf = true;
			} else {
				this.leaf = false;
			}
		}

		public List<T> getRandomPath() {
			if (availableElements.isEmpty()) {
				return null;
			}
			int i = random.nextInt(availableElements.size());
			T randomChild = this.availableElements.get(i);

			if (leaf) {
				this.availableElements.remove(i);
				List<T> out = new LinkedList<T>();
				out.add(this.element);
				out.add(randomChild);
				return out;
			}
			
			TreeNode childTN;
			if (children.containsKey(randomChild)) {
				childTN = children.get(randomChild);
			} else {
				childTN = createChildTree(randomChild);
				this.children.put(randomChild, childTN);
			}

			List<T> path = childTN.getRandomPath();
			if (path == null) {
				this.availableElements.remove(i);
				return this.getRandomPath();
			}

			path.add(0, this.element);
			return path;
		}
		
		private TreeNode createChildTree(T child) {
			TreeNode tn = new TreeNode(this.depth +1, child);
			return tn;
		}
		
	}
	
	public MultiVarRandomIterator<T> setMaxSamples(int n) {
		this.maxSamples = n;
		this.hasSampleLimit = true;
		return this;
	}
	
	public int getMaxSamples() {
		return this.maxSamples; 
	}

}
