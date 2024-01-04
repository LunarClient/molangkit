package com.eliotlash.molang.ast;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Stream;

import com.eliotlash.molang.Molang;
import com.eliotlash.molang.variables.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.eliotlash.molang.TestBase;

class EvaluatorTest extends TestBase {

	private Evaluator eval;

	@BeforeEach
	void setUp() throws Exception {
		eval = new Evaluator();
		eval.setExecutionContext(new ExecutionContext(eval));
	}

	@Test
	void visitAccess() {
		Expr.Access acc = access("temp", "test");
		eval.visitAssignment(new Expr.Assignment(acc, new Expr.Constant(5)));
		assertEquals(5, eval.visitAccess(acc));
	}

	@Test
	void visitCoallesce() {
		double value1 = evaluateMultiline("c.x = c.x ?? 5; return c.x;");
		double value2 = evaluateMultiline("c.x = 6; c.x = c.x ?? 5; return c.x;");
		assertEquals(5, value1);
		assertEquals(6, value2);
	}

	@Test
	void visitAssignment() {
		Expr.Access acc = access("query", "test");
		assertEquals(5, eval.visitAssignment(new Expr.Assignment(acc, new Expr.Constant(5))));
	}

	@Test
	void visitCall() {
		assertEquals(2, eval.visitCall(new Expr.Call(v("math"), "min", List.of(c(2), c(5)))));
		eval.evaluate(parseE("query.head_y_rotation = 5"));
		assertEquals(-30, eval.evaluate(parseE("math.clamp(-5*6,-30,30)")));
	}

	@Test
	void visitConstant() {
		assertEquals(20, eval.visitConstant(c(20)));
	}

	@Test
	void visitGroup() {
		assertEquals(20, eval.visitGroup(paren(c(20))));
	}

	@Test
	void visitVariable() {
		assertEquals(0, eval.visitVariable(new Expr.Variable("query.test")));

		ExecutionContext context = new ExecutionContext(eval);
		eval.setExecutionContext(context);
		context.setVariable("q.test", 5);
		context.setVariable("query.me", 5);
		context.setVariable("hello", 5);
		assertEquals(5, eval.visitVariable(new Expr.Variable("query.test")));
		assertEquals(5, eval.visitVariable(new Expr.Variable("q.test")));
		assertEquals(5, eval.visitVariable(new Expr.Variable("q.me")));
		assertEquals(5, eval.visitVariable(new Expr.Variable("hello")));
	}

	@Test
	void visitNegate() {
		assertEquals(-20, eval.visitNegate(new Expr.Negate(c(20))));
	}

	@Test
	void visitNot() {
		assertEquals(0, eval.visitNot(new Expr.Not(c(20))));
		assertEquals(1, eval.visitNot(new Expr.Not(c(0))));
	}

	@Test
	void visitConditional() {
		assertEquals(10, eval.visitConditional(new Expr.Conditional(c(1), c(10))));
		assertEquals(0, eval.visitConditional(new Expr.Conditional(c(0), c(10))));
	}

	@Test
	void visitTernary() {
		assertEquals(10, eval.visitTernary(new Expr.Ternary(c(1), c(10), c(30))));
		assertEquals(30, eval.visitTernary(new Expr.Ternary(c(0), c(10), c(30))));
	}

	@Test
	void visitReturn() {
		//multiplication with return
		Stmt s1 = parseS("t.x = 5;");
		Stmt s2 = parseS("t.y = 7;");
		Stmt s3 = parseS("return t.x * t.y;");
		assertEquals(35, eval.evaluate(List.of(s1, s2, s3)));

		System.out.println(evaluateMultiline("""
				v.x = 1;
				v.y = 1;
				loop(6, {
				  t.x = v.x + v.y;
				  v.x = v.y;
				  v.y = t.x;
				});
				t.x;
				"""));
	}

	@Test
	void visitIf() {
		Double ifValue = evaluateMultiline("""
				if (5 * 5 == 24) {
					return 7 + 3;
				}
				if(5 * 4 == 20) {
					return 7 * 3;
				}
				""");
		assertEquals(21, ifValue);

		Double elifValue1 = evaluateMultiline("""
				if (1 + 1 == 2) {
					temp.test =  7 + 3;
				}
				elif (1 + 1 == 3) {
					temp.test = 7 * 3;
				}
				elif (1 + 1 == 4) {
					temp.test = 7 * 1;
				}
				else {
					temp.test = 100;
				}
				return temp.test;
				""");
		assertEquals(10, elifValue1);

		Double elifValue2 = evaluateMultiline("""
				if (1 + 1 == 0) {
					temp.test =  7 + 3;
				}
				elif (1 + 1 == 2) {
					temp.test = 7 * 3;
				}
				elif (1 + 1 == 4) {
					temp.test = 7 * 1;
				}
				else {
					temp.test = 100;
				}
				return temp.test;
				""");
		assertEquals(21, elifValue2);

		Double elifValue3 = evaluateMultiline("""
				if (1 + 1 == 0) {
					temp.test =  7 + 3;
				}
				elif (1 + 1 == 2) {
					temp.test = 7 * 3;
				}
				elif (1 + 1 == 2) {
					temp.test = 7 * 1;
				}
				else {
					temp.test = 100;
				}
				return temp.test;
				""");
		assertEquals(21, elifValue3);

		Double elifValue4 = evaluateMultiline("""
				if (1 + 1 == 0) {
					temp.test =  7 + 3;
				}
				elif (1 + 1 == 4) {
					temp.test = 7 * 3;
				}
				elif (1 + 1 == 9) {
					temp.test = 1800;
				}
				elif (1 + 1 == 2) {
					temp.test = 7 * 1;
				}
				elif (1 + 1 == 90) {
					temp.test = 1800;
				}
				else {
					temp.test = 100;
				}
				return temp.test;
				""");
		assertEquals(7, elifValue4);

		Double elifValue5 = evaluateMultiline("""
				if (1 + 1 == 0) {
					temp.test =  7 + 3;
				}
				elif (1 + 1 == 4) {
					temp.test = 7 * 3;
				}
				elif (1 + 1 == 6) {
					temp.test = 7 * 1;
				}
				else {
					temp.test = 100;
				}
				return temp.test;
				""");
		assertEquals(100, elifValue5);
	}

	@Test
	void visitStruct() {
		double v = evaluateMultiline("""
				animal.cow.head = 5;
				animal.cow.leg = 3;
				return animal.cow.leg * animal.cow.head;
				""");
		assertEquals(v, 15);
	}


	public double evaluateMultiline(String multiline) {
		return eval.evaluate(Molang.parse(multiline));
	}

	/**
	 * Parses the input as an expression.
	 */
	Expr parseE(String expr) {
		return Molang.parseExpression(expr);
	}

	/**
	 * Parses the input as a statement.
	 */
	Stmt parseS(String expr) {
		return Molang.parseSingle(expr);
	}

	public static Stream<BinOpTest> binOpTestProvider() {
		return BinOpTest.provider();
	}

	@ParameterizedTest
	@MethodSource("binOpTestProvider")
	void visitBinOp(BinOpTest args) {
		Double result = new Expr.BinOp(args.op(), c(args.lhs()), c(args.rhs())).accept(eval);
		assertEquals(args.expectedResult(), result, 0.000001);
	}


	@Test
	void stringEqualityTest() {
		// the binops != & == use String#equals behind the scenes
		assertEquals(1.0, eval.evaluate(parseE("'minecraft:pig' != 'minecraft:cow'")));
		assertEquals(0.0, eval.evaluate(parseE("'minecraft:pig' == 'minecraft:cow'")));
		assertEquals(1.0, eval.evaluate(parseE("'minecraft:pig' != ''")));
		assertEquals(1.0, eval.evaluate(parseE("'' == ''")));
	}
}
