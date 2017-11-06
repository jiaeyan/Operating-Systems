package cs131.pa1.filter.concurrent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class FileCreator {
    private static String generateFizzBuzz(int max){
    	StringBuffer sb = new StringBuffer();
    	for(int i = 0; i <= max; i++){
    		if (i % 3 == 0 && i % 5 == 0){
    			sb.append("FizzBuzz\n");
    		} else if (i % 3 == 0){
    			sb.append("Fizz\n");
    		} else if (i % 5 == 0){
    			sb.append("Buzz\n");
    		} else {
    			sb.append(i + "\n");
    		}
    	}
    	return sb.toString();
    }
    private static void createFile(String fileName, String content){
    	File f = new File(fileName);
    	PrintWriter pw;
		try {
			pw = new PrintWriter(f);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("This should not happen; we are creating a new file.");
		}
    	pw.print(content);
    	pw.close();
    }
    public static void main (String args[]) {
    	createFile("fizz-buzz-10000.txt", generateFizzBuzz(10000));
    }
}
