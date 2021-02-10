//import java.io.*;
//import java.util.*; 
//import java.io.FileWriter;   
//import java.io.PrintWriter;
//
//import ast.AddExpr;
//import ast.AndExpr;
//import ast.ArrayAccessExpr;
//import ast.ArrayLengthExpr;
//import ast.AssignArrayStatement;
//import ast.AssignStatement;
//import ast.AstNode;
//import ast.BinaryExpr;
//import ast.BlockStatement;
//import ast.BoolAstType;
//import ast.ClassDecl;
//import ast.Expr;
//import ast.FalseExpr;
//import ast.FormalArg;
//import ast.IdentifierExpr;
//import ast.IfStatement;
//import ast.IntArrayAstType;
//import ast.IntAstType;
//import ast.IntegerLiteralExpr;
//import ast.LtExpr;
//import ast.MainClass;
//import ast.MethodCallExpr;
//import ast.MethodDecl;
//import ast.MultExpr;
//import ast.NewIntArrayExpr;
//import ast.NewObjectExpr;
//import ast.NotExpr;
//import ast.Program;
//import ast.RefType;
//import ast.Statement;
//import ast.SubtractExpr;
//import ast.SysoutStatement;
//import ast.ThisExpr;
//import ast.TrueExpr;
//import ast.VarDecl;
//import ast.Visitor;
//import ast.WhileStatement;
//
//public class VtableVisitor implements Visitor {
//	private PrintWriter printWriter;
//	private String className;
//	private AstToSymbol root;
//
//	
//	
////@.Classes_vtable = global [1 x i8*] [i8* bitcast (i32 (i8*)* @Classes.run to i8*)]
//
//	public VtableVisitor(AstToSymbol astToSymbol, PrintWriter file) {
//		this.printWriter = file;
//	}
//	
//	boolean overrided() {
//		
//	}
//
//	@Override
//	public void visit(Program program) {
//		program.mainClass().accept(this);
//		for (ClassDecl classdecl :program.classDecls()) {
//			classdecl.accept(this);
//			this.printWriter.print("\n");
//		}
//
//		this.printWriter.println("\ndeclare i8* @calloc(i32, i32)\r\n"
//				+ "declare i32 @printf(i8*, ...)\r\n"
//				+ "declare void @exit(i32)\r\n"
//				+ "\r\n"
//				+ "@_cint = constant [4 x i8] c\"%d\\0a\\00\"\r\n"
//				+ "@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\r\n"
//				+ "define void @print_int(i32 %i) {\r\n"
//				+ "    %_str = bitcast [4 x i8]* @_cint to i8*\r\n"
//				+ "    call i32 (i8*, ...) @printf(i8* %_str, i32 %i)\r\n"
//				+ "    ret void\r\n"
//				+ "}\r\n"
//				+ "\r\n"
//				+ "define void @throw_oob() {\r\n"
//				+ "    %_str = bitcast [15 x i8]* @_cOOB to i8*\r\n"
//				+ "    call i32 (i8*, ...) @printf(i8* %_str)\r\n"
//				+ "    call void @exit(i32 1)\r\n"
//				+ "    ret void\r\n"
//				+ "}");
//
//	}
//
//	@Override
//	public void visit(ClassDecl classDecl) {		
//		int numOfMethods = classDecl.methoddecls().size();
//		this.printWriter.format("@.%s_vtable = global [%d x i8*] [", classDecl.name(), numOfMethods);
//		
//		this.className = classDecl.name();
//		
////		for (var fieldDecl : classDecl.fields()) {
////			fieldDecl.accept(this);
////		}
//		int numberOfIteartions= classDecl.methoddecls().size();
//		
//		SymbolTable classTable = root.(classDecl);
//		overried(classDecl);
//		for (var methodDecl : classDecl.methoddecls()) {
//			
//			numberOfIteartions--;	
//			methodDecl.accept(this);
//			if (numberOfIteartions == 0) {
//				this.printWriter.print("]");
//			}
//			else {
//				this.printWriter.print(", ");
//			}
//		}
//	}
//
//	@Override
//	public void visit(MainClass mainClass) {
//		mainClass.mainStatement().accept(this);
//		
//	}
//// i8* bitcast (i32 (i8*, i32)* @Base.set to i8*)
//	@Override
//	public void visit(MethodDecl methodDecl) {
//		this.printWriter.print("i8* bitcast (");
//		methodDecl.returnType().accept(this);
//		
//		this.printWriter.print(" (i8*");
//
//		for (var formal : methodDecl.formals()) {
//			this.printWriter.print(", ");
//			formal.accept(this);
//		}
//		this.printWriter.format(")* @%s.%s to i8*)", this.className, methodDecl.name());
//
////		for (var varDecl : methodDecl.vardecls()) {
////			varDecl.accept(this);
////		}
////		for (var stmt : methodDecl.body()) {
////			stmt.accept(this);
////		}
//
//		methodDecl.ret().accept(this);		
//		
//	}
//
//	@Override
//	public void visit(FormalArg formalArg) {
//		formalArg.type().accept(this);
//		
//	}
//
//	@Override
//	public void visit(VarDecl varDecl) {
//		varDecl.type().accept(this);
//		
//	}
//
//	@Override
//	public void visit(BlockStatement blockStatement) {
//		for (var s : blockStatement.statements()) {
//			s.accept(this);
//		}		
//	}
//
//	@Override
//	public void visit(IfStatement ifStatement) {
//		ifStatement.cond().accept(this);
//		ifStatement.thencase().accept(this);
//		ifStatement.elsecase().accept(this);
//	}
//
//	@Override
//	public void visit(WhileStatement whileStatement) {
//		whileStatement.cond().accept(this);
//		whileStatement.body().accept(this);
//	}
//
//	@Override
//	public void visit(SysoutStatement sysoutStatement) {
//		sysoutStatement.arg().accept(this);
//	}
//
//	@Override
//	public void visit(AssignStatement assignStatement) {
//        assignStatement.rv().accept(this);
//		
//	}
//
//	@Override
//	public void visit(AssignArrayStatement assignArrayStatement) {
//        assignArrayStatement.index().accept(this);
//        assignArrayStatement.rv().accept(this);		
//	}
//	
//	private void visitBinaryExpr(BinaryExpr e) {
//		e.e1().accept(this);
//		e.e2().accept(this);
//	}
//
//	@Override
//	public void visit(AndExpr e) {
//		visitBinaryExpr(e);
//		
//	}
//
//	@Override
//	public void visit(LtExpr e) {
//		visitBinaryExpr(e);
//		
//	}
//
//	@Override
//	public void visit(AddExpr e) {
//		visitBinaryExpr(e);
//		
//	}
//
//	@Override
//	public void visit(SubtractExpr e) {
//		visitBinaryExpr(e);
//		
//	}
//
//	@Override
//	public void visit(MultExpr e) {
//		visitBinaryExpr(e);
//		
//	}
//
//	@Override
//	public void visit(ArrayAccessExpr e) {
//        e.arrayExpr().accept(this);
//        e.indexExpr().accept(this);		
//	}
//
//	@Override
//	public void visit(ArrayLengthExpr e) {
//        e.arrayExpr().accept(this);
//		
//	}
//
//	@Override
//	public void visit(MethodCallExpr e) {
//        e.ownerExpr().accept(this);
//        for (Expr arg : e.actuals()) {
//            arg.accept(this);
//        }
//		
//	}
//
//	@Override
//	public void visit(IntegerLiteralExpr e) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void visit(TrueExpr e) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void visit(FalseExpr e) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void visit(IdentifierExpr e) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void visit(ThisExpr e) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void visit(NewIntArrayExpr e) {
//		e.lengthExpr().accept(this);
//		
//	}
//
//	@Override
//	public void visit(NewObjectExpr e) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void visit(NotExpr e) {
//		e.e().accept(this);
//		
//	}
//
//	@Override
//	public void visit(IntAstType t) {
//		this.printWriter.print("i32");
//		
//	}
//
//	@Override
//	public void visit(BoolAstType t) {
//		this.printWriter.print("i1");
//
//	}
//
//	@Override
//	public void visit(IntArrayAstType t) {
//		this.printWriter.print("i32*");
//	}
//
//	@Override
//	public void visit(RefType t) {
//		// TODO Auto-generated method stub
//		
//	}
//
//}
