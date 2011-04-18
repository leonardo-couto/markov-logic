package formulaLearner;

import java.util.Queue;
import java.util.concurrent.CountDownLatch;

/**
 * Puts a end instance in the <code>queue</code> when all producer 
 * threads linked to the <code>done</code> CountDownLatch are done.
 */
public class ProducersWatcher<T> implements Runnable {

	private final Queue<T> queue;
	private final CountDownLatch done;
	private final T end;
	
	public ProducersWatcher(Queue<T> queue, CountDownLatch done, T end) {
		this.queue = queue;
		this.done = done;
		this.end = end;
	}

	@Override
	public void run() {
		try {
			this.done.await();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			this.queue.offer(this.end);
		}
	}

}
