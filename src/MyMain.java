import java.io.File;

import org.apache.commons.io.FilenameUtils;

public class MyMain {
	public static void main(String[] args) {
		String dirLocation = "codrep/buggy/Dataset1/";
		File folder = new File(dirLocation);
		File[] listOfFiles = folder.listFiles();

		try {
			for (int i = 0; i < listOfFiles.length; i++) {
				String fileNameWithOutExtension = FilenameUtils.removeExtension(listOfFiles[i].getName());
				System.out.println("Processing " +fileNameWithOutExtension);
				long startingTime = System.nanoTime();
				FaultLocalizer faultLocalizer = FaultLocalizer.createFaultLocalizer();
				faultLocalizer.localizeFault(fileNameWithOutExtension);
				PatchEvaluator patchEvaluator = PatchEvaluator.createPatchEvaluator();
				patchEvaluator.prepareSolutionAST(fileNameWithOutExtension);
				PatchGenerator patchGenerator = PatchGenerator.createPatchGenerator(); 
				patchGenerator.generatePatch(listOfFiles[i], startingTime);
				System.out.println();
				System.out.println();
			}
		} catch(Exception e) {
			System.out.println(e.getMessage());
		}
	}
}
