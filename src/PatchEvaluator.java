import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.Document;


public class PatchEvaluator {
	private static PatchEvaluator patchEvaluator;
	CompilationUnit solutionCompilationUnit;
	
	private PatchEvaluator() {

	}
	
	public static PatchEvaluator createPatchEvaluator() {
		if(patchEvaluator == null){
			patchEvaluator = new PatchEvaluator();
		}

		return patchEvaluator;
	}
	
	public void prepareSolutionAST(String filename) {
		File file = new File("dataset/fixed/"+filename);
		PatchGenerator patchGenerator = PatchGenerator.createPatchGenerator(); 
		ASTParser parser = ASTParser.newParser(AST.JLS12);
		String fileContent = patchGenerator.readFileToString(file.getAbsolutePath());
		Document document = new Document(fileContent);
		Map<String, String> options = JavaCore.getOptions();
	    options.put("org.eclipse.jdt.core.compiler.source", "1.8");
	    parser.setCompilerOptions(options);
		parser.setSource(document.get().toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setResolveBindings(true);
		parser.setEnvironment(new String[] {}, new String[] {}, null, true);
		parser.setUnitName("fixed.java");
		solutionCompilationUnit = (CompilationUnit) parser.createAST(null);
//		System.out.println(solutionCompilationUnit.toString());
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
//		System.out.println();
	}

	public boolean evaluatePatch(String patch) {
		return solutionCompilationUnit.toString().equals(patch);
	}
}
