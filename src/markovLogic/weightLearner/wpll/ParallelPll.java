package markovLogic.weightLearner.wpll;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import util.ProducersWatcher;

public class ParallelPll {
	
	public static final List<Count> END = Collections.emptyList();
	
	private final Collection<List<Count>> counts;
	private final double[] weights;
	
	public ParallelPll(Collection<List<Count>> counts, double[] weights) {
		this.counts = counts;
		this.weights = weights;
	}
	
	public BlockingQueue<DataPll> run() {
		BlockingQueue<List<Count>> countsQueue = new LinkedBlockingQueue<List<Count>>(this.counts);
		BlockingQueue<DataPll> data = new LinkedBlockingQueue<DataPll>();
		countsQueue.add(END);
		int threads = Runtime.getRuntime().availableProcessors();
		CountDownLatch done = new CountDownLatch(threads);
		(new Thread(new ProducersWatcher<DataPll>(data, done, DataPll.END))).start();
		for (int i = 0; i < threads; i++) {
			Runnable parallelPll = new Runner(countsQueue, data, done, this.weights);
			Thread t = new Thread(parallelPll);
			t.start();
		}
		return data;
	}

	private static class Runner implements Runnable {
		
		private final BlockingQueue<List<Count>> countsQueue;
		private final BlockingQueue<DataPll> results;
		private final CountDownLatch done;
		private final LogConditionalProbability prob;
		
		public Runner(BlockingQueue<List<Count>> countsQueue, 
				BlockingQueue<DataPll> results, 
				CountDownLatch done,
				double[] weights) {
			this.countsQueue = countsQueue;
			this.results = results;
			this.done = done;
			this.prob = new LogConditionalProbability(weights);
		}
		

		@Override
		public void run() {
			try {
				while(true) {
					List<Count> counts = this.countsQueue.take();
					if (counts == END) {
						this.countsQueue.put(END);
						return;
					}
					DataPll pll = this.prob.evaluate(counts);
					results.put(pll);
				}				
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				this.done.countDown();
			}			
		}
		
	}
	

}
