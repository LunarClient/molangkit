package com.eliotlash.molang;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.eliotlash.molang.ast.*;

public class ParserTest extends TestBase {

	@Test
	void testString() {
		assertEquals(new Expr.Str("minecraft:pig"), e("'minecraft:pig'"));
		assertEquals(new Expr.Str("hello, world. this is a string"), e("'hello, world. this is a string'"));
		// empty strings work
		assertEquals(new Expr.Str(""), e("''"));

		assertThrows(Exception.class, () -> {
			e("'this string is not closed              ");
		});

		assertThrows(Exception.class, () -> {
			e("'''");
		});


		assertThrows(Exception.class, () -> {
			e("'");
		});

		assertEquals(new Expr.Str("this string is was closed              "), e("'this string is was closed              '"));
	}
	@Test
	void testStmt() {
		Expr setThing = call("v", "set_thing", access("q", "thing"));

		assertEquals(new Stmt.Expression(setThing), s("v.set_thing(q.thing);"));
		assertThrows(ParseException.class, () -> s("v.set_thing(q.thing)"));

		assertEquals(new Stmt.Return(c(20)), s("return 20;"));
		assertThrows(ParseException.class, () -> s("return 20"));

		assertEquals(new Stmt.Loop(c(20), setThing), s("loop(20, v.set_thing(q.thing));"));
		assertThrows(ParseException.class, () -> s("loop(20, v.set_thing(q.thing))"));

		assertEquals(new Stmt.Break(), s("break;"));
		assertThrows(ParseException.class, () -> s("break"));

		assertEquals(new Stmt.Continue(), s("continue;"));
		assertThrows(ParseException.class, () -> s("continue"));

		assertThrows(ParseException.class, () -> s("if(5 * 5)"));


	}

	@Test
	void testNumber() {
		assertEquals(c(20), e("20"));
		assertEquals(new Expr.Negate(c(20)), e("-20"));
	}

	@Test
	void testOperators() {
		for (Operator value : Operator.values()) {
			assertEquals(new Expr.BinOp(value, c(20), c(20)), e("20 " + value.sign + " 20"));
		}
	}

	@Test
	void testConditionals() {
		// Binary Conditional
		assertEquals(new Expr.Conditional(c(0), c(20)), e("0 ? 20"));
		assertEquals(
				new Expr.Conditional(c(0), paren(new Expr.Conditional(c(20), c(30)))),
				e("0 ? (20 ? 30)")
		);
		// Ternary Conditional
		assertEquals(new Expr.Ternary(c(0), c(10), c(20)), e("0 ? 10 : 20"));
		assertEquals(
				new Expr.Ternary(c(0), paren(new Expr.Ternary(c(10), c(20), c(30))), c(20)),
				e("0 ? (10 ? 20 : 30) : 20")
		);
		// Combinations
		assertEquals(
				new Expr.Conditional(c(0), new Expr.Ternary(c(10), c(20), c(30))),
				e("0 ? 10 ? 20 : 30")
		);
		assertEquals(
				new Expr.Ternary(c(0), c(10), new Expr.Conditional(c(20), c(30))),
				e("0 ? 10 : 20 ? 30")
		);
	}

	@Test
	void testMath() {
		Expr.Constant twenty = c(20);
		assertEquals(op(twenty, Operator.MUL, paren(op(twenty, Operator.ADD, twenty))), e("20 * (20 + 20)"));
		assertEquals(op(op(twenty, Operator.MUL, twenty), Operator.ADD, twenty), e("20 * 20 + 20"));
		assertEquals(op(paren(op(twenty, Operator.MUL, twenty)), Operator.ADD, twenty), e("(20 * 20) + 20"));
	}

	@Test
	void testPrecedence() {
		Expr.Constant one = c(1);
		Expr.Variable a = v("a");
		Expr.Access acc = access("q", "test");

		// assignment/disjunction
		assertEquals(new Expr.Assignment(acc, op(one, Operator.OR, one)), e("q.test = 1 || 1"));

		// disjunction/conjunction
		assertEquals(op(one, Operator.OR, op(one, Operator.AND, one)), e("1 || 1 && 1"));
		assertEquals(op(op(one, Operator.AND, one), Operator.OR, one), e("1 && 1 || 1"));

		// conjunction/equality
		assertEquals(op(one, Operator.AND, op(one, Operator.EQ, one)), e("1 && 1 == 1"));
		assertEquals(op(op(one, Operator.NEQ, one), Operator.AND, one), e("1 != 1 && 1"));

		// equality/comparison
		assertEquals(op(one, Operator.EQ, op(one, Operator.LT, one)), e("1 == 1 < 1"));
		assertEquals(op(op(one, Operator.GT, one), Operator.EQ, one), e("1 > 1 == 1"));

		// comparison/coalesce
		assertEquals(new Expr.Coalesce(op(one, Operator.LT, a), one), e("1 < a ?? 1"));
		assertEquals(new Expr.Coalesce(a, op(one, Operator.LT, one)), e("a ?? 1 < 1"));

		// coalesce/term
		assertEquals(new Expr.Coalesce(a, op(one, Operator.SUB, one)), e("a ?? 1 - 1"));
		assertEquals(new Expr.Coalesce(op(one, Operator.ADD, a), one), e("1 + a ?? 1"));

		// term/factor
		assertEquals(op(op(one, Operator.MUL, one), Operator.ADD, one), e("1 * 1 + 1"));
		assertEquals(op(one, Operator.SUB, op(one, Operator.DIV, one)), e("1 - 1 / 1"));

		// factor/exponentiation
		assertEquals(op(op(one, Operator.POW, one), Operator.MUL, one), e("1 ^ 1 * 1"));
		assertEquals(op(one, Operator.DIV, op(one, Operator.POW, one)), e("1 / 1 ^ 1"));

		// exponentiation/unary
		assertEquals(op(one, Operator.POW, new Expr.Negate(one)), e("1 ^ -1"));
		assertEquals(op(new Expr.Negate(one), Operator.POW, new Expr.Negate(one)), e("-1 ^ -1"));
		assertEquals(op(new Expr.Negate(one), Operator.POW, one), e("-1 ^ 1"));

		// ternary
		assertEquals(
				new Expr.Ternary(
						op(a, Operator.GT, c(1)),
						c(1),
						c(0)
				),
				e("a > 1 ? 1 : 0")
		);
		assertEquals(
				new Expr.Ternary(
						op(a, Operator.GT, c(1)),
						new Expr.Ternary(
								op(a, Operator.LT, c(1)),
								c(0),
								c(1)
						),
						new Expr.Ternary(
								op(a, Operator.LT, c(1)),
								c(0),
								c(1)
						)
				),
				e("a > 1 ? a < 1 ? 0 : 1 : a < 1 ? 0 : 1")
		);

	}

	@Test
	void testCoalesce() {
		assertEquals(new Expr.Coalesce(access("q", "test1"), access("q", "test2")), e("q.test1 ?? q.test2"));
		assertEquals(op(c(2), Operator.ADD, paren(new Expr.Coalesce(access("q", "test1"), access("q", "test2")))), e("2 + (q.test1 ?? q.test2)"));
	}

	@Test
	void testGrouping() {
		assertEquals(new Expr.Group(c(20)), e("(20)"));
		assertEquals(new Expr.Negate(new Expr.Group(new Expr.Negate(c(20)))), e("-(-20)"));

		assertThrows(ParseException.class, () -> e("(20"));
		assertThrows(ParseException.class, () -> e("20)"));
		assertThrows(ParseException.class, () -> e("(((((((20))))))"));
	}

	@Test
	void testVariables() {
		assertEquals(v("test"), e("test"));
		assertEquals(v("test_other"), e("test_other"));
	}

	@Test
	void testAccess() {
		assertEquals(access("query", "test"), e("query.test"));
		assertEquals(access("q", "test"), e("q.test"));
	}

	@Test
	void testAssignment() {
		Expr.Constant twenty = c(20);
		Expr.Access acc = access("query", "test");

		assertEquals(new Expr.Assignment(acc, twenty), e("query.test = 20"));
		assertEquals(new Expr.Assignment(acc, new Expr.Assignment(acc, twenty)), e("query.test = query.test = 20"));
		assertEquals(new Expr.Assignment(acc, op(twenty, Operator.ADD, twenty)), e("query.test = 20 + 20"));
		assertThrows(ParseException.class, () -> e("fail = 20"));
		assertThrows(ParseException.class, () -> e("20 = 20"));
		assertThrows(ParseException.class, () -> e("(query.fail) = 20"));

		Expr.Access ridingAcc = access("query", "riding");
		assertEquals(new Expr.Assignment(ridingAcc, new Expr.Str("minecraft:pig")), e("query.riding = 'minecraft:pig'"));
	}

	@Test
	void testCall() {
		assertEquals(new Expr.Call(v("q"), "count", List.of(c(20))), e("q.count(20)"));
		assertEquals(new Expr.Call(
				v("q"),
				"count",
				List.of(c(20),
						c(40),
						op(c(20), Operator.MUL, c(40)))
		), e("q.count(20, 40, 20 * 40)"));

		Expr.Call stringCall = new Expr.Call(v("q"), "str", List.of(new Expr.Str("hello, world"), new Expr.Str("minecraft:pig")));

		assertEquals(stringCall, e("q.str('hello, world', 'minecraft:pig')"));

		assertThrows(Exception.class, () -> {
			e("q.str('hello, world', 'minecraft:pig)");
		});
	}

	@Test
	void testBlock() {
		assertEquals(new Expr.Block(List.of()), e("{ }"));
		assertEquals(new Expr.Block(List.of(new Stmt.Expression(new Expr.Block(List.of())))), e("{ { }; }"));

		Expr.Access tTest = access("t", "test");
		List<Stmt> stmts = List.of(
				new Stmt.Expression(new Expr.Assignment(tTest, c(20))),
				new Stmt.Return(tTest)
		);
		assertEquals(new Expr.Block(stmts), e("{ t.test = 20; return t.test; }"));
	}

	@Test
	void testStruct() {
		Expr e = e("animal.cow.head.testdfsdf.sdf = 5");
		System.out.println(e);
	}

	/**
	 * Parses the input as an expression.
	 */
	Expr e(String expr) {
		return Molang.parseExpression(expr);
	}

	/**
	 * Parses the input as a statement.
	 */
	Stmt s(String expr) {
		return Molang.parseSingle(expr);
	}
}
