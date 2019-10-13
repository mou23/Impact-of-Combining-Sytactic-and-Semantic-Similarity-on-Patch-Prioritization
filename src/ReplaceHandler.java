import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.SimpleName;

public class ReplaceHandler {
	private static ReplaceHandler replaceHandler;
	CompatibilityChecker compatibilityChecker;
	PatchListUpdater patchListUpdater;
	private ReplaceHandler() {
		this.compatibilityChecker = CompatibilityChecker.createCompatibilityChecker(); 
		this.patchListUpdater = PatchListUpdater.createPatchListUpdater();
	}

	public static ReplaceHandler createReplaceHandler() {
		if(replaceHandler == null){
			replaceHandler = new ReplaceHandler();
		}

		return replaceHandler;
	}

	public void replace(Node faultyNode) {
		IngredientCollector ingredientCollector = IngredientCollector.createIngredientCollector();
		for(int i = 0; i<ingredientCollector.fixingIngredients.size(); i++) {
			Node fixingIngredient = ingredientCollector.fixingIngredients.get(i);

			if(!faultyNode.node.toString().equals(fixingIngredient.node.toString())) {
				//				if(this.compatibilityChecker.checkCompatibility(faultyNode.node, fixingIngredient, "replace")==true) {
				//					System.out.println("FAULT "+ faultyNode.node.toString() + " line:"+faultyNode.startLine);
				//					System.out.println("FIX " + fixingIngredient.node.toString()+ " line:"+fixingIngredient.startLine);
				CandidatePatch candidatePatch = new CandidatePatch();
				candidatePatch.faultyNode = faultyNode.node;
				candidatePatch.fixingIngredient = fixingIngredient.node;
				candidatePatch.mutationOperation = "replace";

//				ModelExtractor modelExtractor = ModelExtractor.createModelExtractor();
//				candidatePatch.genealogyScore = modelExtractor.getGenealogySimilarityScore(faultyNode.genealogy, fixingIngredient.genealogy);
//				if(faultyNode.type.equals("SIMPLE_NAME") && fixingIngredient.type.equals("SIMPLE_NAME")) {
//					IBinding faultyBinding = ((Expression)faultyNode.node).resolveTypeBinding();
//					IBinding fixingBinding = ((Expression)fixingIngredient.node).resolveTypeBinding();
//					if(faultyBinding!=null && fixingBinding!=null && faultyBinding.equals(fixingBinding)) {
//						//							System.out.println(faultyNode.node + " line : " + faultyNode.startLine);
//						//							System.out.println(fixingIngredient.node + " line : " + fixingIngredient.startLine);
//						candidatePatch.variableScore = 1.0;
//					}
//				}
//				else {
//					candidatePatch.variableScore = modelExtractor.getVariableSimilarityScore(faultyNode.variableAccessed, fixingIngredient.variableAccessed);
//				}
//				candidatePatch.score = candidatePatch.genealogyScore*candidatePatch.variableScore;
//
//				if(candidatePatch.score>0) {
				this.patchListUpdater.updatePatchList(candidatePatch);
//				}
			}

		}
	}
}
