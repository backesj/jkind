package jkind.translation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import jkind.lustre.Expr;
import jkind.lustre.Function;
import jkind.lustre.FunctionCallExpr;
import jkind.lustre.Program;
import jkind.lustre.RecordAccessExpr;
import jkind.lustre.RecordType;
import jkind.lustre.Type;
import jkind.lustre.VarDecl;
import jkind.lustre.builders.ProgramBuilder;
import jkind.lustre.visitors.TypeAwareAstMapVisitor;

public class FlattenFunctionsWithRecords extends TypeAwareAstMapVisitor {

	public static final String functionRecordDelimeter = "~~";

	public FlattenFunctionsWithRecords(Program program) {
		super(program.functions);
	}

	public static Program program(Program program) {

		// this flattens all the record arguments
		program = new FlattenFunctionsWithRecords(program).visit(program);

		// if the function has a record type return value then make copies for
		// the arguments
		List<Function> newFuncs = new ArrayList<>();
		for (Function func : program.functions) {
			if (func.outputs.size() != 1) {
				throw new IllegalArgumentException("We expect functions to have single return values");
			}
			VarDecl returnVar = func.outputs.get(0);
			if (returnVar.type instanceof RecordType) {
				List<VarDecl> flatVars = flattenVar(returnVar);
				for (VarDecl flatVar : flatVars) {
					String name = flatVar.id.replace(".", functionRecordDelimeter);
					name = name.replaceFirst("[^~]*" + functionRecordDelimeter, "~~");
					name = func.id + name;
					newFuncs.add(new Function(name, func.inputs, Collections.singletonList(flatVar)));
				}
			}
			newFuncs.add(func);

		}
		return new ProgramBuilder(program).clearFunctions().addFunctions(newFuncs).build();
	}

	@Override
	public Function visit(Function e) {
		List<VarDecl> inputs = flattenVars(e.inputs);
		List<VarDecl> outputs = visitVarDecls(e.outputs);
		return new Function(e.location, e.id, inputs, outputs);
	}

	@Override
	public Expr visit(FunctionCallExpr expr) {
		List<Expr> args = visitExprs(expr.args);
		return new FunctionCallExpr(expr.function, flattenAllAccesses(args));
	}

	public List<Expr> flattenAllAccesses(List<Expr> exprs) {
		List<Expr> accesses = new ArrayList<>();
		for (Expr expr : exprs) {
			accesses.addAll(flattenAccesses(expr));
		}
		return accesses;
	}

	public List<Expr> flattenAccesses(Expr expr) {
		List<Expr> accesses = new ArrayList<>();
		Type type = getType(expr);

		if (type instanceof RecordType) {
			RecordType recType = (RecordType) type;
			for (Entry<String, Type> entry : recType.fields.entrySet()) {
				Expr newExpr = new RecordAccessExpr(expr, entry.getKey());
				accesses.addAll(flattenAccesses(newExpr));
			}
		} else {
			accesses.add(expr);
		}
		return accesses;

	}

	public static List<VarDecl> flattenVars(List<VarDecl> vars) {
		List<VarDecl> flats = new ArrayList<>();
		for (VarDecl var : vars) {
			flats.addAll(flattenVar(var));
		}
		return flats;
	}

	public static List<VarDecl> flattenVar(VarDecl var) {
		List<VarDecl> flats = new ArrayList<>();
		Type type = var.type;
		if (type instanceof RecordType) {
			RecordType recType = (RecordType) type;
			for (Entry<String, Type> entry : recType.fields.entrySet()) {
				List<VarDecl> subFlats = flattenVar(new VarDecl(entry.getKey(), entry.getValue()));
				for (VarDecl subVar : subFlats) {
					flats.add(new VarDecl(var.id + "." + subVar.id, subVar.type));
				}
			}
		} else {
			flats.add(var);
		}
		return flats;
	}

}
