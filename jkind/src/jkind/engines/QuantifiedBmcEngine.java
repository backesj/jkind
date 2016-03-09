package jkind.engines;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jkind.JKindException;
import jkind.JKindSettings;
import jkind.engines.messages.InvalidMessage;
import jkind.engines.messages.ValidMessage;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.LustreUtil;
import jkind.lustre.QuantExpr;
import jkind.lustre.VarDecl;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.solvers.Model;
import jkind.solvers.Result;
import jkind.solvers.SatResult;
import jkind.solvers.UnknownResult;
import jkind.solvers.UnsatResult;
import jkind.solvers.cvc4.Cvc4MultiSolver;
import jkind.translation.InductiveDataTypeSpecification;
import jkind.translation.InlineTransitionRelation;
import jkind.translation.Lustre2Sexp;
import jkind.translation.Lustre2SexpMaybeArrow;
import jkind.translation.Lustre2SexpNoArrow;
import jkind.util.Util;

public class QuantifiedBmcEngine extends BmcEngine{

	private final Map<String, Expr> exprMap;
	private Sexp currentQuery = null;
	
	public QuantifiedBmcEngine(InductiveDataTypeSpecification spec, JKindSettings settings, Director director) {
		super(spec, settings, director);
		exprMap = spec.propertyExprs;
	}
	
	@Override
	protected void checkProperties(int k) {
		Result result;
		List<String> possiblyFalse = new ArrayList<>(properties);
		do {
			String prop = possiblyFalse.remove(0);
			if(!properties.contains(prop)){
				continue;
			}
			List<String> singleProp = Collections.singletonList(prop);
			
			result = query(prop, k);
			if (result instanceof SatResult || result instanceof UnknownResult) {
				Model model = getModel(result);
				if (model == null) {
					sendUnknown(properties);
					properties.clear();
					break;
				}

				if (result instanceof SatResult) {
					sendInvalid(singleProp, k, model);
					properties.remove(prop);
				} else {
					sendUnknown(singleProp);
				}
				
			}
		} while (!possiblyFalse.isEmpty());

		sendBaseStep(k);
	}
	
	private Result query(String prop, int k){
		if(!(solver instanceof Cvc4MultiSolver)){
			throw new JKindException("QuantifiedBmcEngine expects "
					+ "its solver to be the Cvc4MultiSolver");
		}
		if(!properties.contains(prop)){
			throw new JKindException("We assume that this property "
					+ "is in the properties at the begining of the call");
		}
		
		Cvc4MultiSolver multiSolver = (Cvc4MultiSolver)solver;
		
		List<String> singleProp = Collections.singletonList(prop);
		Sexp sexp = toSexps(singleProp, k).get(0);
		
		currentQuery = sexp;
		multiSolver.asyncQuery(sexp);
		
		while(!multiSolver.asyncQueryCompleted() && properties.contains(prop)){
			processMessages();
			try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
		}
		
		Result result;
		if(properties.contains(prop)){
			result = multiSolver.getAsyncQueryResult();
		}else{
			result = new UnsatResult();
		}
		
		multiSolver.cancelAsyncQuery();
		return result;
	}
	
	private List<Sexp> toSexps(List<String> props, int k){
		List<Sexp> sexps = new ArrayList<>();
		Lustre2Sexp specialTranslater = new Lustre2SexpNoArrow(k);
		for(String prop : props){
			Expr expr = exprMap.get(prop);
			if(expr != null){
				sexps.add(expr.accept(specialTranslater));
			}else{
				sexps.add((new IdExpr(prop).accept(specialTranslater)));
			}
		}
		return sexps;
	}
	
//	@Override
//	protected Sexp getTransition(int k, Sexp init) {
//		return InlineTransitionRelation.inline(spec.transitionRelation, k);
//	}
	
	@Override
	protected void handleMessage(InvalidMessage im) {
		if (!im.source.equals(this.name)) {
			throw new JKindException("We did not expect another engine to send an invalid "
					+ "message while using Inductive Datatypes and/or Quantified Expressions");
		}
	}
	
	@Override
	protected void handleMessage(ValidMessage vm) {
		properties.removeAll(vm.valid);
		validProperties.addAll(vm.valid);
		
		//strengthen the unsat process if the current query is not yet completed
		Cvc4MultiSolver multiSolver = (Cvc4MultiSolver)solver;
		if(multiSolver.isRunning()){
			multiSolver.cancelAsyncQuery();
			addPropertiesAsInvariants(vm.k, vm.valid);
			multiSolver.asyncQuery(currentQuery);
		}else{
			addPropertiesAsInvariants(vm.k, vm.valid);
		}
	}
	
	protected void addPropertiesAsInvariants(int k, List<String> valid) {
		List<Expr> newInvariants = new ArrayList<>();
		for(String str : valid){
			Expr expr = exprMap.get(str);
			//we only care about strengthening with quantified expressions
			if (expr != null) {
				newInvariants.add(expr);
			}
		}
		assertNewInvariants(newInvariants, k);
	}
	
	protected void assertNewInvariants(List<Expr> invariants, int limit) {
		for (int i = 0; i <= limit; i++) {
			assertInvariants(invariants, i);
		}
	}

	protected void assertInvariants(List<Expr> invariants, int i) {
		Cvc4MultiSolver cvc4Solver = (Cvc4MultiSolver)solver;
		Lustre2Sexp translater = new Lustre2SexpMaybeArrow(i);
		for (Expr expr : invariants) {
			cvc4Solver.assertUnsatOnly(expr.accept(translater));
		}
	}
	
}
