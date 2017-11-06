package cs131.pa1.filter.concurrent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import cs131.pa1.filter.Filter;
import cs131.pa1.filter.Message;

public class RedirectFilter extends ConcurrentFilter {
	private FileWriter fw;
	
	public RedirectFilter(String line) throws Exception {
		super();
		String[] param = line.split(">");
		if(param.length > 1) {
			if(param[1].trim().equals("")) {
				System.out.printf(Message.REQUIRES_PARAMETER.toString(), line.trim());
				throw new Exception();
			}
			try {
				fw = new FileWriter(new File(ConcurrentREPL.currentWorkingDirectory + Filter.FILE_SEPARATOR + param[1].trim()));
			} catch (IOException e) {
				System.out.printf(Message.FILE_NOT_FOUND.toString(), line);	//shouldn't really happen but just in case
				throw new Exception();
			}
		} else {
			System.out.printf(Message.REQUIRES_INPUT.toString(), line);
			throw new Exception();
		}
	}
	
	//if the previous filter is done and no more input to process,
	//close the I/O flow
	public void process() throws InterruptedException {
		processInput();
		if (prev.isDone()) {
			processInput();
			try {
				fw.flush();
				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			terminated = true;
		}
	}
	
	@Override
	public void processInput() throws InterruptedException {
		while(!input.isEmpty()) {
			processLine(input.take());
		}
	}
	
	public String processLine(String line) {
		try {
			fw.append(line + "\n");
		} catch (IOException e) {
			System.out.printf(Message.FILE_NOT_FOUND.toString(), line);
		}
		return null;
	}
}
