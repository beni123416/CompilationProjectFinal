
import java.io.*;

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

public class AstRenameVisitor implements Visitor {
	private AstToSymbol astToSymbol;
	private AstNode caller;
	private SymbolTable DeclSymbolTable;
	private boolean isMethod;
	private String originalName;
	private String originalLine;
	private String newName;

	public AstRenameVisitor(AstToSymbol astToSymbol, SymbolTable declTable, boolean isMethod, String originalName,
			String originalLine, String newName) {
		this.astToSymbol = astToSymbol;
		this.isMethod = isMethod;
		this.DeclSymbolTable = declTable;
		this.originalName = originalName;
		this.originalLine = originalLine;
		this.newName = newName;
	}

	private boolean checkEquals(int lineNumber, String name) {
		return (lineNumber == Integer.parseInt(originalLine) && name.equals(originalName));
	}

	private SymbolTable ExtractTable(AstNode nodeCaller) {
		SymbolTable table = astToSymbol.mapAstNodeToSymboTable.get(nodeCaller);
		return table;
	}

	private boolean Lookup(SymbolTable table) {
		while (table != null) {
			if (table == this.DeclSymbolTable) {
				return true;
			} else {
				if (table.getVariableEntries().get(this.originalName) != null) {
					return false;
				}
			}
			table = table.getParentSymbolTable();
		}
		return false;
	}

	private String LookupVarClassName(SymbolTable table, String name) {
		while (table != null) {
			if (table.getVariableEntries().containsKey(name) ) {
				return table.getVariableEntries().get(name);
			}
			table = table.getParentSymbolTable();
		}
		return null;
	}

	private void visitBinaryExpr(BinaryExpr e, String infixSymbol) {
		e.e1().accept(this);
		e.e2().accept(this);
	}

	@Override
	public void visit(Program program) {
		program.mainClass().accept(this);
		for (ClassDecl classdecl : program.classDecls()) {
			classdecl.accept(this);
		}
	}

	@Override
	public void visit(MainClass mainClass) {
		mainClass.mainStatement().accept(this);
	}

	@Override
	public void visit(ClassDecl classDecl) {
		if (isMethod == true) {
			for (var methodDecl : classDecl.methoddecls()) {
				if (methodDecl.name().equals(originalName)) {
					if (LookUpMethodClassDecl(ExtractTable(methodDecl))) {
						methodDecl.setName(newName);
					}
				}
				this.caller = classDecl;
				methodDecl.accept(this);
			}

			for (var fieldDecl : classDecl.fields()) {
				this.caller = classDecl;
				fieldDecl.accept(this);
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

	private boolean LookUpMethodClassDecl(SymbolTable CurrMethodSymbolTable) {
		while (CurrMethodSymbolTable != null) {
			if (CurrMethodSymbolTable == this.DeclSymbolTable) {
				return true;
			}
			CurrMethodSymbolTable = CurrMethodSymbolTable.getParentSymbolTable();
		}
		return false;
	}

	@Override
	public void visit(MethodDecl methodDecl) {
		methodDecl.returnType().accept(this);

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

	@Override
	public void visit(FormalArg formalArg) {
		if (isMethod == false) {
			formalArg.type().accept(this);
			if (formalArg.name().equals(this.originalName)) {
				if (Lookup(ExtractTable(this.caller)) == true) {
					formalArg.setName(this.newName);
				}
			}
		}
	}

	@Override
	public void visit(VarDecl varDecl) {
		if (isMethod == false) {
			varDecl.type().accept(this);
			if (varDecl.name().equals(this.originalName)) {
				if (Lookup(ExtractTable(this.caller)) == true) {
					varDecl.setName(this.newName);
				}
			}
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
		if (isMethod == false) {
			if (assignStatement.lv().equals(this.originalName)) {
				if (Lookup(ExtractTable(this.caller)) == true) {
					assignStatement.setLv(this.newName);
				}
			}
		}
		assignStatement.rv().accept(this);
	}

	@Override
	public void visit(AssignArrayStatement assignArrayStatement) {
		if (isMethod == false) {
			if (assignArrayStatement.lv().equals(this.originalName)) {
				if (Lookup(ExtractTable(this.caller)) == true) {
					assignArrayStatement.setLv(this.newName);
				}
			}
		}
		assignArrayStatement.index().accept(this);
		assignArrayStatement.rv().accept(this);
	}

	private void visitBinaryExpr(BinaryExpr e) {
		e.e1().accept(this);
		e.e2().accept(this);
	}

	@Override
	public void visit(AndExpr e) {
		visitBinaryExpr(e);

	}

	@Override
	public void visit(LtExpr e) {
		visitBinaryExpr(e);
	}

	@Override
	public void visit(AddExpr e) {
		visitBinaryExpr(e);
	}

	@Override
	public void visit(SubtractExpr e) {
		visitBinaryExpr(e);
	}

	@Override
	public void visit(MultExpr e) {
		visitBinaryExpr(e);
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
		if (isMethod == true) {
			if (e.ownerExpr().getClass() == ThisExpr.class) {
				if (LookUpMethodClassDecl(ExtractTable(this.caller)) == true) {
					e.setMethodId(newName);
				}
			} else if (e.ownerExpr().getClass() == NewObjectExpr.class) {
				NewObjectExpr newObjOwner = (NewObjectExpr) e.ownerExpr();
				if (LookUpMethodClassDecl(ExtractTable(astToSymbol.retriveAst(newObjOwner.classId()))) == true) {
					e.setMethodId(newName);
				}

			} else if (e.ownerExpr().getClass() == IdentifierExpr.class) {
				IdentifierExpr idenExpr = (IdentifierExpr) e.ownerExpr();
				String classType = LookupVarClassName(ExtractTable(this.caller), idenExpr.id());
				if (LookUpMethodClassDecl(ExtractTable(astToSymbol.retriveAst(classType))) == true) {
					e.setMethodId(newName);
				}
			}
		}
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
		if (isMethod == false) {
			if (e.id().equals(this.originalName)) {
				if (Lookup(ExtractTable(this.caller)) == true) {
					e.setId(this.newName);
				}
			}
		}

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
		if (t.id().equals(this.originalName)) {
			if (Lookup(ExtractTable(this.caller)) == true) {
				t.setId(this.newName);
			}
		}
	}
}
