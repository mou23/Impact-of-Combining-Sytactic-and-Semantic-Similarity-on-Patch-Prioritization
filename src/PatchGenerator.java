import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jdt.core.JavaCore;
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
	
	void generatePatch(File file, long startingTime) {
		this.startingTime = startingTime;
		this.init();
		//	file = new File("digit003/src/main/java/introclassJava/digits_6e464f2b_003_old.java"); // //D:/thesis/software repair/resources/20/capgen/CapGen/IntroClassJava/dataset/syllables/fcf701e8bed9c75a4cc52a990a577eb0204d7aadf138a4cad08726a847d66e77126f95f06f839ec9224b7e8a887b873fe0d4b6f4311b4e8bd2a36e5028d1feca/002/src/main/java/introclassJava/syllables_fcf701e8_002.java
		this.file = file;

		ASTParser parser = ASTParser.newParser(AST.JLS10);
		String fileContent = readFileToString(file.getAbsolutePath());
		this.document = new Document(fileContent);
		Map<String, String> options = JavaCore.getOptions();
	    options.put("org.eclipse.jdt.core.compiler.source", "1.8");
	    parser.setCompilerOptions(options);
		parser.setSource(document.get().toCharArray());
		parser.setStatementsRecovery(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setResolveBindings(true);
		parser.setEnvironment(new String[] {}, new String[] {}, null, true);
		parser.setUnitName("file.java");
		this.compilationUnit = (CompilationUnit) parser.createAST(null);

		this.compilationUnit.accept(new VariableCollector());
		this.compilationUnit.accept(ingredientCollector);
//		System.out.println("INGREDIENT");
//		System.out.println(this.ingredientCollector.fixingIngredients.size());
//		for(int i=0; i<this.ingredientCollector.fixingIngredients.size(); i++) {
//			System.out.println(this.ingredientCollector.fixingIngredients.get(i));
//		}
		
//		System.out.println("VARIABLES");
//		for(int i=0; i<VariableCollector.variables.size(); i++) {
//			System.out.println(VariableCollector.variables.get(i));
//		}
		ReplaceHandler replaceHandler = ReplaceHandler.createReplaceHandler();
		for(int i=0; i<this.ingredientCollector.faultyNodes.size(); i++) {
			Node faultyNode = this.ingredientCollector.faultyNodes.get(i);
			replaceHandler.replace(faultyNode);
//			System.out.println(faultyNode);
//			this.generatePatchTemplate(faultyNode);
		}
////		candidatePatchesList = new ArrayList<CandidatePatch>(candidatePatchesSet);
//	    Collections.sort(this.candidatePatchesList);
//
		this.writeCandidatePatches();
////		System.out.println((long)15*60*1000000000);
		this.correctPatchFound = false;
//		System.out.println(this.candidatePatchesList.size() +" Patches Generated");
		for(int i=0; i<this.candidatePatchesList.size(); i++) { //candidatePatches.size()
//			long currentTime = System.nanoTime();
//			System.out.println("Patch no: "+(i+1)+ " ");
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
				break;
//				correctPatchFound = false;
			}
//			//				if(count==574) {
//			//					break;
//			//				}
//			//				break;
//			//				System.out.println(document.get());
		}
		
//		System.out.println(this.correctPatches + " Correct Patches Found");
	}
	
	void writeCandidatePatches() {
		String fileNameWithOutExtension = FilenameUtils.removeExtension(this.file.getName());
		File newfile = new File("codrep/patch/Dataset1/"+fileNameWithOutExtension+".csv");
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
		this.correctPatches = 0;
		this.candidatePatchesList.clear();
		VariableCollector.variables.clear();
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

//	public void generatePatchTemplate(FaultyNode faultyNode) {
//		ReplaceHandler replaceHandler = ReplaceHandler.createReplaceHandler();
//		if(faultyNode.type.equals("SIMPLE_NAME")) {
//			// System.out.println("SIMPLE_NAME");
//			SimpleName simpleName = (SimpleName) faultyNode.node;
//			IBinding binding = simpleName.resolveTypeBinding();
//			if(binding == null) {
//				return;
//			}
//			replaceHandler.replace(binding, faultyNode, 0.1721);
//		}
//
//		else if(faultyNode.type.equals("STRING_LITERAL")) {
//			// System.out.println("STRING_LITERAL");
//			StringLiteral stringLiteral = (StringLiteral) faultyNode.node;
//			IBinding binding = stringLiteral.resolveTypeBinding();
//			if(binding == null) {
//				return;
//			}
//			replaceHandler.replace(binding, faultyNode, 0.1310);
//		}
//
//		else if(faultyNode.type.equals("INFIX_EXPRESSION")) {
//			//			System.out.println("INFIX_EXPRESSION " + faultyNode);
////			DeleteHandler.deleteUnderInfixExpression(faultyNode);
//			InfixExpression infixExpression = (InfixExpression) faultyNode.node;
//			IBinding binding = infixExpression.resolveTypeBinding();
//			if(binding == null) {
//				return;
//			}
//
//			replaceHandler.replace(binding, faultyNode, 0.0201);
//		}
//
//		else if(faultyNode.type.equals("QUALIFIED_NAME")) {
//			// System.out.println("QUALIFIED_NAME");
//			QualifiedName qualifiedName = (QualifiedName) faultyNode.node;
//			IBinding binding = qualifiedName.resolveTypeBinding();
//			if(binding == null) {
//				return;
//			}
//
//			replaceHandler.replace(binding, faultyNode, 0.0197);
//		}
//
//		else if(faultyNode.type.equals("CONDITIONAL_EXPRESSION")) {
//			// System.out.println("CONDITIONAL_EXPRESSION");
//			ConditionalExpression conditionalExpression = (ConditionalExpression) faultyNode.node;
//			IBinding binding = conditionalExpression.resolveTypeBinding();
//			if(binding == null) {
//				return;
//			}
//
//			replaceHandler.replace(binding, faultyNode, 0.0135);
//		}
//
//		else if(faultyNode.type.equals("SIMPLE_TYPE")) {
//			// System.out.println("SIMPLE_TYPE");
//			SimpleType simpleType = (SimpleType) faultyNode.node;
//			IBinding binding = simpleType.resolveBinding();
//			if(binding == null) {
//				return;
//			}
//
//			replaceHandler.replace(binding, faultyNode, 0.0082);
//		}
//
//		else if(faultyNode.type.equals("BOOLEAN_LITERAL")) {
//			// System.out.println("BOOLEAN_LITERAL");
//			BooleanLiteral booleanLiteral = (BooleanLiteral) faultyNode.node;
//			IBinding binding = booleanLiteral.resolveTypeBinding();
//			if(binding == null) {
//				return;
//			}
//
//			replaceHandler.replace(binding, faultyNode, 0.0075);
//		}
//		//		
//		else if(faultyNode.type.equals("PARAMETERIZED_TYPE")) {
//			// System.out.println("PARAMETERIZED_TYPE");
//			ParameterizedType parameterizedType = (ParameterizedType) faultyNode.node;
//			IBinding binding = parameterizedType.resolveBinding();
//			if(binding == null) {
//				return;
//			}
//
//			replaceHandler.replace(binding, faultyNode, 0.0038);
//		}
//		//		
//		else if(faultyNode.type.equals("PRIMITIVE_TYPE")) {
//			// System.out.println("PRIMITIVE_TYPE");
//			PrimitiveType primitiveType = (PrimitiveType) faultyNode.node;
//			IBinding binding = primitiveType.resolveBinding();
//			if(binding == null) {
//				return;
//			}
//
//			replaceHandler.replace(binding, faultyNode, 0.0009);
//		}
//		//		
//		else if(faultyNode.type.equals("ASSIGNMENT")) {
//			// System.out.println("ASSIGNMENT");
//			Assignment assignment = (Assignment) faultyNode.node;
//			IBinding binding = assignment.resolveTypeBinding();
//			if(binding == null) {
//				return;
//			}
//
//			replaceHandler.replace(binding, faultyNode, 0.0003);
//		}
//
//		else if(faultyNode.type.equals("METHOD_DECLARATION")) {
//			// System.out.println("METHOD_DECLARATION");
////			InsertHandler.insertUnderMethodDeclaration(faultyNode);
////			DeleteHandler.deleteUnderMethodDeclaration(faultyNode, 0.0157);
//		}
//
//		else if(faultyNode.type.equals("METHOD_INVOCATION")) {
//			// System.out.println("METHOD_INVOCATION");
//			//			InsertHandler.insert("SIMPLE_NAME", faultyNode, 0.0313);
//			//
////			InsertHandler.insertUnderMethodInvocation(faultyNode);
//			//
//			//			InsertHandler.insert("INFIX_EXPRESSION", faultyNode, 0.0125);
//			//
//			//			InsertHandler.insert("QUALIFIED_NAME", faultyNode, 0.0075);
//			//
//			//			InsertHandler.insert("BOOLEAN_LITERAL", faultyNode, 0.0047);
//		}
//
//		else if(faultyNode.type.equals("IF_STATEMENT")) {
//			// System.out.println("IF_STATEMENT");
////			InsertHandler.insertUnderIfStatement(faultyNode);
////			DeleteHandler.deleteUnderIfStatement(faultyNode);
//		}
//
//		else if(faultyNode.type.equals("TRY_STATEMENT")) {
//			// System.out.println("TRY_STATEMENT");
////			InsertHandler.insertUnderTryStatement(faultyNode);
//		}
//
//		else if(faultyNode.type.equals("CATCH_CLAUSE")) {
////			DeleteHandler.deleteUnderCatchClause(faultyNode, 0.0031);
//		}
//	}


	private void generateConcretePatch(ASTRewrite rewriter, CandidatePatch candidatePatch) {
		try{
			if(candidatePatch.mutationOperation.equals("replace")) {
				rewriter.replace(candidatePatch.faultyNode, candidatePatch.fixingIngredient, null);
			}

			TextEdit edits = rewriter.rewriteAST(document,null);
			edits.apply(document);
			ASTParser parser = ASTParser.newParser(AST.JLS10);
			
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
				System.out.println("Correct Patch Generated!");//+ " Elapsed Time: " +(System.nanoTime()-startingTime));
				System.out.println(candidatePatch.faultyNode);
				System.out.println(candidatePatch.fixingIngredient);
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