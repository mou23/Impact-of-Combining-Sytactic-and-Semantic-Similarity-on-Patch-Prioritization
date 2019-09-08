import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.jdt.core.dom.ASTNode;

public class Node {
	ASTNode  node;
	int startLine;
	int endLine;
	String type;
	HashMap<Integer,Integer> genealogy;
	HashSet<Variable> variableAccessed;
	HashMap<String,Integer> tokens;
	String context;
	@Override
	public String toString() {
		return "Node [node=" + node + ", startLine=" + startLine + ", endLine=" + endLine + ", type=" + type + "]";
	}
}
