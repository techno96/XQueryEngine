package edu.ucsd.CSE232B.visitor;

import edu.ucsd.CSE232B.parsers.XpathGrammarBaseVisitor;
import edu.ucsd.CSE232B.parsers.XpathGrammarParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class XpathModifiedVisitor extends XpathGrammarBaseVisitor<ArrayList<Node>> {
    ArrayList<Node> currentNodes = new ArrayList<>();

    @Override
    public ArrayList<Node> visitDoc(XpathGrammarParser.DocContext ctx) {
        final File inputXMLFile = new File(ctx.fileName().getText().replace('\"', ' ').strip());
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException exception) {
            exception.printStackTrace();
        }
        Document doc = null; //use for what
        try {
            if (builder != null) {
                doc = builder.parse(inputXMLFile);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        if (doc != null) {
            doc.getDocumentElement().normalize();
        }
        final ArrayList<Node> result = new ArrayList<>();
        result.add(doc);
        currentNodes = result;
        return result;
    }

    @Override
    public ArrayList<Node> visitChild(XpathGrammarParser.ChildContext ctx) {
        //double check logic
        visit(ctx.doc());
        return visitChildren(ctx);
    }

    @Override
    public ArrayList<Node> visitDescendant(XpathGrammarParser.DescendantContext ctx) {
        visit(ctx.doc());
        return visitChildren(ctx);
    }



}
