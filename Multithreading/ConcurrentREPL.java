package cs131.pa1.filter.concurrent;

import cs131.pa1.filter.Message;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ConcurrentREPL {

	static String currentWorkingDirectory;
	
	public static void main(String[] args){
		currentWorkingDirectory = System.getProperty("user.dir");
		Map<String, Thread> repl = new LinkedHashMap<>(); //a record to keep track each command and its last thread
		Scanner s = new Scanner(System.in);
		System.out.print(Message.WELCOME);
		String command;
		while(true) {
			//obtaining the command from the user
			System.out.print(Message.NEWCOMMAND);
			command = s.nextLine();
			if(command.equals("exit")) {
				break;
			} else if (command.equals("repl_jobs")) {
				displayJobs(repl);
			} else if(!command.trim().equals("")) {
				//build a filter list from the command
				ConcurrentFilter filterlist = ConcurrentCommandBuilder.createFiltersFromCommand(command);
				
				//build a thread list from the filter list
				List<Thread> threadList = new ArrayList<>();
				while (filterlist != null) {
					threadList.add(new Thread(filterlist));
					filterlist = (ConcurrentFilter) filterlist.getNext();
				}
				
				//start all threads by a for loop;
				//if "&" in cmd, go directly back to the start of while loop to print "> ";
				//otherwise join() the last filter to make main thread wait for current cmd to finish
				if (threadList.size() != 0) {
					repl.put(command, threadList.get(threadList.size() - 1));
					for (Thread t : threadList) {t.start();}
					if (!command.contains("&")) {
						try {repl.get(command).join();} 
						catch (InterruptedException e) {e.printStackTrace();}
					}
				}
			}
		}
		s.close();
		System.out.print(Message.GOODBYE);
	}
	
	//check "alive" thread values and print relative command keys in order
	public static void displayJobs(Map<String, Thread> repl) {
		int count = 0;
		for (Map.Entry<String, Thread> entry : repl.entrySet()) {
			String command = entry.getKey();
			Thread t = entry.getValue();
			if (t.isAlive()) {
				System.out.println("\t" + ++count + ". " + command);
			}
		}
	}

}
