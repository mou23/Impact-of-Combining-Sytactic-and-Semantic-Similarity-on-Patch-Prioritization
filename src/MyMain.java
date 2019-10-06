import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.apache.commons.io.FilenameUtils;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;

public class MyMain {
	static String dirLocation = "dataset/buggy/";
	static File rootFolder = new File(dirLocation);
	static HashMap<String,String> urls=new HashMap<String,String>();  
	static File destinationProject = new File("D:/project/");

	public static void main(String[] args) {
		File[] listOfFiles = rootFolder.listFiles();

		retrieveURL();

		try {
			for (int i = 0; i < listOfFiles.length; i++) {
				//				String fileNameWithOutExtension = FilenameUtils.removeExtension(listOfFiles[i].getName());
				//				System.out.println("Processing " +fileNameWithOutExtension);
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
				String file = listOfFiles[i].getParent();
				file = file.substring(file.indexOf(rootFolder.getName())+rootFolder.getName().length()+1);
				System.out.print("Processing " +file + ",");
				System.out.println(urls.get("D:\\thesis\\all\\"+file));

				try {
					URL url = new URL(urls.get("D:\\thesis\\all\\"+file)+".zip");
					InputStream input = url.openStream();

					File f = (new File(destinationProject+"/"+file+"/project.zip"));
					f.getParentFile().mkdirs();
					FileOutputStream output = new FileOutputStream(f);

					byte[] buffer = new byte[4096];
					int n = 0;
					while (-1 != (n = input.read(buffer))) {
						output.write(buffer, 0, n);
					}

					input.close();
					output.close();

					ZipFile zipFile = new ZipFile(f.getAbsolutePath());
					zipFile.extractAll(f.getParentFile().getAbsolutePath());
					
					f.delete();
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}


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

	private static void retrieveURL() {
		try {
			BufferedReader fileReader = new BufferedReader(new FileReader("projectURLs.txt"));
			String line;
			while ((line = fileReader.readLine()) != null) {
				String[] str = line.split(",");
				urls.put(str[0],str[1]);  
				//			System.out.println(str[0]);
			}
			fileReader.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
