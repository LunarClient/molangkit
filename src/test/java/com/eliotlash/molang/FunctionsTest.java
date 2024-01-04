package com.eliotlash.molang;

import com.eliotlash.molang.ast.Evaluator;
import com.eliotlash.molang.ast.Expr;
import com.eliotlash.molang.ast.Transformations;
import com.eliotlash.molang.variables.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FunctionsTest {

    private Evaluator evaluator;

    @BeforeEach
    private void setupEval() {
        evaluator = new Evaluator();
        evaluator.setExecutionContext(new ExecutionContext(evaluator));
    }

    @Test
    void testConstant() throws Exception {
        assertConstant("PI");
        assertConstant("E");
        assertConstant("500");
        assertConstant("math.sqrt(2)");
        assertConstant("math.floor(2.5)");
        assertConstant("math.round(2.5)");
        assertConstant("math.ceil(2.5)");
        assertConstant("math.trunc(2.5)");
        assertConstant("math.clamp(10, 0, 1)");
        assertConstant("math.max(1, 2)");
        assertConstant("math.min(1, 2)");
        assertConstant("math.abs(-20)");
        assertConstant("math.cos(-20)");
        assertConstant("math.sin(60)");
        assertConstant("math.exp(5)");
        assertConstant("math.ln(E)");
        assertConstant("math.mod(10, 3)");
        assertConstant("math.pow(20, 2)");
        assertNotConstant("math.sqrt(v.val)");
        assertNotConstant("math.floor(v.val)");
        assertNotConstant("math.round(v.val)");
        assertNotConstant("math.ceil(v.val)");
        assertNotConstant("math.trunc(v.val)");
        assertNotConstant("math.clamp(10, v.val, 1)");
        assertNotConstant("math.max(1, v.val)");
        assertNotConstant("math.min(v.val, 2)");
        assertNotConstant("math.abs(-v.val)");
        assertNotConstant("math.cos(v.val)");
        assertNotConstant("math.sin(v.val)");
        assertNotConstant("math.exp(v.val)");
        assertNotConstant("math.ln(v.val)");
        assertNotConstant("math.mod(10, v.val)");
        assertNotConstant("math.pow(v.val, 2)");
    }

    @Test
    void testNotConstant() throws Exception {
        assertNotConstant("math.random()");
        assertNotConstant("math.sin(math.random())");
    }

    @Test
    void testTrig() throws Exception {
        assertEquals(1.0, evaluate("math.sin(90)"), 0.0001);
        assertEquals(0.0, evaluate("math.sin(0)"), 0.0001);

        assertEquals(0.0, evaluate("math.cos(90)"), 0.0001);
        assertEquals(1.0, evaluate("math.cos(0)"), 0.0001);
    }

    @Test
    void testRounding() throws Exception {
        assertEquals(5.0, evaluate("math.floor(5.4)"));
        assertEquals(5.0, evaluate("math.floor(5.9)"));

        assertEquals(5.0, evaluate("math.round(5.1)"));
        assertEquals(5.0, evaluate("math.round(4.9)"));

        assertEquals(5.0, evaluate("math.ceil(4.1)"));
        assertEquals(5.0, evaluate("math.ceil(4.9)"));

        assertEquals(5.0, evaluate("math.trunc(5.1)"));
        assertEquals(-5.0, evaluate("math.trunc(-5.1)"));
    }

    @Test
    void testSelection() throws Exception {
        assertEquals(0.0, evaluate("math.clamp(-1, 0, 1)"));
        assertEquals(1.0, evaluate("math.clamp(20, 0, 1)"));
        assertEquals(0.5, evaluate("math.clamp(0.5, 0, 1)"));

        assertEquals(1.0, evaluate("math.max(0, 1)"));
        assertEquals(1.0, evaluate("math.max(1, 0)"));
        assertEquals(1.0, evaluate("math.max(1, -10)"));
        assertEquals(-20.0, evaluate("math.max(-20, -70)"));

        assertEquals(0.0, evaluate("math.min(0, 1)"));
        assertEquals(0.0, evaluate("math.min(1, 0)"));
        assertEquals(-10.0, evaluate("math.min(1, -10)"));
        assertEquals(-70.0, evaluate("math.min(-20, -70)"));
    }

    @Test
    void testMiscFunctions() throws Exception {
        assertEquals(10.0, evaluate("math.abs(-10)"));
        assertEquals(10.0, evaluate("math.abs(10)"));

        assertEquals(Math.exp(1), evaluate("math.exp(1)"));
        assertEquals(0.0, evaluate("math.ln(1)"));
        assertEquals(2.0, evaluate("math.sqrt(4)"));
        assertEquals(2.0, evaluate("math.mod(5, 3)"));
        assertEquals(100.0, evaluate("math.pow(10, 2)"));

        assertEquals(1.0, evaluate("!string.equals('minecraft:pig', 'minecraft:cow')"));
        assertEquals(1.0, evaluate("string.equals('minecraft:cow', 'minecraft:cow')"));
        assertEquals(0.0, evaluate("string.equals('COW', 'cow')"));
        assertEquals(1.0, evaluate("string.equalsIgnoreCase('COW', 'cow')"));

        assertEquals(13, evaluate("string.length('minecraft:cow')"));
        assertEquals(0.0, evaluate("string.length('')"));
        assertEquals(10.0, evaluate("'I am nothing' + 10"));

        assertEquals(1.0, evaluate("system.print('\"1+1-1/1*1->([{9+1}]);\"')"));
        assertEquals(1.0, evaluate("system.print('test')"));
        assertEquals(1.0, evaluate("system.print('oh no' + 'math is hard')"));
    }

    private double evaluate(String expression) throws Exception {
        return evaluator.evaluate(Molang.parseExpression(expression));
    }

    private void assertConstant(String expression) throws Exception {
        Expr expr = Molang.parseExpression(expression)
                .accept(Transformations.SIMPLIFY_CONSTANTS);

        assertTrue(expr instanceof Expr.Constant);
    }

    private void assertNotConstant(String expression) throws Exception {
        Expr expr = Molang.parseExpression(expression)
                .accept(Transformations.SIMPLIFY_CONSTANTS);

        assertFalse(expr instanceof Expr.Constant);
    }
}
