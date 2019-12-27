package dataprocessor;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;

public class DependencyManager {
	static File rootFolder = new File("D:/code/apache_activemq/mf2/4");
	public static void main(String[] args) {
		File[] listOfFiles = rootFolder.listFiles();

		try {
			for (int i = 0; i < listOfFiles.length; i++) {
				scanDirectory(listOfFiles[i]);
			}
		} catch(Exception e) {
			e.printStackTrace();
			System.out.println(e.getLocalizedMessage());
		}
	}
	
	private static void scanDirectory(File folder) throws Exception {
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile() && listOfFiles[i].getName().contains("pom.xml")) {
				
				System.out.println("FOUND "+listOfFiles[i].getAbsolutePath());
				InvocationRequest request = new DefaultInvocationRequest();
				request.setPomFile( new File(listOfFiles[i].getAbsolutePath()) );
				request.setGoals(Arrays.asList( "install", "dependency:copy-dependencies"));
				 
				Invoker invoker = new DefaultInvoker();
				invoker.setMavenHome(new File(System.getenv("M2_HOME")));
				InvocationResult result = invoker.execute( request );
				 
				if ( result.getExitCode() != 0 )
				{
				   System.err.println( "Error!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" );
				}
				return;
			}
		}
				
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isDirectory()) {
				scanDirectory(new File(folder+"/"+listOfFiles[i].getName()));
			}
		}
	}
}
