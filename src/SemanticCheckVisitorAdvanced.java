import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ast.AddExpr;
import ast.AndExpr;
import ast.ArrayAccessExpr;
import ast.ArrayLengthExpr;
import ast.AssignArrayStatement;
import ast.AssignStatement;
import ast.AstNode;
import ast.BinaryExpr;
import ast.BlockStatement;
import ast.BoolAstType;
import ast.ClassDecl;
import ast.Expr;
import ast.FalseExpr;
import ast.FormalArg;
import ast.IdentifierExpr;
import ast.IfStatement;
import ast.IntArrayAstType;
import ast.IntAstType;
import ast.IntegerLiteralExpr;
import ast.LtExpr;
import ast.MainClass;
import ast.MethodCallExpr;
import ast.MethodDecl;
import ast.MultExpr;
import ast.NewIntArrayExpr;
import ast.NewObjectExpr;
import ast.NotExpr;
import ast.Program;
import ast.RefType;
import ast.Statement;
import ast.SubtractExpr;
import ast.SysoutStatement;
import ast.ThisExpr;
import ast.TrueExpr;
import ast.VarDecl;
import ast.Visitor;
import ast.WhileStatement;

public class SemanticCheckVisitorAdvanced implements Visitor {
	private AstToSymbol astToSymbol;
	private String ownerName;
	private AstNode caller;
	private PrintWriter printWriter;
	private List<ClassDecl> classesDeclrs;
	private String varType;
	private String lastExpr;
	private boolean exprFlag;
	private Map<String, Boolean> initDef;
	// private boolean isAssigning;

	public SemanticCheckVisitorAdvanced(AstToSymbol astToSymbol, PrintWriter outfile, List<ClassDecl> classDeclList) {
		this.astToSymbol = astToSymbol;
		this.printWriter = outfile;
		this.classesDeclrs = classDeclList;
	}

	private SymbolTable ExtractTable(AstNode caller) {
		SymbolTable table = astToSymbol.mapAstNodeToSymboTable.get(caller);
		return table;
	}

	@Override
	public void visit(Program program) {
		program.mainClass().accept(this);

		for (ClassDecl classdecl : program.classDecls()) {
			this.caller = program;
			classdecl.accept(this);
		}
	}

	@Override
	public void visit(ClassDecl classDecl) {

		for (var fieldDecl : classDecl.fields()) {
			this.caller = classDecl;
			fieldDecl.accept(this);
		}
		for (var methodDecl : classDecl.methoddecls()) {
			this.caller = classDecl;
			methodDecl.accept(this);
		}

	}

	@Override
	public void visit(MainClass mainClass) {
		this.caller = mainClass;
		mainClass.mainStatement().accept(this);

	}

	@Override
	public void visit(MethodDecl methodDecl) {
		methodDecl.returnType().accept(this);
		String declaredReturnType = varType;

		SymbolTable callerTable = ExtractTable(this.caller);
		callerTable = callerTable.getParentSymbolTable();

		while (callerTable != null) {
			String[] arguments = null;
			if (callerTable.getMethodEntries().containsKey(methodDecl.name())) {
				arguments = callerTable.getMethodArguments().get(methodDecl.name());
				String methodParentReturnType = callerTable.getMethodReturnType().get(methodDecl.name());
				if (!checkHierarchy(declaredReturnType, methodParentReturnType)) {
					printError(" Not a covariant static return type");
				}
				if (methodDecl.formals().size() != arguments.length) {
					printError("Not the same size of arguments");
				} else {
					int i = 0;
					// Checking if for each arg given is a subtype of the method's arugments
					for (var formal : methodDecl.formals()) {
						formal.accept(this);
						if (!this.varType.equals(arguments[i])) {
							printError("Not the same static type argument");
						}
						i++;
					}
				}
				break;
			}
			callerTable = callerTable.getParentSymbolTable();
		}
		for (var formal : methodDecl.formals()) {
			this.caller = methodDecl;
			formal.accept(this);
		}

		for (var varDecl : methodDecl.vardecls()) {
			this.caller = methodDecl;
			varDecl.accept(this);
		}

		initDef = new HashMap<String, Boolean>();
		for (var varDecl : methodDecl.vardecls()) {
			initDef.put(varDecl.name(), false);
		}

		for (var stmt : methodDecl.body()) {
			this.caller = methodDecl;
			stmt.accept(this);
		}

		this.caller = methodDecl;
		methodDecl.ret().accept(this);
		if (!varType.equals(declaredReturnType)) {
			if (varType.equals("int") || varType.equals("boolean") || varType.equals("int[]")) {
				printError(
						"The static type of e in return e is invalid according to the definition of the current method");
			} else if (checkHierarchy(varType, declaredReturnType) == false) {
				printError(
						"The static type of e in return e is invvalid according to the definition of the current method");
			}
		}
	}

	@Override
	public void visit(FormalArg formalArg) {
		formalArg.type().accept(this);

	}

	@Override
	public void visit(VarDecl varDecl) {
		varDecl.type().accept(this);

	}

	@Override
	public void visit(BlockStatement blockStatement) {
		for (var s : blockStatement.statements()) {
			s.accept(this);
		}
	}

	@Override
	public void visit(IfStatement ifStatement) {
		ifStatement.cond().accept(this);
		if (!varType.equals("boolean")) {
			printError(" 17. In if, the condition isn't boolean");
		}
		Map<String, Boolean> initDefBeforeThen = deepClone(this.initDef);

		ifStatement.thencase().accept(this);
		Map<String, Boolean> initDefAfterThen = deepClone(this.initDef);

		this.initDef = initDefBeforeThen;
		ifStatement.elsecase().accept(this);

		this.initDef = join(this.initDef, initDefAfterThen);
	}

	@Override
	public void visit(WhileStatement whileStatement) {
		whileStatement.cond().accept(this);
		if (!varType.equals("boolean")) {
			printError(" 17. In while, the condition isn't boolean");
		}
		Map<String, Boolean> initDefBeforeBody = deepClone(this.initDef);
		whileStatement.body().accept(this);

		this.initDef = join(this.initDef, initDefBeforeBody);
	}

	@Override
	public void visit(SysoutStatement sysoutStatement) {
		////
		sysoutStatement.arg().accept(this);
		if (!varType.equals("int")) {
			printError("20. The argument to System.out.println isn't of type int");
		}
	}

	@Override
	public void visit(AssignStatement assignStatement) {
		String lvName = assignStatement.lv();

		SymbolTable callerTable = ExtractTable(this.caller);
		SymbolTable table = callerTable;

		boolean isVarDeclared = false;
		while (table != null) {
			if (table.getVariableEntries().containsKey(lvName)) {
				isVarDeclared = true;
				break;
			}
			table = table.getParentSymbolTable();
		}
		if (isVarDeclared == false) {
			printError("14. A reference in an expression to a variable is to a local variable");
		}

		String lvType = LookupVarTypeName(callerTable, lvName);
		////

		assignStatement.rv().accept(this);
		if (!checkHierarchy(varType, lvType)) {
			printError("16. In an assignment x = e, the static type of e is valid according to the declaration of x");
		}
		if (this.initDef.containsKey(lvName)) {
			this.initDef.put(lvName, true);
		}
	}

	@Override
	public void visit(AssignArrayStatement assignArrayStatement) {
		String arrayName = assignArrayStatement.lv();
		SymbolTable callerTable = ExtractTable(this.caller);
		SymbolTable table = callerTable;

		boolean isVarDeclared = false;
		while (table != null) {
			if (table.getVariableEntries().containsKey(arrayName)) {
				isVarDeclared = true;
				break;
			}
			table = table.getParentSymbolTable();
		}
		if (isVarDeclared == false) {
			printError("14. A reference in an expression to a variable is to a local variable");
		}

		varType = LookupVarTypeName(callerTable, arrayName);
		if (!varType.equals("int[]")) {
			printError(" 23. In an assignment to an array x[e1] = e2, x isnt an int[]");
		}
		////
		assignArrayStatement.index().accept(this);
		if (!varType.equals("int")) {
			printError(" 23. In an assignment to an array x[e1] = e2, e1 isnt an int");
		}
		/////
		assignArrayStatement.rv().accept(this);
		if (!varType.equals("int")) {
			printError(" 23. In an assignment to an array x[e1] = e2, e2 isnt an int");

		}
	}

	private void visitBinaryExpr(BinaryExpr e) {
		e.e1().accept(this);
		e.e2().accept(this);
	}

	@Override
	public void visit(AndExpr e) {
		this.exprFlag = true;
		///
		e.e1().accept(this);
		if (!varType.equals("boolean")) {
			printError("e1 isn't a boolean in an And opreator");
		}
		////
		this.exprFlag = true;
		e.e2().accept(this);
		if (!varType.equals("boolean")) {
			printError("e2 isn't a boolean in an And opreator");
		}
		varType = "boolean";
		this.lastExpr = "AndExpr";
	}

	@Override
	public void visit(LtExpr e) {
		this.exprFlag = true;
		///
		e.e1().accept(this);
		if (!varType.equals("int")) {
			printError("e1 isn't an int in a LT opreator");
		}
		///
		this.exprFlag = true;
		e.e2().accept(this);
		if (!varType.equals("int")) {
			printError("e2 isn't an int in a LT opreator");
		}

		varType = "boolean";
		this.lastExpr = "LtExpr";

	}

	@Override
	public void visit(AddExpr e) {
		this.exprFlag = true;
		///
		e.e1().accept(this);
		if (!varType.equals("int")) {
			printError("e1 isn't an int in a Add opreator");
		}
		////
		this.exprFlag = true;
		e.e2().accept(this);
		if (!varType.equals("int")) {
			printError("e2 isn't an int in a add opreator");
		}

		varType = "int";
		this.lastExpr = "AddExpr";

	}

	@Override
	public void visit(SubtractExpr e) {
		this.exprFlag = true;
		////
		e.e1().accept(this);
		if (!varType.equals("int")) {
			printError("e1 isn't an int in a sub opreator");
		}
		this.exprFlag = true;
		////
		e.e2().accept(this);
		if (!varType.equals("int")) {
			printError("e2 isn't an int in a sub opreator");
		}

		varType = "int";
		this.lastExpr = "SubtractExpr";

	}

	@Override
	public void visit(MultExpr e) {
		this.exprFlag = true;
		////
		e.e1().accept(this);
		if (!varType.equals("int")) {
			printError("e1 isn't an int in a mult opreator");
		}
		this.exprFlag = true;
		////
		e.e2().accept(this);
		if (!varType.equals("int")) {
			printError("e2 isn't an int in a mult opreator");
		}

		varType = "int";
		this.lastExpr = "MultExpr";

	}

	@Override
	public void visit(ArrayAccessExpr e) {
		this.exprFlag = true;
		e.arrayExpr().accept(this);
		if (!varType.equals("int[]")) {
			printError(" 22. In an array access x[e], x isn't int[]");
		}
		if (!isInit(this.ownerName)) {
			printError("ArrayAccessExpr not init");
		}
		this.exprFlag = true;
		////
		e.indexExpr().accept(this);
		if (!varType.equals("int")) {
			printError(" 22. In an array access x[e], e isn't an int");
		}
		this.lastExpr = "ArrayAccessExpr";
		varType = "int";

	}

	@Override
	public void visit(ArrayLengthExpr e) {
		this.exprFlag = true;
		e.arrayExpr().accept(this);
		if (!this.varType.equals("int[]")) {
			printError("The static type of the object on which length invoked is not int[].");
		}
		if (!isInit(this.ownerName)) {
			printError("ArrayLengthExpr not init");
		}
		this.lastExpr = "ArrayLengthExpr";
		varType = "int";
	}

	@Override
	public void visit(MethodCallExpr e) {
		this.exprFlag = false;
		e.ownerExpr().accept(this);
		if (!this.lastExpr.equals("NewObjectExpr") && !this.lastExpr.equals("ThisExpr") && !isInit(this.ownerName)) {
			printError("MethodCallExpr owner not init");
		}
		if (!(this.lastExpr.equals("NewObjectExpr") || this.lastExpr.equals("ThisExpr")
				|| this.lastExpr.equals("IdentifierExpr"))) {
			printError(
					"A method call isnt invoked on an owner expression e which is either this, a new expression, or a reference to a local variable, formal parameter or a field.");
		}
		String ownerType = "";

		if (this.lastExpr.equals("NewObjectExpr") || this.lastExpr.equals("ThisExpr")) {
			ownerType = varType;
		} else {
			SymbolTable table = ExtractTable(this.caller);

			while (table != null) {
				if (table.getVariableEntries().containsKey(this.ownerName)) {
					ownerType = table.getVariableEntries().get(this.ownerName);
					if (ownerType.equals("int") || ownerType.equals("int[]") || ownerType.equals("boolean")) {
						printError(
								"In method invocation, the static type of the object is a reference type (not int, bool, or int[]).");
					}
					break;
				} else {
					table = table.getParentSymbolTable();
				}
			}
		}

		// Find variable class symbol table
		SymbolTable classTable = null;
		for (ClassDecl classDecl : this.classesDeclrs) {
			if (classDecl.name().equals(ownerType)) {
				classTable = ExtractTable(classDecl);
				break;
			}
		}

		// Check if the method is actually defined in that class symbol table, otherwise
		// go to the parent table
		while (classTable != null) {
			if (classTable.getMethodEntries().containsKey(e.methodId())) {
				String[] arguments = classTable.getMethodArguments().get(e.methodId());
				if (e.actuals().size() != arguments.length) {
					printError("Not the same size of arguments");
				} else {
					int i = 0;
					// Checking if for each arg given is a subtype of the method's arugments
					for (Expr arg : e.actuals()) {
						this.exprFlag = true;
						arg.accept(this);
						if (!checkHierarchy(this.varType, arguments[i])) {
							printError("Not subtype of argument");
						}
						i++;
					}
					varType = classTable.getMethodReturnType().get(e.methodId());
					break;
				}
			}
			classTable = classTable.getParentSymbolTable();
		}
		if (classTable == null) {
			printError("MethodCallExpr method is not defiend");
		}

		this.lastExpr = "MethodCallExpr";
	}

	@Override
	public void visit(IntegerLiteralExpr e) {
		varType = "int";
		this.lastExpr = "IntegerLiteralExpr";

	}

	@Override
	public void visit(TrueExpr e) {
		varType = "boolean";
		this.lastExpr = "TrueExpr";

	}

	@Override
	public void visit(FalseExpr e) {
		varType = "boolean";
		this.lastExpr = "FalseExpr";

	}

	@Override
	public void visit(IdentifierExpr e) {

		if (this.exprFlag == true) {
			boolean isVarDeclared = false;
			SymbolTable table = ExtractTable(this.caller);
			while (table != null) {
				if (table.getVariableEntries().containsKey(e.id())) {
					isVarDeclared = true;
					break;
				}
				table = table.getParentSymbolTable();
			}
			if (isVarDeclared == false) {
				printError("14. A reference in an expression to a variable is to a local variable");
			}
		}
		this.ownerName = e.id();
		if (!isInit(this.ownerName)) {
			printError("IdentifierExpr not init");
		}
		this.varType = LookupVarTypeName(ExtractTable(this.caller), e.id());
		this.lastExpr = "IdentifierExpr";
	}

	@Override
	public void visit(ThisExpr e) {
		SymbolTable table = ExtractTable(this.caller);
		if (!(this.caller.getClass() == ast.ClassDecl.class)) {
			table = table.getParentSymbolTable();
		}
		this.varType = table.className;

		this.lastExpr = "ThisExpr";
	}

	@Override
	public void visit(NewIntArrayExpr e) {
		this.exprFlag = true;
		e.lengthExpr().accept(this);
		if (!varType.equals("int")) {
			printError("In an array allocation new int[e], e is an int");
		}
		this.lastExpr = "NewIntArrayExpr";
		varType = "int[]";

	}

	@Override
	public void visit(NewObjectExpr e) {
		this.lastExpr = "NewObjectExpr";
		this.exprFlag = false;
		varType = e.classId();
		this.ownerName = e.classId();
	}

	@Override
	public void visit(NotExpr e) {
		this.exprFlag = true;
		e.e().accept(this);
		if (!varType.equals("boolean")) {
			printError("e isn't a boolean in not opreator");
		}
		this.lastExpr = "NotExpr";
		varType = "boolean";
	}

	@Override
	public void visit(IntAstType t) {
		varType = "int";
	}

	@Override
	public void visit(BoolAstType t) {
		varType = "boolean";
	}

	@Override
	public void visit(IntArrayAstType t) {
		varType = "int[]";
	}

	@Override
	public void visit(RefType t) {
		varType = t.id();
	}

	private String LookupVarTypeName(SymbolTable table, String name) {
		while (table != null) {
			if (table.getVariableEntries().containsKey(name)) {
				return table.getVariableEntries().get(name);
			}
			table = table.getParentSymbolTable();
		}
		return null;
	}

	private boolean checkHierarchy(String lowerArg, String upperArg) {
		// System.out.println("The lowerArg is " + lowerArg);
		// System.out.println("The upperArg is " + upperArg);

		if (lowerArg.equals(upperArg)) {
			return true;
		}
		List<String> primTypes = Arrays.asList("int", "int[]", "boolean");

		if (primTypes.contains(lowerArg) || primTypes.contains(upperArg)) {
			return false;
		}

		SymbolTable table = null;
		for (ClassDecl classDecl : this.classesDeclrs) {
			if (classDecl.name().equals(lowerArg)) {
				table = ExtractTable(classDecl);
			}
		}
		while (table != null) {
			if (table.className != null && table.className.equals(upperArg)) {
				return true;
			}
			table = table.getParentSymbolTable();
		}
		return false;
	}

	private void printError(String error) {
		throw new RuntimeException(error);
	}

	private Map<String, Boolean> join(Map<String, Boolean> initDef1, Map<String, Boolean> initDef2) {
		Map<String, Boolean> retInitDef = new HashMap<String, Boolean>();
		if (initDef1 == null || initDef2 == null) {
			return null;
		}
		for (String key1 : initDef1.keySet()) {
			if (!initDef1.get(key1) || !initDef2.get(key1)) {
				retInitDef.put(key1, false);
			} else {
				retInitDef.put(key1, true);
			}
		}
		return retInitDef;
	}

	private Map<String, Boolean> deepClone(Map<String, Boolean> initDef2) {
		Map<String, Boolean> deepcopied = new HashMap<String, Boolean>();
		if (initDef2 == null) {
			return null;
		}
		for (String key : initDef2.keySet()) {
			deepcopied.put(key, initDef2.get(key));
		}
		return deepcopied;
	}

	private boolean isInit(String varName) {
		if (!this.initDef.containsKey(varName)) {
			return true;
		}
		return this.initDef.get(varName);
	}
}
