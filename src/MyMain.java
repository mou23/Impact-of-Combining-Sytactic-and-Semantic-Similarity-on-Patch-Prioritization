import java.io.File;


public class MyMain {
	static String dirLocation = "dataset/buggySmall/";
	static File rootFolder = new File(dirLocation);
	public static void main(String[] args) {
		File[] listOfFiles = rootFolder.listFiles();

		try {
			for (int i = 0; i < listOfFiles.length; i++) {
				scanDirectory(listOfFiles[i]);
//				break;
			}
		} catch(Exception e) {
			e.printStackTrace();
			System.out.println(e.getLocalizedMessage());
		}
	}

	private static void scanDirectory(File folder) throws Exception {
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile() && listOfFiles[i].getName().contains(".java")) {
				String file = listOfFiles[i].getAbsolutePath();
				file = file.substring(file.indexOf(rootFolder.getName())+rootFolder.getName().length()+1);
//				System.out.println("Processing " +file);
				System.out.print(file+",");
				long startingTime = System.nanoTime();
				FaultLocalizer faultLocalizer = FaultLocalizer.createFaultLocalizer();
				faultLocalizer.localizeFault(file);
				PatchEvaluator patchEvaluator = PatchEvaluator.createPatchEvaluator();
				patchEvaluator.prepareSolutionAST(file);
				PatchGenerator patchGenerator = PatchGenerator.createPatchGenerator(); 
				String identifier = listOfFiles[i].getParent();
				identifier = identifier.substring(identifier.indexOf(rootFolder.getName())+rootFolder.getName().length()+1);
				patchGenerator.generatePatch(listOfFiles[i], identifier, startingTime);
				System.out.println();
//				System.out.println();
				break;
			}
			else if (listOfFiles[i].isDirectory()) {
				scanDirectory(new File(folder+"/"+listOfFiles[i].getName()));
			}
		}
	}
}
