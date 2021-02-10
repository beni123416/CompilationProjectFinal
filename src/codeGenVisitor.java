import java.io.PrintWriter;
import java.util.ArrayList;
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
import ast.SubtractExpr;
import ast.SysoutStatement;
import ast.ThisExpr;
import ast.TrueExpr;
import ast.VarDecl;
import ast.Visitor;
import ast.WhileStatement;

public class codeGenVisitor implements Visitor {
	private PrintWriter printWriter;
	Map<AstNode, SymbolTable> astMapping;
	private int registerCounter;
	private int ifLabelCounter;
	String varType;
	Map<String, Integer> methodVarsToRegister;
	private AstNode caller;
	String resultTemporalRegister;

	public codeGenVisitor(PrintWriter outfile, AstToSymbol astToSymbol) {
		this.printWriter = outfile;
		this.astMapping = astToSymbol.mapAstNodeToSymboTable;
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
	public void visit(ClassDecl classDecl) {

		this.caller = classDecl;
		for (var fieldDecl : classDecl.fields()) {
			fieldDecl.accept(this);

		}
		for (var methodDecl : classDecl.methoddecls()) {
			this.caller = classDecl;
			this.registerCounter = 0;
			this.ifLabelCounter = 0;
			methodVarsToRegister = new HashMap<String, Integer>();
			methodDecl.accept(this);

		}

	}

	@Override
	public void visit(MainClass mainClass) {
		this.printWriter.format("define i32 @main() {\n");
		mainClass.mainStatement().accept(this);
				
		this.printWriter.format("\tret i32 0\n");
		this.printWriter.format("}\n");
	}

	
	@Override
	public void visit(MethodDecl methodDecl) {
		SymbolTable currentSymbolTable = astMapping.get(caller);
		String returnType = currentSymbolTable.getMethodReturnType().get(methodDecl.name());
		String className = ((ClassDecl) this.caller).name();
		this.printWriter.format("define %s @%s.%s(i8* %%this", returnTypeLLVM(returnType), className, methodDecl.name());

		int numberOfArgs = currentSymbolTable.getMethodArguments().get(methodDecl.name()).length;
		for (var formal : methodDecl.formals()) {
			formal.accept(this);
			this.printWriter.format(", %s %%.%s", returnTypeLLVM(varType), formal.name());
		}
		this.printWriter.format(") {\n");

		for (var formal : methodDecl.formals()) {
			this.caller = methodDecl;
			formal.accept(this);
			this.printWriter.format("\t%%%s = alloca %s \n", formal.name(),returnTypeLLVM(varType));
			this.printWriter.format("\tstore %s %%.%s, %s*  %%%s\n",returnTypeLLVM(varType), formal.name(), returnTypeLLVM(varType), formal.name());
		}

		for (var varDecl : methodDecl.vardecls()) {
			this.caller = methodDecl;
			varDecl.accept(this);
			this.printWriter.format("\t%%%s = alloca %s \n", varDecl.name(), returnTypeLLVM(varType));
		}
		for (var stmt : methodDecl.body()) {
			this.caller = methodDecl;
			stmt.accept(this);
		}

		methodDecl.ret().accept(this);

		this.printWriter.format("\tret %s %s\n", returnTypeLLVM(returnType), resultTemporalRegister);
		this.printWriter.println("}");

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
		this.printWriter.format("\tbr i1 %s, label %%if%d, label %%if%d\n", resultTemporalRegister, ifLabelCounter++,
				ifLabelCounter++);
		int firstLabelOfIf = ifLabelCounter - 2;
		ifLabelCounter++;
		this.printWriter.format("if%d:\n", firstLabelOfIf);

		ifStatement.thencase().accept(this);
		this.printWriter.format("\tbr label %%if%d\n", firstLabelOfIf + 2);
		this.printWriter.format("if%d:\n", firstLabelOfIf + 1);

		ifStatement.elsecase().accept(this);
		this.printWriter.format("\tbr label %%if%d\n", firstLabelOfIf + 2);

		this.printWriter.format("if%d:\n", firstLabelOfIf + 2);

	}

	@Override
	public void visit(WhileStatement whileStatement) {

		this.printWriter.format("\tbr label %%loop%d\n", ifLabelCounter++);
		ifLabelCounter += 2;

		int conditionWhileLabel = ifLabelCounter - 3;
		this.printWriter.format("\tloop%d:\n", conditionWhileLabel);

		whileStatement.cond().accept(this);
		this.printWriter.format("\tbr i1 %s, label %%loop%d, label %%loop%d\n", resultTemporalRegister,
				conditionWhileLabel + 1, conditionWhileLabel + 2);
		this.printWriter.format("\tloop%d:\n", conditionWhileLabel + 1);

		whileStatement.body().accept(this);
		this.printWriter.format("\tbr label %%loop%d\n", conditionWhileLabel);
		this.printWriter.format("\tloop%d:\n", conditionWhileLabel + 2);
	}

	@Override
	public void visit(SysoutStatement sysoutStatement) {

		sysoutStatement.arg().accept(this);
		this.printWriter.format("\tcall void (i32) @print_int(i32 %s)\n", resultTemporalRegister);

	}

	@Override
	public void visit(AssignStatement assignStatement) {

		assignStatement.rv().accept(this);
		String rvRegResult = this.resultTemporalRegister;

		SymbolTable currSymTable = ExtractTable(this.caller);
		
		String lvReg ="";
		if (this.caller.getClass() == ast.MethodDecl.class) {
			if (currSymTable.getVariableEntries().containsKey(assignStatement.lv())) {
				//this.printWriter.format("%%_%d = load i32*, i32** %%%s\n", registerCounter++, assignStatement.lv());
				lvReg = "%".concat(assignStatement.lv());
			}
			else {
				while (currSymTable != null) {
					if (currSymTable.getVariableEntries().containsKey(assignStatement.lv())) {
						int offset = currSymTable.getVariableOffset().get(assignStatement.lv());
						this.printWriter.format("\t%%_%d = getelementptr i8, i8* %%this, i32 %d\n", registerCounter++, offset);
						this.printWriter.format("\t%%_%d = bitcast i8* %%_%d to %s*\n", registerCounter++, registerCounter - 2,
								returnTypeLLVM(currSymTable.getVariableEntries().get(assignStatement.lv())));
						//this.printWriter.format("\t%%_%d = load %s, %%%s* %%_%d \n", registerCounter++,returnTypeLLVM(varType),returnTypeLLVM(varType),registerCounter-2);///Check if needed!!!!!!
						lvReg = "%_".concat(Integer.toString(registerCounter-1));
						break;
					}
					currSymTable = currSymTable.getParentSymbolTable();
				}
		} 
		}else {
			while (currSymTable != null) {
				if (currSymTable.getVariableEntries().containsKey(assignStatement.lv())) {
					int offset = currSymTable.getVariableOffset().get(assignStatement.lv());
					this.printWriter.format("\t%%_%d = getelementptr i8, i8* %%this, i32 %d\n", registerCounter++, offset);
					this.printWriter.format("\t%%_%d = bitcast i8* %%_%d to %s*\n", registerCounter++, registerCounter - 2,
							returnTypeLLVM(currSymTable.getVariableEntries().get(assignStatement.lv())));
					//this.printWriter.format("\t%%_%d = load %s, %%%s* %%_%d \n", registerCounter++,returnTypeLLVM(varType),returnTypeLLVM(varType),registerCounter-2);///Check if needed!!!!!!
					lvReg = "%_".concat(Integer.toString(registerCounter-1));
					break;
				}
				currSymTable = currSymTable.getParentSymbolTable();
			}
	} 
		MethodDecl temp = (MethodDecl)this.caller;
		String varType=LookupVarClassName(currSymTable,assignStatement.lv());
		String lvType = returnTypeLLVM(varType);

		this.printWriter.format("\tstore %s %s, %s* %s\n", lvType, rvRegResult, lvType, lvReg);

	}

	@Override
	public void visit(AssignArrayStatement assignArrayStatement) {

		SymbolTable currSymTable = ExtractTable(this.caller);
		
		if (this.caller.getClass() == ast.MethodDecl.class) {
			if (currSymTable.getVariableEntries().containsKey(assignArrayStatement.lv())) {
				this.printWriter.format("\t%%_%d = load i32*, i32** %%%s\n", registerCounter++, assignArrayStatement.lv());
			}
			 else {
					while (currSymTable != null) {
						if (currSymTable.getVariableEntries().containsKey(assignArrayStatement.lv())) {
							int offset = currSymTable.getVariableOffset().get(assignArrayStatement.lv());
							this.printWriter.format("\t%%_%d = getelementptr i8, i8* %%this, i32 %d\n", registerCounter++, offset);
							this.printWriter.format("\t%%_%d = bitcast i8* %%_%d to %s*\n", registerCounter++, registerCounter - 2,
									returnTypeLLVM(currSymTable.getVariableEntries().get(assignArrayStatement.lv())));
							this.printWriter.format("\t%%_%d = load i32*, i32** %%_%d\n", registerCounter++,registerCounter-2);///Check if needed!!!!!!
							break;
						}
						currSymTable = currSymTable.getParentSymbolTable();
					}
		}
		}else {
			while (currSymTable != null) {
				if (currSymTable.getVariableEntries().containsKey(assignArrayStatement.lv())) {
					int offset = currSymTable.getVariableOffset().get(assignArrayStatement.lv());
					this.printWriter.format("\t%%_%d = getelementptr i8, i8* %%this, i32 %d\n", registerCounter++, offset);
					this.printWriter.format("\t%%_%d = bitcast i8* %%_%d to %s*\n", registerCounter++, registerCounter - 2,
							returnTypeLLVM(currSymTable.getVariableEntries().get(assignArrayStatement.lv())));
					this.printWriter.format("\t%%_%d = load i32*, i32** %%_%d\n", registerCounter++,registerCounter-2);///Check if needed!!!!!!
					break;
				}
				currSymTable = currSymTable.getParentSymbolTable();
			}
}

		int lvAdressReg = registerCounter - 1;
		assignArrayStatement.index().accept(this);
		String lvTemporalRegister=resultTemporalRegister;
		assignArrayStatement.rv().accept(this);

		this.printWriter.format("\t%%_%d = icmp slt i32 %s, 0\n", registerCounter++, lvTemporalRegister);
				
		this.printWriter.format("\tbr i1 %%_%d, label %%arr_alloc%d, label %%arr_alloc%d\n", registerCounter - 1,
				ifLabelCounter++, ifLabelCounter++);

		this.printWriter.format("arr_alloc%d:\n", ifLabelCounter - 2);

		this.printWriter.format("\tcall void @throw_oob()\n" + "\tbr label %%arr_alloc%d\n", ifLabelCounter - 1);

		this.printWriter.format("arr_alloc%d:\n", ifLabelCounter - 1);

		this.printWriter.format("\t%%_%d = getelementptr i32, i32* %%_%d, i32 0\n", registerCounter++, lvAdressReg);
		this.printWriter.format("\t%%_%d = load i32, i32* %%_%d\n", registerCounter++, registerCounter - 2);

		this.printWriter.format("\t%%_%d = icmp sle i32 %%_%d, %s\n", registerCounter++, registerCounter - 2,
				lvTemporalRegister);
		this.printWriter.format("\tbr i1 %%_%d, label %%arr_alloc%d, label %%arr_alloc%d\n", registerCounter - 1,
				ifLabelCounter++, ifLabelCounter++);

		this.printWriter.format("arr_alloc%d:\n", ifLabelCounter - 2);

		this.printWriter.format("\tcall void @throw_oob()\n" + "\tbr label %%arr_alloc%d\n", ifLabelCounter - 1);

		this.printWriter.format("arr_alloc%d:\n", ifLabelCounter - 1);

		this.printWriter.format("\t%%_%d = add i32 %s, 1\n", registerCounter++, lvTemporalRegister);

		this.printWriter.format("\t%%_%d = getelementptr i32, i32* %%_%d, i32 %%_%d\n", registerCounter++, lvAdressReg,
				registerCounter - 2);
		int indexReg = registerCounter - 1;

		
		this.printWriter.format("\tstore i32 %s, i32* %%_%d\n", resultTemporalRegister, indexReg);

	}

	@Override
	public void visit(AndExpr e) {

		int andLabelBegining = ifLabelCounter;
		e.e1().accept(this);
		this.printWriter.format("\tbr label %%andcond%d\n", andLabelBegining);
		ifLabelCounter += 4;
		this.printWriter.format("andcond%d:\n", andLabelBegining);

		
		this.printWriter.format("\tbr i1 %s, label %%andcond%d, label %%andcond%d\n", resultTemporalRegister,
				andLabelBegining + 1, andLabelBegining + 3);
		this.printWriter.format("andcond%d:\n", andLabelBegining + 1);

		e.e2().accept(this);
		this.printWriter.format("\tbr label %%andcond%d\n", andLabelBegining + 2);

		this.printWriter.format("andcond%d:\n", andLabelBegining + 2);
		this.printWriter.format("\tbr label %%andcond%d\n", andLabelBegining + 3);

		this.printWriter.format("andcond%d:\n", andLabelBegining + 3);
		this.printWriter.format("\t%%_%d = phi i1 [0, %%andcond%d], [%s, %%andcond%d]\n", registerCounter++, andLabelBegining,
				resultTemporalRegister, andLabelBegining + 2);
		resultTemporalRegister = Integer.toString(registerCounter - 1);
		String tmp = "%_";
		resultTemporalRegister = tmp.concat(resultTemporalRegister);
	}

	@Override
	public void visit(LtExpr e) {

		e.e1().accept(this);
		String e1Result = resultTemporalRegister;

		e.e2().accept(this);

		this.printWriter.format("\t%%_%d = icmp slt %s %s, %s\n", registerCounter++, returnTypeLLVM(varType), e1Result,
				resultTemporalRegister);

		resultTemporalRegister = Integer.toString(registerCounter - 1);
		String tmp = "%_";
		resultTemporalRegister = tmp.concat(resultTemporalRegister);

	}

	@Override
	public void visit(AddExpr e) {

		e.e1().accept(this);
		String e1Result = resultTemporalRegister;
		e.e2().accept(this);
		this.printWriter.format("\t%%_%d = add %s %s, %s\n", registerCounter++, returnTypeLLVM(varType), e1Result,
				resultTemporalRegister);

		resultTemporalRegister = Integer.toString(registerCounter - 1);
		String tmp = "%_";
		resultTemporalRegister = tmp.concat(resultTemporalRegister);
	}

	@Override
	public void visit(SubtractExpr e) {

		e.e1().accept(this);
		String e1Result = resultTemporalRegister;

		e.e2().accept(this);

		this.printWriter.format("\t%%_%d = sub %s %s, %s\n", registerCounter++, returnTypeLLVM(varType), e1Result,
				resultTemporalRegister);

		resultTemporalRegister = Integer.toString(registerCounter - 1);
		String tmp = "%_";
		resultTemporalRegister = tmp.concat(resultTemporalRegister);
	}

	@Override
	public void visit(MultExpr e) {

		e.e1().accept(this);
		String e1Result = resultTemporalRegister;
		e.e2().accept(this);

		this.printWriter.format("\t%%_%d = mul %s %s, %s\n", registerCounter++, returnTypeLLVM(varType), e1Result,
				resultTemporalRegister);

		resultTemporalRegister = Integer.toString(registerCounter - 1);
		String tmp = "%_";
		resultTemporalRegister = tmp.concat(resultTemporalRegister);

	}

	@Override
	public void visit(ArrayAccessExpr e) {
	
		e.arrayExpr().accept(this);
		String arrayTemporalRegister=resultTemporalRegister;
		e.indexExpr().accept(this);
		
		this.printWriter.format("\t%%_%d = icmp slt i32 %s, 0\n",registerCounter++,resultTemporalRegister);
		this.printWriter.format("\tbr i1 %%_%d, label %%arr_alloc%d, label %%arr_alloc%d\n",registerCounter-1,ifLabelCounter++,ifLabelCounter++);
		
		this.printWriter.format("arr_alloc%d:\n",ifLabelCounter-2);
		this.printWriter.format("\tcall void @throw_oob()\n");
		this.printWriter.format("\tbr label %%arr_alloc%d\n",ifLabelCounter-1);


		this.printWriter.format("arr_alloc%d:\n",ifLabelCounter-1);
		this.printWriter.format("\t%%_%d = getelementptr i32, i32* %s, i32 0\n",registerCounter++,arrayTemporalRegister);
		this.printWriter.format("\t%%_%d = load i32, i32* %%_%d\n",registerCounter++,registerCounter-2);
		this.printWriter.format("\t%%_%d = icmp sle i32 %%_%d, %s\n",registerCounter++,registerCounter-2,resultTemporalRegister);
		this.printWriter.format("\tbr i1 %%_%d, label %%arr_alloc%d, label %%arr_alloc%d\n",registerCounter-1,ifLabelCounter++,ifLabelCounter++);
		
		this.printWriter.format("arr_alloc%d:\n",ifLabelCounter-2);
		this.printWriter.format("\tcall void @throw_oob()\n");
		this.printWriter.format("\tbr label %%arr_alloc%d\n",ifLabelCounter-1);
		
		this.printWriter.format("arr_alloc%d:\n",ifLabelCounter-1);
		this.printWriter.format("\t%%_%d = add i32 %s, 1\n",registerCounter++,resultTemporalRegister);
		this.printWriter.format("\t%%_%d = getelementptr i32, i32* %s, i32 %%_%d\n",registerCounter++,arrayTemporalRegister,registerCounter-2);
		this.printWriter.format("\t%%_%d = load i32, i32* %%_%d\n",registerCounter++,registerCounter-2);

		resultTemporalRegister = Integer.toString(registerCounter - 1);
		String tmp = "%_";
		resultTemporalRegister = tmp.concat(resultTemporalRegister);
		varType="int";


	}

	@Override
	public void visit(ArrayLengthExpr e) {

		e.arrayExpr().accept(this);
		this.printWriter.format("\t%%_%d = load i32, i32* %s\n",registerCounter++,resultTemporalRegister);
		resultTemporalRegister = "%_".concat(Integer.toString(registerCounter - 1));
	}

	@Override
	public void visit(MethodCallExpr e) {
		List<String> argsRegisterForMethod = new ArrayList<String>(); 
		e.ownerExpr().accept(this);
		String ownerReg = resultTemporalRegister;
		
	String varClassName =this.varType; 
		
		String methodRyruenType = "";
		String[] methodArgs= {""};
		int methodOffset=0;
		for (AstNode node : astMapping.keySet()) {
			if (node.getClass() == ast.ClassDecl.class) {
				ClassDecl currClassNode = (ClassDecl) node;
				if (currClassNode.name().equals(varClassName)) {
					SymbolTable methodSumbolTable = ExtractTable(currClassNode);
					while (methodSumbolTable != null) {
						
						if (methodSumbolTable.getMethodEntries().containsKey(e.methodId())) {
							
							methodRyruenType = methodSumbolTable.getMethodReturnType().get(e.methodId());
							methodArgs = methodSumbolTable.getMethodArguments().get(e.methodId());
							methodOffset =getMethodOffset(methodSumbolTable,e.methodId());
							varType= methodSumbolTable.getMethodEntries().get(e.methodId());
							break;
						}
						methodSumbolTable = methodSumbolTable.getParentSymbolTable();
					}

				}

			}
		}
		
		this.printWriter.format("\t%%_%d = bitcast i8* %s to i8***\n", registerCounter++, resultTemporalRegister);
		this.printWriter.format("\t%%_%d = load i8**, i8*** %%_%d\n", registerCounter++, registerCounter - 2);
		this.printWriter.format("\t%%_%d = getelementptr i8*, i8** %%_%d, i32 %d\n", registerCounter++, registerCounter - 2,methodOffset);
		this.printWriter.format("\t%%_%d = load i8*, i8** %%_%d\n", registerCounter++, registerCounter - 2);

		
	
		
		this.printWriter.format("\t%%_%d = bitcast i8* %%_%d to %s (i8*", registerCounter++, registerCounter - 2,
				returnTypeLLVM(methodRyruenType));
		 int registerSaveForCall =registerCounter-1;

		for (int i = 0; i < methodArgs.length; i++) {

			this.printWriter.format(", %s", returnTypeLLVM(methodArgs[i]));

		}
		this.printWriter.format(")*\n");

	
		for (Expr arg : e.actuals()) {

			arg.accept(this);
			argsRegisterForMethod.add(resultTemporalRegister);
			
		}
		
		
		this.printWriter.format("\t%%_%d = call %s %%_%d(i8* %s", registerCounter++,returnTypeLLVM(methodRyruenType),registerSaveForCall,ownerReg);
		int argCounter = 0;
		for (String register : argsRegisterForMethod)
		{
			this.printWriter.format(", %s %s",returnTypeLLVM(methodArgs[argCounter]),register);/*problem*/
			argCounter++;


		}
		
		this.printWriter.format(")\n");
		
		resultTemporalRegister = "%_".concat(Integer.toString(registerCounter-1));


	}

	@Override
	public void visit(IntegerLiteralExpr e) {
		resultTemporalRegister = Integer.toString(e.num());
		varType = "int";

	}

	@Override
	public void visit(TrueExpr e) {
		resultTemporalRegister = "1";
		varType = "boolean";

	}

	@Override
	public void visit(FalseExpr e) {
		resultTemporalRegister = "0";
		varType = "boolean";
	}
	
	@Override
	public void visit(IdentifierExpr e) {
		varType = LookupVarClassName(ExtractTable(this.caller), e.id());
		SymbolTable currSymTable= ExtractTable(this.caller);
		if (this.caller.getClass() == ast.MethodDecl.class) {
			if (currSymTable.getVariableEntries().containsKey(e.id())) {
				varType=currSymTable.getVariableEntries().get(e.id());
				this.printWriter.format("\t%%_%d = load %s, %s* %%%s\n", registerCounter++,returnTypeLLVM(varType),returnTypeLLVM(varType),e.id());
			}
			 else  { //variable not declared in method 
				while (currSymTable != null) {
					if (currSymTable.getVariableEntries().containsKey(e.id())) {
						int offset = currSymTable.getVariableOffset().get(e.id());
						this.printWriter.format("\t%%_%d = getelementptr i8, i8* %%this, i32 %d\n", registerCounter++, offset);
						this.printWriter.format("\t%%_%d = bitcast i8* %%_%d to %s*\n", registerCounter++, registerCounter - 2,returnTypeLLVM(currSymTable.getVariableEntries().get(e.id())));
						this.printWriter.format("\t%%_%d = load %s, %s* %%_%d\n", registerCounter++,returnTypeLLVM(varType),returnTypeLLVM(varType),registerCounter-2);///Check if needed!!!!!!
						break;
					}	
					currSymTable = currSymTable.getParentSymbolTable();
					}
				}
			}
		else {
			while (currSymTable != null) {
				if (currSymTable.getVariableEntries().containsKey(e.id())) {
					int offset = currSymTable.getVariableOffset().get(e.id());
					this.printWriter.format("\t%%_%d = getelementptr i8, i8* %%this, i32 %d\n", registerCounter++, offset);
					this.printWriter.format("\t%%_%d = bitcast i8* %%_%d to %s*\n", registerCounter++, registerCounter - 2,returnTypeLLVM(currSymTable.getVariableEntries().get(e.id())));
					this.printWriter.format("\t%%_%d = load %s, %s* %%_%d\n", registerCounter++,returnTypeLLVM(varType),returnTypeLLVM(varType),registerCounter-2);///Check if needed!!!!!!
					break;
					}
				currSymTable = currSymTable.getParentSymbolTable();
				}
			}
		resultTemporalRegister = Integer.toString(registerCounter - 1);
		String tmp = "%_";
		resultTemporalRegister = tmp.concat(resultTemporalRegister);	
		varType = LookupVarClassName(ExtractTable(this.caller), e.id());
	}

	public void visit(ThisExpr e) {
		SymbolTable callerSymbolTable= ExtractTable(this.caller);
		while (callerSymbolTable!=null)
		{
			if  (callerSymbolTable.className!=null)
			{
				varType=callerSymbolTable.className;
				break;
			}
			callerSymbolTable=callerSymbolTable.getParentSymbolTable();
		}
		resultTemporalRegister = "%this";
	}


	@Override
	public void visit(NewIntArrayExpr e) {

		e.lengthExpr().accept(this);
		this.printWriter.format("\t%%_%d = icmp slt i32 %s, 0\n", registerCounter++,
				resultTemporalRegister);

		int currentArrLabel = ifLabelCounter;
		ifLabelCounter += 2;
		this.printWriter.format("\tbr i1 %%_%d, label %%arr_alloc%d, label %%arr_alloc%d\n", registerCounter - 1,
				currentArrLabel, currentArrLabel + 1);

		this.printWriter.format("arr_alloc%d:\n", currentArrLabel);

		this.printWriter.print("\tcall void @throw_oob()\n");

		this.printWriter.format("\tbr label %%arr_alloc%d\n", currentArrLabel + 1);

		this.printWriter.format("arr_alloc%d:\n", currentArrLabel + 1);

		this.printWriter.format("\t%%_%d = add i32 %s, 1\n", registerCounter++, resultTemporalRegister);

		this.printWriter.format("\t%%_%d = call i8* @calloc(i32 4, i32 %%_%d)\n", registerCounter++, registerCounter - 2);

		this.printWriter.format("\t%%_%d = bitcast i8* %%_%d to i32*\n", registerCounter++, registerCounter - 2);

		this.printWriter.format("\tstore i32 %s, i32* %%_%d\n", resultTemporalRegister, registerCounter-1);

		resultTemporalRegister = Integer.toString(registerCounter - 1);
		String tmp = "%_";
		resultTemporalRegister = tmp.concat(resultTemporalRegister);

	}

	@Override
	public void visit(NewObjectExpr e) {
		String className = e.classId();
		int vtableSize=0;
		int numberOfMethods=0;
		for (AstNode node : astMapping.keySet()) {
			if (node.getClass() == ast.ClassDecl.class) {
				ClassDecl currClassNode = (ClassDecl) node;
				if (currClassNode.name().equals(className))
				{
					vtableSize = ExtractTable(currClassNode).vtableSize;
					numberOfMethods = ExtractTable(currClassNode).getMethodOffset().size();
					break;
				}
			}
		}
		 	
		this.printWriter.format("\t%%_%d = call i8* @calloc(i32 1, i32 %d)\n", registerCounter++, vtableSize);
		this.printWriter.format("\t%%_%d = bitcast i8* %%_%d to i8***\n",registerCounter++,registerCounter-2);		
		this.printWriter.format("\t%%_%d = getelementptr [%d x i8*], [%d x i8*]* @.%s_vtable, i32 0, i32 0\n",registerCounter++,numberOfMethods,numberOfMethods,className);
		this.printWriter.format("\tstore i8** %%_%d, i8*** %%_%d\n",registerCounter-1,registerCounter-2);	

		resultTemporalRegister = Integer.toString(registerCounter - 3);	
		resultTemporalRegister = "%_".concat(resultTemporalRegister);
		this.varType = e.classId();
	}

	@Override
	public void visit(NotExpr e) {

		e.e().accept(this);
		this.printWriter.format("\t%%_%d = sub i1 1, %s\n",registerCounter++ , resultTemporalRegister);
		resultTemporalRegister = Integer.toString(registerCounter - 1);	
		resultTemporalRegister = "%_".concat(resultTemporalRegister);

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

	public String returnTypeLLVM(String type) {
		if (type.equals("int")) {
			return "i32";
		}
		if (type.equals("boolean")) {
			return "i1";

		}
		if (type.equals("int[]")) {
			return "i32*";
		}
		else {
			return "i8*";
		}
	}

	private SymbolTable ExtractTable(AstNode nodeCaller) {
		SymbolTable table = astMapping.get(nodeCaller);
		return table;
	}

	private String LookupVarClassName(SymbolTable table, String name) {
		while (table != null) {
			if (table.getVariableEntries().containsKey(name)) {
				return table.getVariableEntries().get(name);
			}
			table = table.getParentSymbolTable();
		}
		return null;
	}
	
	private int getMethodOffset(SymbolTable table, String methodID)
	{
		return table.getMethodOffset().get(methodID);
	}
	

}
