package jkind.solvers.smtlib2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jkind.JKindException;
import jkind.lustre.InductType;
import jkind.lustre.InductTypeElement;
import jkind.lustre.NamedType;
import jkind.lustre.Type;
import jkind.lustre.TypeConstructor;
import jkind.lustre.VarDecl;
import jkind.lustre.parsing.StdoutErrorListener;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.sexp.Symbol;
import jkind.solvers.Model;
import jkind.solvers.ProcessBasedSolver;
import jkind.solvers.Result;
import jkind.solvers.SatResult;
import jkind.solvers.UnknownResult;
import jkind.solvers.UnsatResult;
import jkind.solvers.smtlib2.SmtLib2Parser.ModelContext;
import jkind.translation.Relation;
import jkind.util.Util;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;

public abstract class SmtLib2Solver extends ProcessBasedSolver {
    private final Set<String> definedTypes = new HashSet<>();
    private final Set<String> typeConstructors = new HashSet<>();
	public SmtLib2Solver(String scratchBase) {
		super(scratchBase);
	}

	@Override
	public void assertSexp(Sexp sexp) {
		send(new Cons("assert", sexp));
	}

	protected void send(Sexp sexp) {
		send(Quoting.quoteSexp(sexp).toString());
	}

	protected void send(String str) {
		scratch(str);
		try {
			toSolver.append(str);
			toSolver.newLine();
			toSolver.flush();
		} catch (IOException e) {
			throw new JKindException("Unable to write to " + getSolverName() + ", "
					+ "probably due to internal JKind error", e);
		}
	}

	public Symbol type(Type type) {
		return new Symbol(capitalize(Util.getName(type)));
	}

	private String capitalize(String name) {
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}

	@Override
	public void define(VarDecl decl) {
		varTypes.put(decl.id, decl.type);
		send(new Cons("declare-fun", new Symbol(decl.id), new Symbol("()"), type(decl.type)));
	}

	@Override
	public void define(Relation relation) {
		send(new Cons("define-fun", new Symbol(relation.getName()), inputs(relation.getInputs()),
				type(NamedType.BOOL), relation.getBody()));
	}
	
	@Override
	public void define(InductType type) {

        if (!definedTypes.contains(type.name)) {
            definedTypes.add(type.name);
            List<Sexp> constructorExprs = new ArrayList<>();
            for (TypeConstructor constructor : type.constructors) {
                // args.add(new Symbol(constructor.name));
            	typeConstructors.add(constructor.name);
                List<Sexp> args = new ArrayList<>();
                for (InductTypeElement element : constructor.elements) {
                    args.add(new Cons(element.name, type(element.type)));
                }
                constructorExprs.add(new Cons(constructor.name, args));
            }
            Sexp cons = new Cons(type.name, constructorExprs);
            cons = new Cons("declare-datatypes", new Symbol("()"), new Symbol("(" + cons.toString() + ")"));
            send(cons);
            isWellFounded();
        }
	}

    protected boolean isWellFounded() {
        try {
            if (fromSolver.ready()) {
                String line = fromSolver.readLine();
                if (line.contains(" is not well-founded")) {
                    int endIndex = line.indexOf(" is not well-founded");
                    // TODO: this will break if the error message changes
                    String typeName = line.substring(21, endIndex);
                    throw new JKindException("Type '" + typeName + "' is not well-founded");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
	
	private Sexp inputs(List<VarDecl> inputs) {
		List<Sexp> args = new ArrayList<>();
		for (VarDecl vd : inputs) {
			args.add(new Cons(vd.id, type(vd.type)));
		}
		return new Cons(args);
	}

	@Override
	public Result query(Sexp sexp) {
		Result result = null;
		push();

		assertSexp(new Cons("not", sexp));
		send("(check-sat)");
		String status = readFromSolver();
		if (isSat(status)) {
			send("(get-model)");
			result = new SatResult(parseModel(readFromSolver()));
		} else if (isUnsat(status)) {
			result = new UnsatResult();
		} else {
		    result = new UnknownResult();
			throw new IllegalArgumentException("Unknown result: " + result);
		}

		pop();
		return result;
	}

	@Override
	protected Result quickCheckSat(List<Symbol> activationLiterals) {
		push();
		for (Symbol actLit : activationLiterals) {
			String name = "_" + actLit.str;
			assertSexp(new Cons("!", actLit, new Symbol(":named"), new Symbol(name)));
		}

		send("(check-sat)");
		String status = readFromSolver();
		Result result;
		if (isSat(status)) {
			result = new SatResult();
		} else if (isUnsat(status)) {
			result = new UnsatResult(getUnsatCore(activationLiterals));
		} else {
			result = new UnknownResult();
		}
		
		pop();
		return result;
	}

	protected abstract List<Symbol> getUnsatCore(List<Symbol> activationLiterals);

	protected boolean isSat(String output) {
		return output.trim().equals("sat");
	}

	protected boolean isUnsat(String output) {
		return output.trim().equals("unsat");
	}
	
	protected String readFromSolver() {
		send("(echo \"" + DONE + "\")");

		try {
			String line;
			StringBuilder content = new StringBuilder();
			while (true) {
				line = fromSolver.readLine();
				comment(getSolverName() + ": " + line);
				if (line == null) {
					throw new JKindException(getSolverName() + " terminated unexpectedly");
				} else if (line.contains("define-fun " + Relation.T + " ")) {
					// No need to parse the transition relation
				} else if (isDone(line)) {
					break;
				} else if (line.contains("model is not available")) {
					return null;
				} else if (line.contains(" is not well-founded")){
				    int endIndex = line.indexOf(" is not well-founded");
				    //TODO: this will break if the error message changes
				    String typeName = line.substring(21, endIndex);
				    throw new JKindException("Type '"+typeName+"' is not well-founded");
				} else if (line.contains(" |-> ")) {
					// Ignore Z3 optimization information
				} else if (line.contains("error \"") || line.contains("Error:")) {
					// Flush the output since errors span multiple lines
					while ((line = fromSolver.readLine()) != null) {
						comment(getSolverName() + ": " + line);
						if (isDone(line)) {
							break;
						}
					}
					throw new JKindException(getSolverName()
							+ " error (see scratch file for details)");
				} else {
					content.append(line);
					content.append("\n");
				}
			}

			return content.toString();
		} catch (RecognitionException e) {
			throw new JKindException("Error parsing " + getSolverName() + " output", e);
		} catch (IOException e) {
			throw new JKindException("Unable to read from " + getSolverName(), e);
		}
	}

	protected boolean isDone(String line) {
		return line.contains(DONE);
	}

	protected Model parseModel(String string) {
		CharStream stream = new ANTLRInputStream(string);
		SmtLib2Lexer lexer = new SmtLib2Lexer(stream);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		SmtLib2Parser parser = new SmtLib2Parser(tokens);
		parser.removeErrorListeners();
		parser.addErrorListener(new StdoutErrorListener());
		ModelContext ctx = parser.model();

		if (parser.getNumberOfSyntaxErrors() > 0) {
			throw new JKindException("Error parsing " + getSolverName() + " output: " + string);
		}
		return ModelExtractor.getModel(ctx, varTypes, typeConstructors);
	}

	@Override
	public void push() {
		send("(push 1)");
	}

	@Override
	public void pop() {
		send("(pop 1)");
	}

	@Override
	public String getSolverExtension() {
		return "smt2";
	}
}
