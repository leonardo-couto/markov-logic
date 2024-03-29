package markovLogic.weightLearner.wpll;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import fol.Atom;
import fol.Formula;

public class CountsGenerator {
	
	private static final Formula END = Atom.TRUE;

	private final CountCache cache;
	private final int samples;
	private final int threads;
	
	public CountsGenerator(CountCache cache, int samples, int threads) {
		this.cache = cache;
		this.samples = samples;
		this.threads = threads;
	}
	
	public void count(List<? extends Formula> formulas) {
		BlockingQueue<Formula> queue = new LinkedBlockingQueue<Formula>(formulas);
		queue.add(END);
		CountDownLatch done = new CountDownLatch(threads);
		for (int i = 0; i < threads; i++) {
			Runnable counter = new Runner(queue, done);
			Thread thread = new Thread(counter);
			thread.start();
		}
		try {
			done.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private class Runner implements Runnable {
		
		private final BlockingQueue<Formula> queue;
		private final CountDownLatch done;
		
		public Runner(BlockingQueue<Formula> queue, CountDownLatch done) {
			this.queue = queue;
			this.done = done;
		}
		
		@Override
		public void run() {
			try {
				while (true) {
					Formula formula;
					formula = this.queue.take();
					if (formula == END) {
						this.queue.put(formula);
						break;
					}
					cache.getCounts(formula, samples);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				this.done.countDown();
			}
		}
		
	}
	
}
