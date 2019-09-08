import java.io.BufferedReader;
import java.io.FileReader;

public class FaultLocalizer {
	int faultyLine;
	private static FaultLocalizer faultLocalizer;
	
	public static FaultLocalizer createFaultLocalizer() {
		if(faultLocalizer == null) {
			faultLocalizer = new FaultLocalizer();
		}

		return faultLocalizer;
	}
	
	void localizeFault(String filename) {
		try (BufferedReader br = new BufferedReader(new FileReader("codrep/fault location/Dataset1/"+filename+".txt"))) {
			String line;
		    while ((line = br.readLine()) != null) {
		    	faultyLine = Integer.parseInt(line);
		    	System.out.println("Fault in line no "+faultyLine);
		    }
		    br.close();
		} catch(Exception e) {
			System.out.println(e.getMessage());
		}
	}
}
