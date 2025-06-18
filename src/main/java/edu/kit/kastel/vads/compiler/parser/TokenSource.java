package edu.kit.kastel.vads.compiler.parser;

import edu.kit.kastel.vads.compiler.lexer.Ident;
import edu.kit.kastel.vads.compiler.lexer.Lexer;
import edu.kit.kastel.vads.compiler.lexer.Separator;
import edu.kit.kastel.vads.compiler.lexer.Separator.SeparatorType;
import edu.kit.kastel.vads.compiler.lexer.keywords.Keyword;
import edu.kit.kastel.vads.compiler.lexer.keywords.Keyword.KeywordType;
import edu.kit.kastel.vads.compiler.lexer.operators.Operator;
import edu.kit.kastel.vads.compiler.lexer.operators.Operator.OperatorType;
import edu.kit.kastel.vads.compiler.lexer.Token;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class TokenSource {

    private final List<Token> tokens;
    private int idx;

    public TokenSource(Lexer lexer) {
        this.tokens = Stream.generate(lexer::nextToken)
            .takeWhile(Optional::isPresent)
            .map(Optional::orElseThrow)
            .toList();
    }

    TokenSource(List<Token> tokens) {
        this.tokens = List.copyOf(tokens);
    }

    public Token peek() {
        expectHasMore();
        return this.tokens.get(this.idx);
    }

    public Keyword expectKeyword(KeywordType type) {
        Token token = peek();
        if (!(token instanceof Keyword kw) || kw.type() != type) {
            throw new ParseException("expected keyword '" + type + "' but got " + token);
        }
        this.idx++;
        return kw;
    }

    public Keyword expectKeyword() {
        Token token = peek();
        if (!(token instanceof Keyword kw)) {
            throw new ParseException("expected a keyword but got " + token);
        }
        this.idx++;
        return kw;
    }

    public Separator expectSeparator(SeparatorType type) {
        Token token = peek();
        if (!(token instanceof Separator sep) || sep.type() != type) {
            throw new ParseException("expected separator '" + type + "' but got " + token);
        }
        this.idx++;
        return sep;
    }

    public Separator expectSeparator() {
        Token token = peek();
        if (!(token instanceof Separator sep)) {
            throw new ParseException("expected a separator but got " + token);
        }
        this.idx++;
        return sep;
    }

    public Operator expectOperator(OperatorType type) {
        Token token = peek();
        if (!(token instanceof Operator op) || op.type() != type) {
            throw new ParseException("expected operator '" + type + "' but got " + token);
        }
        this.idx++;
        return op;
    }

    public Operator expectOperator() {
        Token token = peek();
        if (!(token instanceof Operator op)) {
            throw new ParseException("expected an operator but got " + token);
        }
        this.idx++;
        return op;
    }

    public Ident expectIdentifier() {
        Token token = peek();
        if (!(token instanceof Ident ident)) {
            throw new ParseException("expected identifier but got " + token);
        }
        this.idx++;
        return ident;
    }

    public Token consume() {
        Token token = peek();
        this.idx++;
        return token;
    }

    public boolean hasMore() {
        return this.idx < this.tokens.size();
    }

    private void expectHasMore() {
        if (this.idx >= this.tokens.size()) {
            throw new ParseException("reached end of file");
        }
    }
}
