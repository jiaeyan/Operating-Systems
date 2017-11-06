package cs131.pa1.filter.concurrent;

public class PwdFilter extends ConcurrentFilter{
	public PwdFilter() {
		super();
	}
	
	public void process() throws InterruptedException {
		output.put(processLine(""));
		terminated = true;
	}
	
	public String processLine(String line) {
		return ConcurrentREPL.currentWorkingDirectory;
	}

}
