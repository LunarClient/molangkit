package com.eliotlash.molang.lexer;

import java.util.ArrayList;
import java.util.List;

public class Lexer {

	private final String input;
	private final List<Token> tokens = new ArrayList<>();

	/**
	 * The first index of the current token.
	 */
	private int startPos;
	/**
	 * The lexer's current position in the input.
	 */
	private int nextPos = 0;

	public static List<Token> tokenize(String input) {
		return new Lexer(input).scanTokens();
	}

	public Lexer(String input) {
		this.input = input;
	}

	private boolean hasNextChar() {
		return nextPos < input.length();
	}

	/**
	 * Peeks the next character in the input.
	 * @return The next character in the input.
	 */
	private char peek() {
		if (nextPos >= input.length()) return '\0';
		return input.charAt(nextPos);
	}

	private char peekNext() {
		if (nextPos + 1 >= input.length()) return '\0';
		return input.charAt(nextPos + 1);
	}

	/**
	 * Advances the lexer to the next character.
	 */
	private char advance() {
		return input.charAt(nextPos++);
	}

	public List<Token> scanTokens() {
		while (hasNextChar()) {
			startPos = nextPos;
			scanToken();
		}

		tokens.add(new Token(TokenType.EOF, ""));
		return tokens;
	}

	private void scanToken() {
		char c = advance();

		if (Character.isWhitespace(c)) {
			eatWhitespace();
			return;
		}



		var token = tryOperator(c);
		if (token != null) {
			if (token == TokenType.QUOTE) {
				try {
					eatString();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				return;
			}

			setToken(token);
		} else if (isDigit(c)) {
			eatNumeral();
		} else if (isIdentifierStart(c)) {
			eatIdentifier();
		}
	}


	private void eatString() throws Exception {
		// skip over the current character ('), we don't want that to part of the string.
		// The lexer seems to always include the startPos.
		// Other option would be to make it like a state machine where if it comes across a quote
		// it'll consume everything within it until it reaches another quote.
		// But that would be behavior specific to Strings, and doesn't fit with
		// current behavior for numerals.
		startPos++;

		// while we have a next character, and the next character does not equal "'"
		// we are safe to advance.
		while (hasNextChar() && peek() != '\'') {
			advance();
		}

		// Early fail for unclosed strings
		if (!hasNextChar() || peek() != '\'') {
			System.out.println(peek());
			// We want to skip over the closing quote.
			// We don't however want to include it in the substring, so we skip over, after we set token.
			// However, if we find that there isn't another quote, we mark it as an unclosed string
			throw new Exception("string not closed");
		}

		setToken(TokenType.STRING);


		// Skip over the next quote so the lexer doesn't pick it up as a new string.
		advance();
	}

	private void eatWhitespace() {
		while (Character.isWhitespace(peek())) {
			advance();
		}
	}

	private void eatNumeral() {
		while (isDigit(peek())) {
			advance();
		}

		if (peek() == '.' && isDigit(peekNext())) {
			advance();

			while (isDigit(peek())) {
				advance();
			}
		}

		setToken(TokenType.NUMERAL);
	}

	private void eatIdentifier() {
		while (isIdentifier(peek())) {
			advance();
		}

		setToken(TokenType.IDENTIFIER);
	}

	private TokenType tryOperator(char c) {
		switch (c) {
			case '!' -> {
				if (match('=')) {
					return TokenType.BANG_EQUAL;
				}
				return TokenType.NOT;
			}
			case '=' -> {
				if (match('=')) {
					return TokenType.EQUAL_EQUAL;
				}
				return TokenType.EQUALS;
			}
			case '<' -> {
				if (match('=')) {
					return TokenType.LESS_EQUAL;
				}
				return TokenType.LESS_THAN;
			}
			case '>' -> {
				if (match('=')) {
					return TokenType.GREATER_EQUAL;
				}
				return TokenType.GREATER_THAN;
			}
			case '&' -> {
				if (match('&')) {
					return TokenType.AND;
				}
				return TokenType.AND;
			}
			case '|' -> {
				if (match('|')) {
					return TokenType.OR;
				}
				return TokenType.OR;
			}
			case '-' -> {
				if (match('>')) {
					return TokenType.ARROW;
				}
				return TokenType.MINUS;
			}
			case '?' -> {
				if (match('?')) {
					return TokenType.COALESCE;
				}
				return TokenType.QUESTION;
			}
		}

		return switch (c) {
			case '^' -> TokenType.CARET;
			case '+' -> TokenType.PLUS;
			case '*' -> TokenType.STAR;
			case '/' -> TokenType.SLASH;
			case '%' -> TokenType.PERCENT;
			case '(' -> TokenType.OPEN_PAREN;
			case ')' -> TokenType.CLOSE_PAREN;
			case '{' -> TokenType.OPEN_BRACE;
			case '}' -> TokenType.CLOSE_BRACE;
			case '[' -> TokenType.OPEN_BRACKET;
			case ']' -> TokenType.CLOSE_BRACKET;
			case ',' -> TokenType.COMMA;
			case ';' -> TokenType.SEMICOLON;
			case ':' -> TokenType.COLON;
			case '.' -> TokenType.DOT;
			case '\'' -> TokenType.QUOTE;
			default -> null;
		};
	}

	private boolean match(char c) {
		if (hasNextChar() && peek() == c) {
			advance();
			return true;
		}
		return false;
	}

	private void setToken(TokenType tokenType) {
		tokens.add(new Token(tokenType, input.substring(this.startPos, nextPos)));
	}

	/**
	 * Checks if the given character can be used in an identifier.
	 * @param c The character to check.
	 * @return {@code true} if the character can be used in an identifier, {@code false} otherwise.
	 */
	private static boolean isIdentifier(char c) {
		return Character.isAlphabetic(c) || isDigit(c) || c == '_';
	}

	/**
	 * Checks if the given character can be used to begin an identifier.
	 * @param c The character to check.
	 * @return {@code true} if the character can be used to begin an identifier, {@code false} otherwise.
	 */
	private static boolean isIdentifierStart(char c) {
		return Character.isAlphabetic(c) || c == '_';
	}

	private static boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}
}
