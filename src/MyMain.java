import java.io.File;

import org.apache.commons.io.FilenameUtils;

public class MyMain {
	static String dirLocation = "dataset/buggy/";
	static File rootFolder = new File(dirLocation);
	public static void main(String[] args) {
		File[] listOfFiles = rootFolder.listFiles();

		try {
			for (int i = 0; i < listOfFiles.length; i++) {
//				String fileNameWithOutExtension = FilenameUtils.removeExtension(listOfFiles[i].getName());
//				System.out.println("Processing " +fileNameWithOutExtension);
				scanDirectory(listOfFiles[i]);
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
				patchGenerator.generatePatch(listOfFiles[i], startingTime);
//				System.out.println();
//				System.out.println();
				break;
			}
			else if (listOfFiles[i].isDirectory()) {
				scanDirectory(new File(folder+"/"+listOfFiles[i].getName()));
			}
		}
	}
}
