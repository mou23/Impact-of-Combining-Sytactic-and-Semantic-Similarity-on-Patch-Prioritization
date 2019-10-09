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
	static String dirLocation = "dataset/buggy0/";
	static File rootFolder = new File(dirLocation);

	public static void main(String[] args) {
		
		File[] listOfFiles = rootFolder.listFiles();

		try {
			for (int i = 0; i < listOfFiles.length; i++) {
				scanDirectory(listOfFiles[i]);
				break;
			}

		} catch(Exception e) {
			System.out.println(e.getMessage());
		}
	}

	private static void scanDirectory(File folder) {
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile() && listOfFiles[i].getName().contains(".java")) {
				String file = listOfFiles[i].getAbsolutePath();
				file = file.substring(file.indexOf(rootFolder.getName())+rootFolder.getName().length()+1);
				System.out.println("Processing " +file);
				long startingTime = System.nanoTime();
				FaultLocalizer faultLocalizer = FaultLocalizer.createFaultLocalizer();
				faultLocalizer.localizeFault(file);
				PatchEvaluator patchEvaluator = PatchEvaluator.createPatchEvaluator();
				patchEvaluator.prepareSolutionAST(file);
				PatchGenerator patchGenerator = PatchGenerator.createPatchGenerator(); 
				String identifier = listOfFiles[i].getParent();
				identifier = identifier.substring(identifier.indexOf(rootFolder.getName())+rootFolder.getName().length()+1);
//				System.out.println(identifier);
				patchGenerator.generatePatch(listOfFiles[i], identifier, startingTime);
				System.out.println();
				System.out.println();
				break;
			}
			else if (listOfFiles[i].isDirectory()) {
				scanDirectory(new File(folder+"/"+listOfFiles[i].getName()));
			}
		}
	}
}
