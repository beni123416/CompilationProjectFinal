import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ast.AstNode;
import ast.ClassDecl;
import ast.Program;

public class SymbolTable {
	// Ex1
	private Map<String, String> methodEntries;
	private Map<String, String> variableEntries;
	private SymbolTable parentSymbolTable;

	// Ex2
	public String className;
	public int vtableSize;

	private Map<String, Integer> methodOffset;
	private Map<String, String[]> methodArguments;
	// Mapping from method to return type
	private Map<String, String> methodReturnType;
	private Map<String, String> methodClass;

	private Map<String, Integer> variableOffset;
	private List<ClassDecl> classInOrder;
	private List<String> variableInOrder;
	private List<String> methodInOrder;

	public SymbolTable() {
		methodEntries = new HashMap<String, String>();
		variableEntries = new HashMap<String, String>();
		// Ex2
		methodArguments = new HashMap<String, String[]>();
		methodReturnType = new HashMap<String, String>();
		methodOffset = new HashMap<String, Integer>();
		variableOffset = new HashMap<String, Integer>();
		methodClass = new HashMap<String, String>();
		classInOrder = new ArrayList<ClassDecl>();
		variableInOrder = new ArrayList<String>();
		methodInOrder = new ArrayList<String>();

	}

	public ArrayList<String> getMethodEntriesNames() {
		ArrayList<String> methodNames = new ArrayList<String>();
		methodNames.addAll(methodEntries.keySet());
		return methodNames;
	}

	public void setParentSymbolTable(SymbolTable parentSymbolTable) {
		this.parentSymbolTable = parentSymbolTable;
	}

	public SymbolTable getParentSymbolTable() {
		return this.parentSymbolTable;
	}

	// Ex1
	// Setters
	public void addVariable(String varName, String symbol) {
		variableEntries.put(varName, symbol);
	}

	public void addMethod(String varName, String symbol) {
		methodEntries.put(varName, symbol);
	}

	// Getters
	public Map<String, String> getMethodEntries() {
		return this.methodEntries;
	}

	public Map<String, String> getVariableEntries() {
		return this.variableEntries;
	}

	// Ex2
	// Setters
	public void addMethodArguments(String method, String[] arguments) {
		methodArguments.put(method, arguments);
	}

	public void addMethodReturnType(String method, String returnType) {
		methodReturnType.put(method, returnType);
	}

	public void addMethodOffset(String method, int offset) {
		methodOffset.put(method, offset);
	}

	// Getters
	public Map<String, String[]> getMethodArguments() {
		return this.methodArguments;
	}

	public Map<String, String> getMethodReturnType() {
		return this.methodReturnType;
	}

	public Map<String, Integer> getMethodOffset() {
		return this.methodOffset;
	}

	public void addVariableOffset(String variable, int offset) {
		variableOffset.put(variable, offset);
	}

	public Map<String, Integer> getVariableOffset() {
		return this.variableOffset;
	}

	public Map<String, String> getMethodClass() {
		return this.methodClass;
	}

	public void addMethodClass(String method, String className) {
		this.methodClass.put(method, className);
	}

	public void addClassInOrder(ClassDecl classes) {
		this.classInOrder.add(classes);
	}

	public List<ClassDecl> getClassInOrder() {
		return this.classInOrder;
	}

	public void addVariableInOrder(String variable) {
		this.variableInOrder.add(variable);
	}

	public List<String> getVariableInOrder() {
		return this.variableInOrder;
	}

	public void addMethodInOrder(String method) {
		this.methodInOrder.add(method);
	}

	public List<String> getMethodInOrder() {
		return this.methodInOrder;
	}
}
