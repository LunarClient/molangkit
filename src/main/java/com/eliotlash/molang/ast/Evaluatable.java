package com.eliotlash.molang.ast;

import java.util.List;

public interface Evaluatable {

    double evaluate(Evaluator evaluator);

    default boolean isConstant() {
        return false;
    }

    default double getConstant() {
        return 0.0;
    }

    static Evaluatable of(Expr expression) {
        return new EvaluatableExpr(expression);
    }

    static Evaluatable of(List<Stmt> statements) {
        return new EvaluatableStmt(statements);
    }
}
