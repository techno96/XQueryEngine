package edu.ucsd.CSE232B.visitor;

import edu.ucsd.CSE232B.parsers.XQueryGrammarParser;
import edu.ucsd.CSE232B.parsers.XQueryGrammarVisitor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class XQueryModifiedVisitor extends XQueryGrammarVisitor<List<Node>> {

    static Document output = null;

    @Override
    public List<Node> visitXQueryVariable(XQueryGrammarParser.XQueryVariableContext ctx) {

    }

    @Override
    public List<Node> visitXqConstructor(XQueryGrammarParser.XqConstructorContext ctx) {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException exception) {
            exception.printStackTrace();
        }
        output = builder.newDocument();
        final List<Node> xqNodes = visit(ctx.xq());
        //print results

        //double check reference
        return new ArrayList<>(Arrays.asList(createElement(ctx.NAME(0).getText(), xqNodes)));
    }

    private static Node createElement(String tag, List<Node> nodes) {
        // double check logic
        Node finalNode = output.createElement(tag);
        for (Node n : nodes) {
            if (n != null) {
                Node newNode = output.importNode(n, true);
                finalNode.appendChild(newNode);
            }
        }

        return finalNode;
    }

    @Override
    public List<Node> visitXQueryWithParan(XQueryGrammarParser.XQueryWithParanContext ctx) {
        return visit(ctx.xq());
    }

    @Override
    public List<Node> visitXQuerycomma(XQueryGrammarParser.XQuerycommaContext ctx) {
        List<Node> result = visit(ctx.xq(0));
        result.addAll(visit(ctx.xq(1)));
        return result;
    }

    @Override
    public List<Node> visitXQueryAP(XQueryGrammarParser.XQueryAPContext ctx) {
        return visit(ctx.ap());
    }

}
