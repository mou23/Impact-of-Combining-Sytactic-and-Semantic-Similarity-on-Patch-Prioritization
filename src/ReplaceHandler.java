import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;

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
//									System.out.println("FAULT "+ faultyNode.node.toString() + " line:"+faultyNode.startLine);
//									System.out.println("FIX " + fixingIngredient.node.toString()+ " line:"+fixingIngredient.startLine);
				CandidatePatch candidatePatch = new CandidatePatch();
				candidatePatch.faultyNode = faultyNode.node;
				candidatePatch.fixingIngredient = fixingIngredient.node;
				candidatePatch.mutationOperation = "replace";

				ModelExtractor modelExtractor = ModelExtractor.createModelExtractor();
				candidatePatch.genealogyScore = modelExtractor.getGenealogySimilarityScore(faultyNode.genealogy, fixingIngredient.genealogy);
				if(faultyNode.type.equals("SIMPLE_NAME") || fixingIngredient.type.equals("SIMPLE_NAME")) {
					
					IBinding faultyBinding = ((Expression)faultyNode.node).resolveTypeBinding();
					IBinding fixingBinding = ((Expression)fixingIngredient.node).resolveTypeBinding();
					if(faultyBinding!=null && fixingBinding!=null && faultyBinding.equals(fixingBinding)) {
						//							System.out.println(faultyNode.node + " line : " + faultyNode.startLine);
						//							System.out.println(fixingIngredient.node + " line : " + fixingIngredient.startLine);
						candidatePatch.variableScore = 1.0;
					}
//					System.out.println("FAULT "+faultyNode.toString()  + " "+ faultyBinding);
//					System.out.println("FIX " +fixingIngredient.toString()+ " "+fixingBinding);
				}
				
				else if(faultyNode.type.equals("BOOLEAN_LITERAL") || fixingIngredient.type.equals("BOOLEAN_LITERAL")
						|| faultyNode.type.equals("NUMBER_LITERAL") || fixingIngredient.type.equals("NUMBER_LITERAL")
						|| faultyNode.type.equals("NULL_LITERAL") || fixingIngredient.type.equals("NULL_LITERAL")
						|| faultyNode.type.equals("CHARACTER_LITERAL") || fixingIngredient.type.equals("CHARACTER_LITERAL")
						|| faultyNode.type.equals("STRING_LITERAL") || fixingIngredient.type.equals("STRING_LITERAL")) {
					IBinding faultyBinding = ((Expression)faultyNode.node).resolveTypeBinding();
					IBinding fixingBinding = ((Expression)fixingIngredient.node).resolveTypeBinding();
					if(faultyBinding!=null && fixingBinding!=null && faultyBinding.equals(fixingBinding)) {
						//							System.out.println(faultyNode.node + " line : " + faultyNode.startLine);
						//							System.out.println(fixingIngredient.node + " line : " + fixingIngredient.startLine);
						candidatePatch.variableScore = 1.0;
					}
				}
				else {
					candidatePatch.variableScore = modelExtractor.getVariableSimilarityScore(faultyNode.variableAccessed, fixingIngredient.variableAccessed);
				}
				candidatePatch.tokenScore = modelExtractor.getTokenSimilarityScore(faultyNode.tokens, fixingIngredient.tokens);
				candidatePatch.LCS = modelExtractor.getNormalizedLongestCommonSubsequence(faultyNode.node.toString(), fixingIngredient.node.toString());
				candidatePatch.score = candidatePatch.genealogyScore+candidatePatch.variableScore+candidatePatch.tokenScore; //candidatePatch.genealogyScore+candidatePatch.variableScore+
//				System.out.println("LCS " +candidatePatch.LCS);
				if(candidatePatch.score>0) {
					this.patchListUpdater.updatePatchList(candidatePatch);
				}
			}
		}
	}
}
