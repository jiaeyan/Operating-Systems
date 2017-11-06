package cs131.pa1.filter.concurrent;

public class WcFilter extends ConcurrentFilter {
	private int linecount;
	private int wordcount;
	private int charcount;
	
	public WcFilter() {
		super();
	}
	
	//process one line a time to accumulate counts
	public void process() throws InterruptedException {
		processInput();
		if (prev.isDone()) {
			processInput();
			output.put(linecount + " " + wordcount + " " + charcount);
			terminated = true;
		}
	}
	
	@Override
	//no need to deal with output for now
	public void processInput() throws InterruptedException {
		while(!input.isEmpty()) {
			processLine(input.take());
		}
	}
	
	public String processLine(String line) {
		linecount++;
		String[] wct = line.split(" ");
		wordcount += wct.length;
		String[] cct = line.split("|");
		charcount += cct.length;
		return null;
	}

}
