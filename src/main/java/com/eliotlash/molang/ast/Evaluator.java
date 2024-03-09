package com.eliotlash.molang.ast;

import com.eliotlash.molang.functions.Function;
import com.eliotlash.molang.functions.FunctionDefinition;
import com.eliotlash.molang.utils.MolangUtils;
import com.eliotlash.molang.variables.ExecutionContext;
import com.eliotlash.molang.variables.RuntimeVariable;

import java.util.List;

public class Evaluator implements Expr.Visitor<Double>, Stmt.Visitor<Void> {
    /**
     * A global evaluator with a global context that is not tied to a specific entity
     */
    private static ExecutionContext globalContext;
    private static Evaluator globalEvaluator;

    static {
        globalContext = new ExecutionContext(globalEvaluator);
        globalEvaluator = new Evaluator();
        globalEvaluator.setExecutionContext(globalContext);
    }

    private ExecutionContext context;

    public static Evaluator getGlobalEvaluator() {
        return globalEvaluator;
    }

    public static ExecutionContext getGlobalContext() {
        return globalContext;
    }

    public void setExecutionContext(ExecutionContext context) {
        this.context = context;
    }

    @Override
    public Void visitExpression(Stmt.Expression stmt, StmtContext ctx) {
        ctx.lastExprValue = evaluate(stmt.expr());
        return null;
    }

    @Override
    public Void visitReturn(Stmt.Return stmt, StmtContext ctx) {
        ctx.returnValue = evaluate(stmt.value());
        return null;
    }

    @Override
    public Void visitBreak(Stmt.Break stmt, StmtContext ctx) {
        return null;
    }

    @Override
    public Void visitContinue(Stmt.Continue stmt, StmtContext ctx) {
        return null;
    }

    @Override
    public Void visitLoop(Stmt.Loop stmt, StmtContext ctx) {
        Double count = evaluate(stmt.count());
        for (int i = 0; i < count; i++) {
            evaluate(stmt.expr());
        }
        return null;
    }

    @Override
    public Void visitIf(Stmt.If stmt, StmtContext stmtContext) {
        boolean hasHitBranch = false;

        //evaluate if
        if (MolangUtils.doubleToBoolean(evaluate(stmt.condition()))) {
            evaluate(stmt.body().statements(), stmtContext);
            hasHitBranch = true;
        }

        //evaluate elifs
        for (Stmt.If elif : stmt.elifs()) {
            if (!hasHitBranch) {
                if (MolangUtils.doubleToBoolean(evaluate(elif.condition()))) {
                    evaluate(elif.body().statements(), stmtContext);
                    hasHitBranch = true;
                }
            }
        }

        if (!hasHitBranch && stmt.elseBlock() != null) {
            evaluate(stmt.elseBlock().statements(), stmtContext);
        }

        return null;
    }

    @Override
    public Double visitAccess(Expr.Access expr) {
        if (context.assignableMap.containsKey(expr)) {
            return context.assignableMap.getDouble(expr);
        }
        if (context.functionScopedArguments.containsKey(expr)) {
            return context.functionScopedArguments.getDouble(expr);
        }
        if (expr.target() instanceof Expr.Variable variable) {
            RuntimeVariable cachedVariable = context.getCachedVariable(variable.flavor(), expr.member());
            if (context.getVariableMap().containsKey(cachedVariable)) {
                return context.getVariableMap().getDouble(cachedVariable);
            }
        }
        if (expr.target() instanceof Expr.Struct struct) {
            if (context.getStructMap().containsKey(struct)) {
                return context.getStructMap().getDouble(struct);
            }
        }
        return null;
    }

    @Override
    public Double visitAssignment(Expr.Assignment expr) {
        double value = evaluate(expr.expression());
        if (expr.variable() instanceof Expr.Access access) {
            if (access.target() instanceof Expr.Variable target) {
                context.assignableMap.put(access, value);
                context.parseRuntimeVariable(target.flavor(), target.name(), access);
            }
            else if (access.target() instanceof Expr.Struct struct) {
                context.getStructMap().put(struct, value);
            }
            else {
                throw new RuntimeException("Unexpected assignment to non variable/struct.");
            }
            return value;
        }
        return 0d;
    }

    @Override
    public Double visitBinOp(Expr.BinOp expr) {

        // Evaluate binops on strings if the expressions are strings
        if (expr.left() instanceof Expr.Str lhs && expr.right() instanceof Expr.Str rhs) {
            return expr.operator().applyString(lhs.val(), rhs.val());
        }
        return expr.operator().apply(() -> evaluate(expr.left()), () -> evaluate(expr.right()));
    }

    @Override
    public Double visitBlock(Expr.Block expr) {
        return evaluate(expr.statements());
    }

    @Override
    public Double visitCall(Expr.Call expr) {
        FunctionDefinition functionDefinition = new FunctionDefinition(expr.target(), expr.member());
        Function function = this.context.getFunction(functionDefinition);
        if (function == null) {
            return 0.0;
        } else {
            try {
                return function.evaluate(expr.arguments().toArray(Expr[]::new), context);
            } catch (Exception e) {
                return 0.0;
            }
        }
    }

    @Override
    public Double visitCoalesce(Expr.Coalesce expr) {
        var value = evaluateNullable(expr.value());
        return value == null ? evaluate(expr.fallback()) : value;
    }

    @Override
    public Double visitConstant(Expr.Constant expr) {
        return expr.value();
    }

    @Override
    public Double visitGroup(Expr.Group expr) {
        return evaluate(expr.value());
    }

    @Override
    public Double visitNegate(Expr.Negate expr) {
        Double evaluate = evaluate(expr.value());
        return -evaluate;
    }

    @Override
    public Double visitNot(Expr.Not expr) {
        return evaluate(expr.value()) == 0 ? 1.0 : 0.0;
    }

    @Override
    public Double visitConditional(Expr.Conditional expr) {
        double result = evaluate(expr.condition());
        if (result == 0) return 0.0;
        return evaluate(expr.ifTrue());
    }

    @Override
    public Double visitTernary(Expr.Ternary expr) {
        Expr branch = evaluate(expr.condition()) == 0 ? expr.ifFalse() : expr.ifTrue();
        return evaluate(branch);
    }

    @Override
    public Double visitSwitchContext(Expr.SwitchContext expr) {
        context.contextStack.push(expr.left());
        Double value = evaluate(expr.right());
        context.contextStack.pop();
        return value;
    }

    @Override
    public Double visitVariable(Expr.Variable expr) {
        RuntimeVariable runtimeVariable = context.getCachedVariable(expr.flavor(), expr.name());
        return context.getVariableMap().getOrDefault(runtimeVariable, 0);
    }

    @Override
    public String visitString(Expr.Str str) {
        return str.val();
    }

    public Double evaluate(Expr expr) {
        Double result = expr.accept(this);
        return result == null ? 0 : result;
    }

    public String evaluateString(Expr expr) {
        if (expr instanceof Expr.Str) {
            return ((Expr.Str) expr).val();
        }
        return expr.accept(this).toString();
    }

    public Double evaluateNullable(Expr expr) {
        return expr.accept(this);
    }

    public double evaluate(List<Stmt> stmts) {
        StmtContext ctx = new StmtContext();
        return evaluate(stmts, ctx);
    }

    public double evaluate(List<Stmt> stmts, StmtContext ctx) {
        for (Stmt stmt : stmts) {
            //early return
            if (ctx.returnValue != null) {
                return ctx.returnValue;
            }
            evaluate(stmt, ctx);
        }
        if (ctx.returnValue != null) {
            return ctx.returnValue;
        }

        if (ctx.lastExprValue != null) {
            return ctx.lastExprValue;
        }

        return 0.0;
    }

    private void evaluate(Stmt stmt, StmtContext ctx) {
        stmt.accept(this, ctx);
    }

    public ExecutionContext getContext() {
        return context;
    }
}
