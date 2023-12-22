package com.eliotlash.molang.ast;

import java.util.List;
import java.util.Objects;

public class EvaluatableStmt implements Evaluatable {

    private final List<Stmt> stmts;

    public EvaluatableStmt(List<Stmt> stmts) {
        this.stmts = Objects.requireNonNull(stmts);
    }

    @Override
    public double evaluate(Evaluator evaluator) {
        return evaluator.evaluate(this.stmts);
    }
}
