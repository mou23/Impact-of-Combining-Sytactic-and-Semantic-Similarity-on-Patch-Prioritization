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
//				System.out.println("FAULT "+ faultyNode.node.toString() + " line:"+faultyNode.startLine);
//				System.out.println("FIX " + fixingIngredient.node.toString()+ " line:"+fixingIngredient.startLine);
				CandidatePatch candidatePatch = new CandidatePatch();
				candidatePatch.faultyNode = faultyNode.node;
				candidatePatch.fixingIngredient = fixingIngredient.node;
		
				candidatePatch.mutationOperation = "replace";
				
				this.patchListUpdater.updatePatchList(candidatePatch);
			}
		}
	}
}
