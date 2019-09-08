import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;

public class IngredientCollector extends ASTVisitor {
	private static IngredientCollector ingredientCollector;
	ArrayList<Node> faultyNodes = new ArrayList<Node>();
	ArrayList<Node> fixingIngredients = new ArrayList<Node>();
    
	private IngredientCollector() {
		
	}
	
	public static IngredientCollector createIngredientCollector() {
		if(ingredientCollector == null){
			ingredientCollector = new IngredientCollector();
		}

		return ingredientCollector;
	}
	
	
	@Override
	public void preVisit(ASTNode node) {
		PatchGenerator patchGenerator = PatchGenerator.createPatchGenerator();
		
		if(node instanceof Expression) {
//			System.out.println("NODE " +node);
//			System.out.println(patchGenerator.compilationUnit.getLineNumber(node.getStartPosition()) + " "+patchGenerator.compilationUnit.getLineNumber(node.getStartPosition()+node.getLength()));
			this.collectFaultyNode(node);
			this.findFixingIngredients(node);
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
			this.faultyNodes.add(faultyNode);
		}
	}

	private void findFixingIngredients(ASTNode node) {
		PatchGenerator patchGenerator = PatchGenerator.createPatchGenerator();
		ModelExtractor modelExtractor = ModelExtractor.createModelExtractor();
		
		Node fixingIngredient = new Node();
		fixingIngredient.node = node;
		fixingIngredient.startLine = patchGenerator.compilationUnit.getLineNumber(node.getStartPosition());
		fixingIngredient.endLine = patchGenerator.compilationUnit.getLineNumber(node.getStartPosition()+node.getLength());
		fixingIngredient.type = modelExtractor.getNodeType(node);
		this.fixingIngredients.add(fixingIngredient);
		
	} 
}