import ast.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {
	public static void main(String[] args) {
		try {
			var inputMethod = args[0];
			var action = args[1];
			var filename = args[args.length - 2];
			var outfilename = args[args.length - 1];

			Program prog;

			if (inputMethod.equals("parse")) {
				
				FileReader fileReader = new FileReader(new File(filename));
	        
	            Parser p = new Parser(new Lexer(fileReader));
	            prog = (Program) p.parse().value;
	            	            
	            
	            
			} else if (inputMethod.equals("unmarshal")) {
				AstXMLSerializer xmlSerializer = new AstXMLSerializer();
				prog = xmlSerializer.deserialize(new File(filename));
			} else {
				throw new UnsupportedOperationException("unknown input method " + inputMethod);
			}

			var outFile = new PrintWriter(outfilename);
			try {

				if (action.equals("marshal")) {
					AstXMLSerializer xmlSerializer = new AstXMLSerializer();
					xmlSerializer.serialize(prog, outfilename);
				} else if (action.equals("print")) {
					AstPrintVisitor astPrinter = new AstPrintVisitor();
					astPrinter.visit(prog);
					outFile.write(astPrinter.getString());

				} else if (action.equals("semantic")) {
						try {
							SemanticCheckVisitor visitorSemanticCheck = new SemanticCheckVisitor(outFile, prog.classDecls());
							visitorSemanticCheck.visit(prog);

							AstSymbolTableVisitor symbolTableVisitor = new AstSymbolTableVisitor();
							symbolTableVisitor.visit(prog);
							buildClassHierarchy(symbolTableVisitor.res);
							SemanticCheckVisitorAdvanced advancedVisitor = new SemanticCheckVisitorAdvanced(
									symbolTableVisitor.res, outFile, prog.classDecls());
							advancedVisitor.visit(prog);
							outFile.print("OK\n");
						}catch(RuntimeException ex) {
							outFile.print("ERROR\n");
							outFile.flush();
							outFile.close();
						}
					

					outFile.flush();
					outFile.close();

				} else if (action.equals("compile")) {
					// Build Symbol Tables
					AstSymbolTableVisitor symbolTableVisitor = new AstSymbolTableVisitor();
					symbolTableVisitor.visit(prog);
					buildClassHierarchy(symbolTableVisitor.res);

					buildVtables(symbolTableVisitor.res);
					printLlvmVtables(symbolTableVisitor.res, outFile);

					codeGenVisitor codGen = new codeGenVisitor(outFile, symbolTableVisitor.res);
					codGen.visit(prog);

					outFile.flush();
					outFile.close();

				} else if (action.equals("rename")) {
					var type = args[2];
					var originalName = args[3];
					var originalLine = args[4];
					var newName = args[5];

					boolean isMethod;
					if (type.equals("var")) {
						isMethod = false;
					} else if (type.equals("method")) {
						isMethod = true;
					} else {
						throw new IllegalArgumentException("unknown rename type " + type);
					}
					AstSymbolTableVisitor symbolTableVisitor = new AstSymbolTableVisitor();
					symbolTableVisitor.visit(prog);
					buildClassHierarchy(symbolTableVisitor.res);

					AstDeclRetriverVisitor declTable = new AstDeclRetriverVisitor(symbolTableVisitor.res, isMethod,
							originalName, originalLine);

					declTable.visit(prog);
					if (declTable.getDeclTable() == null) {
						System.out.println("method/variable name wasnt find in program");
						return;
					}
					AstRenameVisitor renameVisitor = new AstRenameVisitor(symbolTableVisitor.res,
							declTable.getDeclTable(), isMethod, originalName, originalLine, newName);
					renameVisitor.visit(prog);

					AstXMLSerializer xmlSerializer = new AstXMLSerializer();
					xmlSerializer.serialize(prog, outfilename);

				} else {
					throw new IllegalArgumentException("unknown command line action " + action);
				}
			} finally {
				outFile.flush();
				outFile.close();
			}

		} catch (FileNotFoundException e) {
			System.out.println("Error reading file: " + e);
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("General error: " + e);
			e.printStackTrace();
		}
	}

	private static void buildClassHierarchy(AstToSymbol res) {
		Map<AstNode, SymbolTable> map = res.mapAstNodeToSymboTable;

		for (AstNode node : map.keySet()) {
			if (node.getClass() == ast.ClassDecl.class) {
				ClassDecl classDeclNode = (ClassDecl) node;
				if (classDeclNode.superName() != null) {
					ClassDecl superClass = retriveSuperClass(map, classDeclNode);
					map.get(classDeclNode).setParentSymbolTable(map.get(superClass));
				}
			}
		}
	}

	private static ClassDecl retriveSuperClass(Map<AstNode, SymbolTable> map, ClassDecl classDeclNode) {
		for (AstNode node : map.keySet()) {
			if (node.getClass() == ast.ClassDecl.class) {
				ClassDecl currClassNode = (ClassDecl) node;
				if (currClassNode.name().equals(classDeclNode.superName())) {
					return currClassNode;
				}
			}
		}
		return null;
	}

	static void buildVtables(AstToSymbol astToSymbol) {
		Map<AstNode, SymbolTable> map = astToSymbol.mapAstNodeToSymboTable;
		for (AstNode node : map.keySet()) {
			if (node.getClass() == ast.ClassDecl.class) {
				ClassDecl currClassNode = (ClassDecl) node;

				SymbolTable currentSymbolTable = map.get(node);
				SymbolTable tmp2 = currentSymbolTable.getParentSymbolTable();

				List<String> methods = currentSymbolTable.getMethodInOrder();
				int numOfMethods = methods.size();

				List<String> variables = currentSymbolTable.getVariableInOrder();
				int numOfVariables = variables.size();

				SymbolTable tmp = currentSymbolTable.getParentSymbolTable();

				Map<String, Integer> methodsOffset = currentSymbolTable.getMethodOffset();
				Map<String, String> methodClass = currentSymbolTable.getMethodClass();
				Map<String, String[]> methodArguments = currentSymbolTable.getMethodArguments();
				Map<String, String> methodReturnType = currentSymbolTable.getMethodReturnType();
				List<String> methodInOrder = currentSymbolTable.getMethodInOrder();
				Map<String, Integer> variableOffset = currentSymbolTable.getVariableOffset();

				int methodOffset = 0;

				for (String method : methods) {
					methodsOffset.put(method, methodOffset);
					methodOffset++;
				}
				int variablesOffset = 8;
				for (String variable : variables) {

					variableOffset.put(variable, variablesOffset);

					switch (currentSymbolTable.getVariableEntries().get(variable)) {
					case ("int"):
						variablesOffset += 4;
						break;
					case ("int[]"):
						variablesOffset += 8;
						break;
					case ("boolean"):
						variablesOffset += 1;

						break;
					default:
						variablesOffset += 8;
					}

				}

				while (tmp != null) {
					for (String method : tmp.getMethodInOrder()) {
						// Check if method wasn't found in the parent symbol table class
						if (!methods.contains(method)) {
							numOfMethods++;
							methodsOffset.put(method, methodOffset);
							methodOffset++;
							methodClass.put(method, tmp.className);
							methodArguments.put(method, tmp.getMethodArguments().get(method));
							methodReturnType.put(method, tmp.getMethodReturnType().get(method));
							methodInOrder.add(method);

						}
					}
					for (String variable : tmp.getVariableInOrder()) {
						if (variables.size() == 0 || !variables.contains(variable)) {
							numOfVariables++;
							variableOffset.put(variable, variablesOffset);

							switch (tmp.getVariableEntries().get(variable)) {
							case ("int"):
								variablesOffset += 4;
								break;
							case ("int[]"):
								variablesOffset += 8;
								break;
							case ("boolean"):
								variablesOffset += 1;
								break;
							default:
								variablesOffset += 8;
							}
						}
					}
					tmp = tmp.getParentSymbolTable();

					// System.out.println("Printing info about parent map"+tmp.toString());

				}
				currentSymbolTable.vtableSize = variablesOffset;

			}
		}

	}

	static void printLlvmVtables(AstToSymbol astToSymbol, PrintWriter pw) {
		Map<AstNode, SymbolTable> map = astToSymbol.mapAstNodeToSymboTable;
		List<ClassDecl> classes = astToSymbol.progSymbolTable.getClassInOrder();
		for (ClassDecl node : classes) {
			if (node.getClass() == ast.ClassDecl.class) {
				ClassDecl currClassNode = (ClassDecl) node;
				SymbolTable currentSymbolTable = map.get(node);

				int numOfMethods = currentSymbolTable.getMethodOffset().keySet().size();
				// Print LLVM code
				pw.format("@.%s_vtable = global [%d x i8*] [", currClassNode.name(), numOfMethods);
				// Print current class Methods

				for (String method : currentSymbolTable.getMethodInOrder()) {
					numOfMethods--;
					String returnType = currentSymbolTable.getMethodReturnType().get(method);

					pw.format("i8* bitcast (%s (i8*", returnTypeLLVM(returnType));
					for (String argument : currentSymbolTable.getMethodArguments().get(method)) {
						pw.format(", %s", returnTypeLLVM(argument));
					}
					String className = currentSymbolTable.getMethodEntries().containsKey(method) ? currClassNode.name()
							: currentSymbolTable.getMethodClass().get(method);
					pw.format(")* @%s.%s to i8*)", className, method);
					if (numOfMethods != 0) {
						pw.format(", ");
					}

				}
				pw.print("]");
				pw.println();

			}
		}

		pw.println("\ndeclare i8* @calloc(i32, i32)\r\n" + "declare i32 @printf(i8*, ...)\r\n"
				+ "declare void @exit(i32)\r\n" + "\r\n" + "@_cint = constant [4 x i8] c\"%d\\0a\\00\"\r\n"
				+ "@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\r\n" + "define void @print_int(i32 %i) {\r\n"
				+ "    %_str = bitcast [4 x i8]* @_cint to i8*\r\n"
				+ "    call i32 (i8*, ...) @printf(i8* %_str, i32 %i)\r\n" + "    ret void\r\n" + "}\r\n" + "\r\n"
				+ "define void @throw_oob() {\r\n" + "    %_str = bitcast [15 x i8]* @_cOOB to i8*\r\n"
				+ "    call i32 (i8*, ...) @printf(i8* %_str)\r\n" + "    call void @exit(i32 1)\r\n"
				+ "    ret void\r\n" + "}");

	}

	static String returnTypeLLVM(String type) {
		if (type.equals("int")) {
			return "i32";
		}
		if (type.equals("boolean")) {
			return "i1";

		}
		if (type.equals("int[]")) {
			return "i32*";
		} else {
			return "i8*";
		}
	}

}
