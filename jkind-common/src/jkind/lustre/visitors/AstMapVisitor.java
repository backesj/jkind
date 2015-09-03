package jkind.lustre.visitors;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jkind.lustre.Ast;
import jkind.lustre.Constant;
import jkind.lustre.Contract;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.RecursiveFunction;
import jkind.lustre.TypeDef;
import jkind.lustre.VarDecl;

public class AstMapVisitor extends ExprMapVisitor implements AstVisitor<Ast, Expr> {
	@Override
	public Constant visit(Constant e) {
		return new Constant(e.location, e.id, e.type, e.expr.accept(this));
	}

	@Override
	public Equation visit(Equation e) {
		// Do not traverse e.lhs since they do not really act like Exprs
		return new Equation(e.location, e.lhs, e.expr.accept(this));
	}

	@Override
	public Node visit(Node e) {
		List<VarDecl> inputs = visitVarDecls(e.inputs);
		List<VarDecl> outputs = visitVarDecls(e.outputs);
		List<VarDecl> locals = visitVarDecls(e.locals);
		List<Equation> equations = visitEquations(e.equations);
		List<Expr> assertions = visitAssertions(e.assertions);
		List<String> properties = visitProperties(e.properties);
		List<Contract> contracts = vistContracts(e.contracts);
		return new Node(e.location, e.id, inputs, outputs, locals, equations, properties,
				assertions, e.realizabilityInputs, Optional.of(contracts));
	}


	private List<Contract> vistContracts(Optional<List<Contract>> contracts) {
		if(contracts.isPresent()){
			return map(this::visit, contracts.get());
		}else{
			return Collections.emptyList();
		}
		
	}

	protected List<VarDecl> visitVarDecls(List<VarDecl> es) {
		return map(this::visit, es);
	}

	protected List<Equation> visitEquations(List<Equation> es) {
		return map(this::visit, es);
	}

	protected List<Expr> visitAssertions(List<Expr> es) {
		return visitExprs(es);
	}

	protected List<String> visitProperties(List<String> es) {
		return map(this::visitProperty, es);
	}

	protected String visitProperty(String e) {
		return e;
	}

	@Override
	public Program visit(Program e) {
		List<TypeDef> types = visitTypeDefs(e.types);
		List<Constant> constants = visitConstants(e.constants);
		List<Node> nodes = visitNodes(e.nodes);
		List<RecursiveFunction> recFuns = visitRecFuns(e.recFuns);
		return new Program(e.location, types, constants, nodes, e.main, recFuns);
	}

	private List<RecursiveFunction> visitRecFuns(List<RecursiveFunction> es) {
	    return map(this::visit, es);  //TODO why does this not work?
    }

    protected List<TypeDef> visitTypeDefs(List<TypeDef> es) {
		return map(this::visit, es);
	}

	protected List<Constant> visitConstants(List<Constant> es) {
		return map(this::visit, es);
	}

	protected List<Node> visitNodes(List<Node> es) {
		return map(this::visit, es);
	}

	@Override
	public TypeDef visit(TypeDef e) {
		return e;
	}

	@Override
	public VarDecl visit(VarDecl e) {
		return e;
	}

	@Override
	public Contract visit(Contract contract) {
		return new Contract(contract.name, visitExprs(contract.requires), visitExprs(contract.ensures));
	}

    @Override
    public RecursiveFunction visit(RecursiveFunction e) {
        List<VarDecl> inputs = visitVarDecls(e.inputs);
        VarDecl output = visit(e.output);
        List<VarDecl> locals = visitVarDecls(e.locals);
        List<Equation> equations = visitEquations(e.equations);
        
        return new RecursiveFunction(e.location, e.id, inputs, locals, output, equations);
    }
}
