import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ast.AstNode;
import ast.ClassDecl;

public class AstToSymbol {

	public Map<AstNode, SymbolTable> mapAstNodeToSymboTable;
	public SymbolTable progSymbolTable;

	public AstToSymbol() {
		this.mapAstNodeToSymboTable = new HashMap<AstNode, SymbolTable>();
		this.progSymbolTable = new SymbolTable();
	}

	public void enterAstNodeToSymbolMap(AstNode key, SymbolTable value) {
		this.mapAstNodeToSymboTable.put(key, value);
	}
	
	public ClassDecl retriveAst(String name){
		 ArrayList<AstNode>  astNodes = new ArrayList<AstNode>();
		 astNodes.addAll(mapAstNodeToSymboTable.keySet());
		 for(AstNode ast : astNodes) {
			 if(ast.getClass() == ClassDecl.class) {
				ClassDecl classAstNode = (ClassDecl)ast;
				 if(classAstNode.name().equals(name)) {
					 return classAstNode;
				 }
			 }
		 }
		 return null;
	}
}
