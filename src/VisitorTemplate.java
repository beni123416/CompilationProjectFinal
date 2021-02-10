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

public class VisitorTemplate implements Visitor {
	
	public VisitorTemplate() {
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
		for (var methodDecl : classDecl.methoddecls()) {
			methodDecl.accept(this);
		}
		
		
		for (var fieldDecl : classDecl.fields()) {
			fieldDecl.accept(this);
		}
		for (var methodDecl : classDecl.methoddecls()) {
			methodDecl.accept(this);
		}
		
	}

	@Override
	public void visit(MainClass mainClass) {
		mainClass.mainStatement().accept(this);
		
	}

	@Override
	public void visit(MethodDecl methodDecl) {
		methodDecl.returnType().accept(this);

		for (var formal : methodDecl.formals()) {
			formal.accept(this);
		}

		for (var varDecl : methodDecl.vardecls()) {
			varDecl.accept(this);
		}
		for (var stmt : methodDecl.body()) {
			stmt.accept(this);
		}

		methodDecl.ret().accept(this);		
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
        e.ownerExpr().accept(this);
        for (Expr arg : e.actuals()) {
            arg.accept(this);
        }
		
	}

	@Override
	public void visit(IntegerLiteralExpr e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(TrueExpr e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(FalseExpr e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(IdentifierExpr e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ThisExpr e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(NewIntArrayExpr e) {
		e.lengthExpr().accept(this);
		
	}

	@Override
	public void visit(NewObjectExpr e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(NotExpr e) {
		e.e().accept(this);
		
	}

	@Override
	public void visit(IntAstType t) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(BoolAstType t) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(IntArrayAstType t) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(RefType t) {
		// TODO Auto-generated method stub
		
	}

}
