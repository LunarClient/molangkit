package com.eliotlash.molang.utils;

import com.eliotlash.molang.ast.Expr;
import com.eliotlash.molang.variables.VariableFlavor;

public class ParserUtils {
    public static Expr.Variable createVariableFromString(String string) {
        if (VariableFlavor.parse(string) == null) {
            return new Expr.Variable(null, string);
        } else {
            return new Expr.Variable(VariableFlavor.parse(string), null);
        }
    }
}