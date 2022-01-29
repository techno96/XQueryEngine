package edu.ucsd.CSE232B;

import edu.ucsd.CSE232B.visitor.XpathModifiedVisitor;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.ucsd.CSE232B.parsers.XpathGrammarLexer;
import edu.ucsd.CSE232B.parsers.XpathGrammarParser;
import org.w3c.dom.Node;

public class Main {
    public static void main(String[] args) {
        final ANTLRInputStream input;
        try {
            //TODO : Replace with FileInputStream
            input = new ANTLRInputStream(new FileInputStream("src/main/resources/queries.txt"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        final XpathGrammarLexer lexer = new XpathGrammarLexer(input);
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final XpathGrammarParser parser = new XpathGrammarParser(tokens);
        final ParseTree tree = parser.ap();
        final XpathModifiedVisitor visitor = new XpathModifiedVisitor();

        //TODO : For each query in queries file, run this
        final List<Node> nodes = visitor.visit(tree);
        System.out.println(nodes.size());
        for (Node n : nodes) {
            System.out.println(n.getTextContent() + '\n');
        }
    }
}