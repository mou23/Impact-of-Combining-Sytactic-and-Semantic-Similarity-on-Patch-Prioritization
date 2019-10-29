import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

public class Variable {
	String name;
	ITypeBinding type;
	IVariableBinding binding;
	int startLine;
	int endLine;
	
	@Override
	public String toString() {
		return "Variable [name=" + name + ", type=" + type + ", binding=" + binding + ", startLine=" + startLine
				+ ", endLine=" + endLine + "]";
	}
}
