package cs131.pa1.filter.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import cs131.pa1.filter.Filter;


public abstract class ConcurrentFilter extends Filter implements Runnable{
	
	protected BlockingQueue<String> input;
	protected BlockingQueue<String> output;
	protected boolean terminated = false; //a signal to tell if this thread is finished
	
	@Override
	public void setPrevFilter(Filter prevFilter) {
		prevFilter.setNextFilter(this);
	}
	
	@Override
	public void setNextFilter(Filter nextFilter) {
		if (nextFilter instanceof ConcurrentFilter){
			ConcurrentFilter sequentialNext = (ConcurrentFilter) nextFilter;
			this.next = sequentialNext;
			sequentialNext.prev = this;
			if (this.output == null){
				this.output = new LinkedBlockingQueue<String>();
			}
			sequentialNext.input = this.output;
		} else {
			throw new RuntimeException("Should not attempt to link dissimilar filter types.");
		}
	}
	
	public Filter getNext() {
		return next;
	}
	
	@Override
	//if this thread is not terminated, keep processing
	public void run() {
		while (!isDone()) {
			try {process();} 
			catch (InterruptedException e) {e.printStackTrace();}
		}
	}
	
	//process input; if the previous filter is done, process remaining input
	//and tell other threads I'm terminated, because no more input to process
	public void process() throws InterruptedException{
		processInput();
		if (prev.isDone()) {
			processInput();
			terminated = true;
		}
	}
	
	//standard take and put operations of LinkedBlockingQueue
	public void processInput() throws InterruptedException {
		while (!input.isEmpty()){
			String line = input.take();
			String processedLine = processLine(line);
			if (processedLine != null){
			    output.put(processedLine);
			}
		}
	}
	
	@Override
	public boolean isDone() {
		return terminated;
	}
	
	protected abstract String processLine(String line);
	
}
