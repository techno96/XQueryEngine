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
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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

        Document outputDoc = null;
        try {
            DocumentBuilderFactory docBF = DocumentBuilderFactory.newInstance();
            DocumentBuilder docB = docBF.newDocumentBuilder();
            outputDoc = docB.newDocument();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return;
        }

        List<Node> result = createElement(visitor.doc, "result", nodes);
        writeNodesToFile(outputDoc, result, "output/XPath.xml");
    }

    public static void writeNodesToFile(Document doc, List<Node> result, String filePath) {
        Node newNode = doc.importNode(result.get(0), true);
        doc.appendChild(newNode);
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();

            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            DOMSource source = new DOMSource(doc);

            StreamResult terminal = new StreamResult(System.out);
            StreamResult fileOutput = new StreamResult(filePath);
            transformer.transform(source, terminal);
            transformer.transform(source, fileOutput);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static List<Node> createElement(Document doc, String tag, List<Node> nodes){
        List<Node> results = new ArrayList<>();
        Node finalNode = doc.createElement(tag);
        for (Node n : nodes) {
            if (n != null) {
                Node newNode = doc.importNode(n, true);
                finalNode.appendChild(newNode);
            }
        }
        results.add(finalNode);
        return results;
    }
}