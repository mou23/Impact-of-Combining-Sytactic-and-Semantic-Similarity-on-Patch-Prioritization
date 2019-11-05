import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;


public class PatchGenerator {
	private static PatchGenerator patchGenerator;
	CompilationUnit compilationUnit;
	int correctPatches;
	//	static HashSet<CandidatePatch> candidatePatchesSet = new HashSet<CandidatePatch>();
	ArrayList<CandidatePatch> candidatePatchesList = new ArrayList<CandidatePatch>();
	ArrayList<String>sourceFiles = new ArrayList<String>();
	ArrayList<String>classFiles = new ArrayList<String>();
	Document document;
	File file;
	boolean correctPatchFound;
	long startingTime;
	IngredientCollector ingredientCollector;
	PatchEvaluator patchEvaluator;

	private PatchGenerator() {
		this.patchEvaluator = PatchEvaluator.createPatchEvaluator();
		this.ingredientCollector = IngredientCollector.createIngredientCollector();
	}

	public static PatchGenerator createPatchGenerator() {
		if(patchGenerator == null){
			patchGenerator = new PatchGenerator();
		}

		return patchGenerator;
	}

	void generatePatch(File file, String fileIdentifier, long startingTime) {
		this.startingTime = startingTime;
		this.init();
		//	file = new File("digit003/src/main/java/introclassJava/digits_6e464f2b_003_old.java"); // //D:/thesis/software repair/resources/20/capgen/CapGen/IntroClassJava/dataset/syllables/fcf701e8bed9c75a4cc52a990a577eb0204d7aadf138a4cad08726a847d66e77126f95f06f839ec9224b7e8a887b873fe0d4b6f4311b4e8bd2a36e5028d1feca/002/src/main/java/introclassJava/syllables_fcf701e8_002.java
		this.file = file;

		ASTParser parser = ASTParser.newParser(AST.JLS12);
		String fileContent = readFileToString(file.getAbsolutePath());
		this.document = new Document(fileContent);
		Map<String, String> options = JavaCore.getOptions();
		options.put("org.eclipse.jdt.core.compiler.source", "1.8");
		parser.setCompilerOptions(options);
		parser.setSource(document.get().toCharArray());
		parser.setStatementsRecovery(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		
		File sourcePath = new File("D:/code/"+fileIdentifier);
		File[] folders = sourcePath.listFiles();
		
		for(int i=0; i<folders.length; i++) {
			try {
				if(folders[i].isDirectory()==true) {
					scanDirectory(folders[i]);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		String[] sourceFilesArray = this.sourceFiles.toArray(new String[0]);
		String[] classFilesArray = this.classFiles.toArray(new String[0]);
//		for(int i=0; i<this.sourceFiles.size(); i++) {
//			System.out.println(sourceFilesArray[i]);
//		}
		
		parser.setEnvironment(classFilesArray, sourceFilesArray , null, true);
		parser.setUnitName("file.java");
		this.compilationUnit = (CompilationUnit) parser.createAST(null);

//		this.compilationUnit.accept(new VariableCollector());
		this.compilationUnit.accept(ingredientCollector);
		
		//		System.out.println("INGREDIENT");
		//		System.out.println(this.ingredientCollector.fixingIngredients.size());
		//		for(int i=0; i<this.ingredientCollector.fixingIngredients.size(); i++) {
		//			System.out.println(this.ingredientCollector.fixingIngredients.get(i));
		//		}

//		System.out.println("VARIABLES "+VariableCollector.variables.size());
//		for(int i=0; i<VariableCollector.variables.size(); i++) {
//			Variable v = VariableCollector.variables.get(i);
//			System.out.println(v);
//		}
		ReplaceHandler replaceHandler = ReplaceHandler.createReplaceHandler();
		for(int i=0; i<this.ingredientCollector.faultyNodes.size(); i++) {
			Node faultyNode = this.ingredientCollector.faultyNodes.get(i);
			replaceHandler.replace(faultyNode);
			//			System.out.println(faultyNode);
			//			this.generatePatchTemplate(faultyNode);
		}
//////		////		candidatePatchesList = new ArrayList<CandidatePatch>(candidatePatchesSet);
		Collections.sort(this.candidatePatchesList);
////////		//
		this.writeCandidatePatches(fileIdentifier);
////////		////		System.out.println((long)15*60*1000000000);
		this.correctPatchFound = false;
//		System.out.println(this.candidatePatchesList.size() +" Patches Generated");
		for(int i=0; i<this.candidatePatchesList.size(); i++) { //candidatePatches.size()
			//			long currentTime = System.nanoTime();
			//			System.out.println("Patch no: "+(i+1)+ " ");
			//			System.out.println(this.candidatePatchesList.get(i).toString()+"\n");
			//			
			//			if((currentTime - startingTime) >= (long)30*60*1000000000) {
			//				System.out.println("time-up!!!!!!!!!!!!!!!!");
			//				break;
			//			}
			this.document = new Document(fileContent);
			CompilationUnit compilationUnitCopy = (CompilationUnit)ASTNode.copySubtree(compilationUnit.getAST(), compilationUnit);

			ASTRewrite rewriter = ASTRewrite.create(compilationUnitCopy.getAST()); //compilationUnit.getAST();
			this.generateConcretePatch(rewriter, candidatePatchesList.get(i));
			if(correctPatchFound==true) {
				this.correctPatches++;
				System.out.println("Total Candidate Patches: " +candidatePatchesList.size());
				System.out.println("Correct Patch Rank: " + (i+1));
//				System.out.print(candidatePatchesList.size()+",");
//				System.out.print((i+1));
				break;
				//				correctPatchFound = false;
			}
		}
//		if(IngredientCollector.problem==true) {
//			System.out.println("DELETE "+file.getName());
//			file.delete();
//		}
//		System.out.println(this.correctPatches + " Correct Patches Found");
	}

	private void scanDirectory(File folder) throws Exception {
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isDirectory()) {
				String str = listOfFiles[i].getAbsolutePath();
				if(this.sourceFiles.contains(str)==false) {
					this.sourceFiles.add(str);
				}
				scanDirectory(new File(folder+"/"+listOfFiles[i].getName()));
			}
			else if (listOfFiles[i].getName().endsWith(".jar")) {
				this.classFiles.add(listOfFiles[i].getAbsolutePath());
			}
		}
	}


	void writeCandidatePatches(String fileIdentifier) {
//		String fileNameWithOutExtension = FilenameUtils.removeExtension(this.file.getAbsolutePath());
		File newfile = new File("dataset/patch/"+fileIdentifier.replace("/", "_")+"semantic_token.csv");
		try {
			FileWriter fileWrite = new FileWriter(newfile.getAbsolutePath());
			for(int i=0; i<this.candidatePatchesList.size(); i++) {
				fileWrite.write(this.candidatePatchesList.get(i).toString()+"\n");
			}

			fileWrite.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	private void init() {
		Tokenizer.stringTokenPairList.clear();
//		IngredientCollector.problem = false;
		this.correctPatches = 0;
		this.candidatePatchesList.clear();
		this.sourceFiles.clear();
		this.classFiles.clear();
//		VariableCollector.variables.clear();
		this.ingredientCollector.faultyNodes.clear();
		this.ingredientCollector.fixingIngredients.clear();
	}

	public String readFileToString(String filePath) {
		StringBuilder fileData = new StringBuilder(100000);
		try{		
			BufferedReader reader = new BufferedReader(new FileReader(filePath));

			char[] buffer = new char[10];
			int numRead = 0;
			while ((numRead = reader.read(buffer)) != -1) {
				String readData = String.valueOf(buffer, 0, numRead);
				fileData.append(readData);
				buffer = new char[1024];
			}

			reader.close();
		} catch(Exception e) {
			System.out.println(e.getMessage());
		}

		return  fileData.toString();	
	}


	private void generateConcretePatch(ASTRewrite rewriter, CandidatePatch candidatePatch) {
		try{
			if(candidatePatch.mutationOperation.equals("replace")) {
				rewriter.replace(candidatePatch.faultyNode, candidatePatch.fixingIngredient, null);
			}

			TextEdit edits = rewriter.rewriteAST(document,null);
			edits.apply(document);
			ASTParser parser = ASTParser.newParser(AST.JLS12);

			Map<String, String> options = JavaCore.getOptions();
			options.put("org.eclipse.jdt.core.compiler.source", "1.8");
			parser.setCompilerOptions(options);
			parser.setSource(document.get().toCharArray());
			parser.setStatementsRecovery(true);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);

			//			System.out.println(compilationUnit.toString());
			correctPatchFound = this.patchEvaluator.evaluatePatch(compilationUnit.toString());
			if(correctPatchFound == true) {
				IProblem[] problems = compilationUnit.getProblems();
			    for(IProblem problem : problems) {
			    	if(problem.getMessage().contains("Syntax error")) {
//			    		System.out.println("problem: " + problem.getMessage() + problem.getSourceStart());
			    		correctPatchFound = false;
//			    		System.out.println(candidatePatch.faultyNode);
//				    	System.out.println(candidatePatch.fixingIngredient);
				    	break;
			    	}	
			    }
			    if(correctPatchFound == true) {
			    	System.out.println("Correct Patch Generated!");//+ " Elapsed Time: " +(System.nanoTime()-startingTime));
			    	System.out.println(candidatePatch.faultyNode);
			    	System.out.println(candidatePatch.fixingIngredient);
			    }
			}


		} catch(Exception e) {
			//						System.out.println("ERROR!!!!!!!!!!!!!!!!!!!");
			//						System.out.println(candidatePatch.faultyNode+ " "+compilationUnit.getLineNumber(candidatePatch.faultyNode.getStartPosition()));
			//						System.out.println(candidatePatch.fixingIngredient+ " "+compilationUnit.getLineNumber(candidatePatch.fixingIngredient.getStartPosition()));
			//						System.out.println(ModelExtractor.getNodeType(candidatePatch.faultyNode));
			//						System.out.println(ModelExtractor.getNodeType(candidatePatch.fixingIngredient));
			//						System.out.println(e.getMessage());
			//			e.printStackTrace();
		} catch(StackOverflowError overflow) {
			//						System.out.println("OVERFLOW!!!!!!!!!!!!!!!!");
			//						System.out.println(candidatePatch.faultyNode+ " "+compilationUnit.getLineNumber(candidatePatch.faultyNode.getStartPosition()));
			//						System.out.println(candidatePatch.fixingIngredient+ " "+compilationUnit.getLineNumber(candidatePatch.fixingIngredient.getStartPosition()));
			//						System.out.println(ModelExtractor.getNodeType(candidatePatch.faultyNode));
			//						System.out.println(ModelExtractor.getNodeType(candidatePatch.fixingIngredient));
			//						System.out.println(overflow.getMessage());
			//						System.out.println();
		}
	}

	boolean deleteDirectory(File directoryToBeDeleted) {
		File[] contents = directoryToBeDeleted.listFiles();
		if (contents != null) {
			for (File file : contents) {
				deleteDirectory(file);
			}
		}
		return directoryToBeDeleted.delete();
	}

	void generateProgramVariant(File file) {
		try {
			FileWriter fileWrite = new FileWriter(file.getAbsolutePath());
			fileWrite.write(document.get());
			fileWrite.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}
