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
import ast.SubtractExpr;
import ast.SysoutStatement;
import ast.ThisExpr;
import ast.TrueExpr;
import ast.VarDecl;
import ast.Visitor;
import ast.WhileStatement;

public class AstDeclRetriver implements Visitor {
	private AstToSymbol astToSymbol;
	private AstNode caller;
	private SymbolTable varDeclTable;
	private boolean isMethod;
	private String originalName;
	private String originalLine;

	public AstDeclRetriver(AstToSymbol astToSymbol, boolean isMethod, String originalName, String originalLine) {
		this.astToSymbol = astToSymbol;
		this.isMethod = isMethod;
		this.originalName = originalName;
		this.originalLine = originalLine;
	}
	public SymbolTable getVarDeclTable() {
		return this.varDeclTable;
	}

	private boolean checkEquals(int lineNumber, String name) {
		return (lineNumber == Integer.parseInt(originalLine) && name.equals(originalName));
	}

	private SymbolTable ExtractTable(AstNode caller) {
		SymbolTable table = astToSymbol.mapAstNodeToSymboTable.get(caller);
		return table;
	}

	@Override
	public void visit(Program program) {
		program.mainClass().accept(this);
		for (ClassDecl classdecl : program.classDecls()) {
			classdecl.accept(this);
		}
	}

	@Override
	public void visit(ClassDecl classDecl) {
		if (isMethod == true) {
			for (var methodDecl : classDecl.methoddecls()) {
				this.caller = classDecl;
				methodDecl.accept(this);
			}
		} else {
			for (var fieldDecl : classDecl.fields()) {
				this.caller = classDecl;
				fieldDecl.accept(this);
			}
			for (var methodDecl : classDecl.methoddecls()) {
				this.caller = classDecl;
				methodDecl.accept(this);
			}
		}
	}

	@Override
	public void visit(MainClass mainClass) {
		mainClass.mainStatement().accept(this);
	}

	@Override
	public void visit(MethodDecl methodDecl) {
		methodDecl.returnType().accept(this);
		if (isMethod == true) {
			if (checkEquals(methodDecl.lineNumber, methodDecl.name())) {
				this.varDeclTable = ExtractTable(this.caller);
			}
		}
		else {
			for (var formal : methodDecl.formals()) {
				this.caller = methodDecl;
				formal.accept(this);
			}

			for (var varDecl : methodDecl.vardecls()) {
				this.caller = methodDecl;
				varDecl.accept(this);
			}
			for (var stmt : methodDecl.body()) {
				stmt.accept(this);
			}

			methodDecl.ret().accept(this);
		}

	}

	@Override
	public void visit(FormalArg formalArg) {
		formalArg.type().accept(this);
		if (isMethod == false && checkEquals(formalArg.lineNumber, formalArg.name())) {
			this.varDeclTable = ExtractTable(this.caller);
		}
	}

	@Override
	public void visit(VarDecl varDecl) {
		varDecl.type().accept(this);
		if (isMethod == false && checkEquals(varDecl.lineNumber, varDecl.name())) {
			this.varDeclTable = ExtractTable(this.caller);
		}
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
		ifStatement.thencase().accept(this);
		ifStatement.elsecase().accept(this);
	}

	@Override
	public void visit(WhileStatement whileStatement) {
		whileStatement.cond().accept(this);
		whileStatement.body().accept(this);

	}

	@Override
	public void visit(SysoutStatement sysoutStatement) {
		sysoutStatement.arg().accept(this);
	}

	@Override
	public void visit(AssignStatement assignStatement) {

		assignStatement.rv().accept(this);
	}

	@Override
	public void visit(AssignArrayStatement assignArrayStatement) {
		assignArrayStatement.index().accept(this);
		assignArrayStatement.rv().accept(this);
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
		e.arrayExpr().accept(this);
		e.indexExpr().accept(this);
	}

	@Override
	public void visit(ArrayLengthExpr e) {
		e.arrayExpr().accept(this);
	}

	@Override
	public void visit(MethodCallExpr e) {
		e.ownerExpr().accept(this);

		for (Expr arg : e.actuals()) {
			arg.accept(this);
		}
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
		e.lengthExpr().accept(this);
	}

	@Override
	public void visit(NewObjectExpr e) {
	}

	@Override
	public void visit(NotExpr e) {
		e.e().accept(this);
	}

	@Override
	public void visit(IntAstType t) {
	}

	@Override
	public void visit(BoolAstType t) {
	}

	@Override
	public void visit(IntArrayAstType t) {
	}

	@Override
	public void visit(RefType t) {
	}
}
