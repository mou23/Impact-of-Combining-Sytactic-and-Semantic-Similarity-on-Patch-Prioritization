import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;

public class IngredientCollector extends ASTVisitor {
	private static IngredientCollector ingredientCollector;
	ArrayList<Node> faultyNodes;
	ArrayList<Node> fixingIngredients;
//    static boolean problem = false;
	private IngredientCollector() {
		faultyNodes = new ArrayList<Node>();
		fixingIngredients = new ArrayList<Node>();
	}
	
	public static IngredientCollector createIngredientCollector() {
		if(ingredientCollector == null){
			ingredientCollector = new IngredientCollector();
		}

		return ingredientCollector;
	}
	
	
	@Override
	public void preVisit(ASTNode node) {
//		if(problem==false) {
//			PatchGenerator patchGenerator = PatchGenerator.createPatchGenerator();
//			if(node.getNodeType()==ASTNode.IMPORT_DECLARATION) {
//				ImportDeclaration id = (ImportDeclaration)node;
//				IBinding ib = id.resolveBinding();
//				if(ib.toString().contains("RecoveredTypeBinding")) {
//					System.out.println(id.toString().replace("\n", "") + " "+ ib.isRecovered());
//					System.out.println("PROBLEM!!!!!!!!!!!!!!!!!!!!");
//					System.out.println(ib);
//					System.out.println();
//					problem = true;
//				}
//			}
//		}
		
		if(node instanceof Expression) {
//			System.out.println("NODE " +node);
//			System.out.println(patchGenerator.compilationUnit.getLineNumber(node.getStartPosition()) + " "+patchGenerator.compilationUnit.getLineNumber(node.getStartPosition()+node.getLength()));
			this.collectFaultyNode(node);
			this.collectFixingIngredients(node);
		}		
	}

	private void collectFaultyNode(ASTNode node) {
		PatchGenerator patchGenerator = PatchGenerator.createPatchGenerator();
		
		Node faultyNode = new Node();
		faultyNode.node = node;
		faultyNode.startLine = patchGenerator.compilationUnit.getLineNumber(node.getStartPosition());
		faultyNode.endLine = patchGenerator.compilationUnit.getLineNumber(node.getStartPosition()+node.getLength());
		
		FaultLocalizer faultLocalizer = FaultLocalizer.createFaultLocalizer();
		if(faultLocalizer.faultyLine==faultyNode.startLine && faultLocalizer.faultyLine==faultyNode.endLine) {
			ModelExtractor modelExtractor = ModelExtractor.createModelExtractor();
			faultyNode.type = modelExtractor.getNodeType(node);
			faultyNode.genealogy = modelExtractor.getGenealogyContext(node);
			faultyNode.variableAccessed = modelExtractor.getVariableContext(node);
			this.faultyNodes.add(faultyNode);
		}
	}

	private void collectFixingIngredients(ASTNode node) {
		PatchGenerator patchGenerator = PatchGenerator.createPatchGenerator();
		ModelExtractor modelExtractor = ModelExtractor.createModelExtractor();
		
		Node fixingIngredient = new Node();
		fixingIngredient.node = node;
		fixingIngredient.startLine = patchGenerator.compilationUnit.getLineNumber(node.getStartPosition());
		fixingIngredient.endLine = patchGenerator.compilationUnit.getLineNumber(node.getStartPosition()+node.getLength());
		fixingIngredient.type = modelExtractor.getNodeType(node);
//		System.out.print("Node " +node);
//		System.out.println(" "+fixingIngredient.startLine);
		fixingIngredient.genealogy = modelExtractor.getGenealogyContext(node);
		fixingIngredient.variableAccessed = modelExtractor.getVariableContext(node);
		this.fixingIngredients.add(fixingIngredient);
	} 	
}