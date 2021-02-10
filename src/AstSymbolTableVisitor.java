import ast.AddExpr;
import ast.AndExpr;
import ast.ArrayAccessExpr;
import ast.ArrayLengthExpr;
import ast.AssignArrayStatement;
import ast.AssignStatement;
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
import ast.SubtractExpr;
import ast.SysoutStatement;
import ast.ThisExpr;
import ast.TrueExpr;
import ast.VarDecl;
import ast.Visitor;
import ast.WhileStatement;

public class AstSymbolTableVisitor implements Visitor {
	private SymbolTable callerSymbolTable;
	public AstToSymbol res;
	private String varType;

	public AstSymbolTableVisitor() {
	}

	private void visitBinaryExpr(BinaryExpr e, String infixSymbol) {
	}

	@Override
	public void visit(Program program) {
		this.res = new AstToSymbol();
		this.callerSymbolTable = res.progSymbolTable;
		this.res.enterAstNodeToSymbolMap(program, res.progSymbolTable);

		program.mainClass().accept(this);

		for (ClassDecl classdecl : program.classDecls()) {
			this.callerSymbolTable = res.progSymbolTable;
			res.progSymbolTable.addClassInOrder(classdecl);
			classdecl.accept(this);
		}
		
	}

	@Override
	public void visit(ClassDecl classDecl) {
		SymbolTable classDeclTable = new SymbolTable();
		classDeclTable.className = classDecl.name();
		classDeclTable.setParentSymbolTable(this.callerSymbolTable);
		this.callerSymbolTable = classDeclTable;
		// Update mapping
		this.res.enterAstNodeToSymbolMap(classDecl, classDeclTable);

		for (var fieldDecl : classDecl.fields()) {
			fieldDecl.type().accept(this);
			classDeclTable.addVariableInOrder(fieldDecl.name());
			classDeclTable.addVariable(fieldDecl.name(), varType);
		}
		int i = 0;
		for (var methodDecl : classDecl.methoddecls()) {
			methodDecl.returnType().accept(this);
			classDeclTable.addMethod(methodDecl.name(), varType);
			classDeclTable.addMethodInOrder(methodDecl.name());
			classDeclTable.addMethodOffset(methodDecl.name(), i);
			i++;
			this.callerSymbolTable = classDeclTable;
			methodDecl.accept(this);
		}
	}

	@Override
	public void visit(MainClass mainClass) {
		SymbolTable mainSymbolTable = new SymbolTable();
		mainSymbolTable.setParentSymbolTable(this.callerSymbolTable);
		this.callerSymbolTable = mainSymbolTable;
		this.res.enterAstNodeToSymbolMap(mainClass, mainSymbolTable);
	}

	@Override
	public void visit(MethodDecl methodDecl) {
		// Ex1
		SymbolTable methodSymbolTable = new SymbolTable();
		methodSymbolTable.setParentSymbolTable(this.callerSymbolTable);
		SymbolTable classCaller = this.callerSymbolTable;
		this.callerSymbolTable = methodSymbolTable;
		this.res.enterAstNodeToSymbolMap(methodDecl, methodSymbolTable);
		
		// Ex2 
		methodDecl.returnType().accept(this);
		classCaller.addMethodReturnType(methodDecl.name(), varType);
		
		String[] Arguments = new String[methodDecl.formals().size()];
		int i = 0;
		
		for (var formal : methodDecl.formals()) {
			
			formal.type().accept(this);
			Arguments[i] = varType;
			methodSymbolTable.addVariable(formal.name(), varType);
			i++;
		}
		// Ex2 
		classCaller.addMethodArguments(methodDecl.name(), Arguments);

		for (var varDecl : methodDecl.vardecls()) {
			varDecl.accept(this);
			methodSymbolTable.addVariable(varDecl.name(),varType);
		}
				

	}

	@Override
	public void visit(FormalArg formalArg) {
	}

	@Override
	public void visit(VarDecl varDecl) {
		varDecl.type().accept(this);
	}

	@Override
	public void visit(BlockStatement blockStatement) {
	}

	@Override
	public void visit(IfStatement ifStatement) {
	}

	@Override
	public void visit(WhileStatement whileStatement) {
	}

	@Override
	public void visit(SysoutStatement sysoutStatement) {
	}

	@Override
	public void visit(AssignStatement assignStatement) {
	}

	@Override
	public void visit(AssignArrayStatement assignArrayStatement) {
	}

	@Override
	public void visit(AndExpr e) {
	}

	@Override
	public void visit(LtExpr e) {
	}

	@Override
	public void visit(AddExpr e) {
	}

	@Override
	public void visit(SubtractExpr e) {
	}

	@Override
	public void visit(MultExpr e) {
	}

	@Override
	public void visit(ArrayAccessExpr e) {
	}

	@Override
	public void visit(ArrayLengthExpr e) {
	}

	@Override
	public void visit(MethodCallExpr e) {
	}

	@Override
	public void visit(IntegerLiteralExpr e) {
	}

	@Override
	public void visit(TrueExpr e) {
	}

	@Override
	public void visit(FalseExpr e) {
	}

	@Override
	public void visit(IdentifierExpr e) {
	}

	public void visit(ThisExpr e) {
	}

	@Override
	public void visit(NewIntArrayExpr e) {
	}

	@Override
	public void visit(NewObjectExpr e) {
	}

	@Override
	public void visit(NotExpr e) {
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
	
}
