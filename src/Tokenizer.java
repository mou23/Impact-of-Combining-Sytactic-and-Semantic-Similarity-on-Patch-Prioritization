import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;

public class Tokenizer {
	private static Tokenizer tokenizer;
	static ArrayList<StringTokenPair> stringTokenPairList = new ArrayList<StringTokenPair>();
	String[] seperators = {"(", ")", "{", "}", "[", "]", ";", ",", ".", "...", "@", "::"};
	List<String> seperatorList;
	
	private Tokenizer() {
		this.seperatorList = (List<String>) Arrays.asList(seperators);
	}
	
	public static Tokenizer createTokenizer() {
		if(tokenizer == null){
			tokenizer = new Tokenizer();
		}

		return tokenizer;
	}
	
	HashMap<String,Integer> tokenize(String expression) {
		HashMap<String,Integer> tokens = new HashMap<String,Integer>();  
		try {
//			System.out.println("Tokenizing "+expression);
			for(int i=0; i<stringTokenPairList.size(); i++) {
				StringTokenPair currentPair = stringTokenPairList.get(i);
				if(currentPair.text.equals(expression)) {
//					System.out.println("EXISTS");
					return currentPair.tokens;
				}
			}
//			FileWriter fileWrite = new FileWriter("out.txt");
//			
//			fileWrite.write();
//			fileWrite.close();
//					
			ProcessBuilder processBuilder = new ProcessBuilder("python","tokenizer.py",expression);
			Process process = processBuilder.start();

			BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String token;
			
			while((token = in.readLine())!=null) {
				if(token.length()==0) {
					break;
				}
				else {
					if(tokens.containsKey(token)) {
						tokens.put(token, tokens.get(token)+1);
					}
					else {
						tokens.put(token, 1);
					}
				}	
			}
			
//			System.out.println(lineWiseTokenList.size());
//			
//			for (int i=0;i<lineWiseTokenList.size();i++) {
//				System.out.println(lineWiseTokenList.get(i));
//			}
			
//			System.out.println("\n\n");
			
//			String []tokens = list.split(",");
//			
//			NodeTokens nodeTokens = new NodeTokens();
//			for (int i=0; i<tokens.length;i++) {
//				if(nodeTokens.tokens.containsKey(tokens[i])) {
//					nodeTokens.tokens.put(tokens[i], nodeTokens.tokens.get(tokens[i])+1);
//				}
//				else {
//					nodeTokens.tokens.put(tokens[i], 1);
//				}
//			}
//			nodeWiseTokenList.add(nodeTokens);
			
			
		} catch(Exception e){
			System.out.println(e.getMessage());
		}
//		System.out.println(tokens);
		StringTokenPair stringTokenPair = new StringTokenPair();
		stringTokenPair.text = expression;
		stringTokenPair.tokens = tokens;
		stringTokenPairList.add(stringTokenPair);
		
		return tokens;
	}
}
