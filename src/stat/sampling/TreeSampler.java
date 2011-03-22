package stat.sampling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Creates and maintain a Tree of chosen elements
 * This is efficient for larger values of n, as long as the 
 * number of sampled elements is much smaller then n
 * sampled << n.
 */
public class TreeSampler<T> extends AbstractSampler<T> {
	
	private final List<Random> random;
	private final boolean leaf;
	
	public TreeSampler(List<? extends Collection<T>> domains) {
		this(domains, Integer.MAX_VALUE);
	}
	
	public TreeSampler(List<? extends Collection<T>> domains, int maxSamples) {
		super(domains, maxSamples);
		this.leaf = (this.isEmpty() || this.domains.size() == 1);
		this.random = new ArrayList<Random>(domains.size());
		for (int i = 0; i < domains.size(); i++) { random.add(new Random()); }
	}
	
	@Override
	public Iterator<List<T>> iterator() {
		if (this.isEmpty()) {
			return this.emptyIterator();
		}
		
		final List<T> availableElements = new LinkedList<T>(this.domains.get(0));
		final Map<T, TreeNode> children = new HashMap<T, TreeNode>((int) Math.ceil(availableElements.size()*1.4));
		
		return new Iterator<List<T>>() {
			
			int i = 0;
			List<T> nextElement = makeNext();
			
			@Override
			public boolean hasNext() {
				if (i > maxSamples) {
					return false;
				}
				return (this.nextElement != null);
			}

			@Override
			public List<T> next() {
				i++;
				List<T> out = this.nextElement;
				this.nextElement = this.makeNext();
				return out;
			}

			@Override
			public void remove() {
				// do nothing		
			}
			
			private List<T> makeNext() {
				if (availableElements.isEmpty()) {
					return null;
				}
				int i = random.get(0).nextInt(availableElements.size());
				T randomChild = availableElements.get(i);

				if (leaf) {
					availableElements.remove(i);
					return Collections.singletonList(randomChild);
				}
				
				TreeNode tn;
				if (children.containsKey(randomChild)) {
					tn = children.get(randomChild);
				} else {
					tn = new TreeNode(1, randomChild);
					children.put(randomChild, tn);
				}
				
				List<T> path = children.get(randomChild).getRandomPath();
				if (path == null) {
					availableElements.remove(i);
					return this.makeNext();
				}
				return path;
			}
			
		};
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
			int i = random.get(this.depth).nextInt(availableElements.size());
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


}
