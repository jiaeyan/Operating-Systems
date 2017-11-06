package cs131.pa1.filter.concurrent;

public class PrintFilter extends ConcurrentFilter{
	public PrintFilter() {
		super();
	}
	
	@Override
	public void processInput() throws InterruptedException {
		while(!input.isEmpty()) {
			processLine(input.take());
		}
	}
	
	public String processLine(String line) {
		System.out.println(line);
		return null;
	}
}
