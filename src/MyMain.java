import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import net.lingala.zip4j.core.ZipFile;

public class MyMain {
	static String dirLocation = "dataset/buggy/";
	static File rootFolder = new File(dirLocation);
	static ArrayList<String> duplicates=new ArrayList<String>();  
	static File destinationProject = new File("D:/project/");
	static int counter = 0;

	public static void main(String[] args) {
		File[] listOfFiles = rootFolder.listFiles();
		
		retrieveDuplicates();
		try {
			String base = "D:\\thesis\\all\\";
			for(int i=0; i<duplicates.size(); i++) {
				String file = duplicates.get(i);
				file = file.substring(file.indexOf(base)+base.length());
				System.out.println(destinationProject+"/"+file);
				FileUtils.deleteDirectory(new File(destinationProject+"/"+file));
			}
////			System.out.println(listOfFiles.length);
//			for (int i = 0; i < listOfFiles.length; i++) {
//				//				String fileNameWithOutExtension = FilenameUtils.removeExtension(listOfFiles[i].getName());
//				//				System.out.println("Processing " +fileNameWithOutExtension);
//				scanDirectory(listOfFiles[i]);
////				break;
//			}
//			System.out.println("Total files "+counter);
		} catch(Exception e) {
			System.out.println(e.getMessage());
		}
	}

	private static void scanDirectory(File folder) {
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
//			System.out.println(listOfFiles[i]);
			if (listOfFiles[i].isFile() && listOfFiles[i].getName().contains(".java")) {
				counter++;
//				String file = listOfFiles[i].getParent();
//				file = file.substring(file.indexOf(rootFolder.getName())+rootFolder.getName().length()+1);
//				System.out.println("Processing " +file);
//				if(duplicates.contains("D:\\thesis\\all\\"+file)) {
//					listOfFiles[i].delete();
//				}


				//				long startingTime = System.nanoTime();
				//				FaultLocalizer faultLocalizer = FaultLocalizer.createFaultLocalizer();
				//				faultLocalizer.localizeFault(file);
				//				PatchEvaluator patchEvaluator = PatchEvaluator.createPatchEvaluator();
				//				patchEvaluator.prepareSolutionAST(file);
				//				PatchGenerator patchGenerator = PatchGenerator.createPatchGenerator(); 
				//				patchGenerator.generatePatch(listOfFiles[i], startingTime);
				//				System.out.println();
				//				System.out.println();
				break;
			}
			else if (listOfFiles[i].isDirectory()) {
				scanDirectory(new File(folder+"/"+listOfFiles[i].getName()));
			}
		}
	}

	private static void retrieveDuplicates() {
		try {
			BufferedReader fileReader = new BufferedReader(new FileReader("duplicateFiles.txt"));
			String line;
			while ((line = fileReader.readLine()) != null) {
				duplicates.add(line);
			}
			fileReader.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
