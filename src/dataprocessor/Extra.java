package dataprocessor;

import java.io.BufferedReader;
import java.io.FileReader;


public class Extra {
	public static void main(String[] args) throws Exception {
		BufferedReader fileReader = new BufferedReader(new FileReader("all.txt"));
		String line;
		boolean process = false;
		boolean solution = false;
		String filename = "";
		int counter = 0;
		while ((line = fileReader.readLine()) != null) {
			if(line.contains("Processing")) {
				String[] str = line.split(" ");
				if(process==false || solution==false) {
					System.out.println(filename);
					counter++;
				}
				filename = str[1];
				process = true;
				solution = false;
			}
			if(line.contains("Correct Patch Generated!")) {
				solution = true;
			}
		}
		System.out.println("total " + counter);
	}
}
