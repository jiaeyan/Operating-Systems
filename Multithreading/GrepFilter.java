package cs131.pa1.filter.concurrent;

import cs131.pa1.filter.Message;

public class GrepFilter extends ConcurrentFilter {
	private String toFind;
	
	public GrepFilter(String line) throws Exception {
		super();
		String[] param = line.split(" ");
		if(param.length > 1) {
			toFind = param[1];
		} else {
			System.out.printf(Message.REQUIRES_PARAMETER.toString(), line);
			throw new Exception();
		}
	}
	
	public String processLine(String line) {
		if(line.contains(toFind)) {
			return line;
		} else {
			return null;
		}
	}
}
