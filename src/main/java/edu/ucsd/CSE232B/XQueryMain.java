package edu.ucsd.CSE232B;

import edu.ucsd.CSE232B.parsers.XQueryGrammarLexer;
import edu.ucsd.CSE232B.parsers.XQueryGrammarParser;
import edu.ucsd.CSE232B.visitor.Rewriter;
import edu.ucsd.CSE232B.visitor.XQueryModifiedVisitor;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.*;
import java.util.Date;
import java.util.List;

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

public class XQueryMain {

    static XQueryGrammarParser.XqContext xqContext = null;

    public static void main(String[] args) {
        String originalQueryFile = "/Users/subharamesh/IdeaProjects/CSE_232B/src/main/resources/queries.txt";
        String rewrittenQueryFile = "/Users/subharamesh/IdeaProjects/CSE_232B/src/main/resources/rewrittenQuery.txt";
        String originalOutputFile = "/Users/subharamesh/IdeaProjects/CSE_232B/src/main/resources/originalOutput.txt";
        String rewrittenOutputFile = "/Users/subharamesh/IdeaProjects/CSE_232B/src/main/resources/newOutput.txt";

        long start = System.currentTimeMillis();
        // Original parsing
        parseQuery(originalQueryFile, originalOutputFile);
        long end = System.currentTimeMillis();
        System.out.println("Run time of original parse in milliseconds: " + (end - start));

        boolean performedJoin = analyzeJoin(rewrittenQueryFile);
        if (performedJoin) {
            // Rewritten parsing
            start = System.currentTimeMillis();
            parseQuery(rewrittenQueryFile, rewrittenOutputFile);
            end = System.currentTimeMillis();
            System.out.println("Run time of rewritten parse in milliseconds: " + (end - start));
        }
    }

    private static void parseQuery(String inputFile, String outputFile) {
        ANTLRInputStream input = null;
        try {
            input = new ANTLRInputStream(new FileInputStream(inputFile));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Call original code
        XQueryGrammarLexer lexer = new XQueryGrammarLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        XQueryGrammarParser parser = new XQueryGrammarParser(tokens);
        ParseTree tree = xqContext = parser.xq();
        XQueryModifiedVisitor visitor = new XQueryModifiedVisitor();

        final List<Node> nodes = visitor.visit(tree);

        writeNodesToFile(nodes, outputFile);
    }

    private static boolean analyzeJoin(String outputFile) {
        Rewriter rewriter = new Rewriter();
        rewriter.constructTree(xqContext);
        try {
            String rewrittenQuery = rewriter.printRewrittenQuery();
            if (rewrittenQuery.equals("")) {
                System.out.println("Cannot rewrite using joins. Output is the same as original Query");
                return false;
            } else {
                File fileOutput = new File(outputFile);
                FileOutputStream fos = new FileOutputStream(fileOutput);
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
                writer.write(rewrittenQuery);
                writer.close();
                System.out.println(rewrittenQuery);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void writeNodesToFile(List<Node> result, String filePath) {

        Document outputDoc = null;
        try {
            DocumentBuilderFactory docBF = DocumentBuilderFactory.newInstance();
            DocumentBuilder docB = docBF.newDocumentBuilder();
            outputDoc = docB.newDocument();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return;
        }

        Node element = outputDoc.createElement("element");
        for(Node entry : result){
            Node newNode = outputDoc.importNode(entry, true);
            element.appendChild(newNode);
        }
        outputDoc.appendChild(element);

        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();

            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            DOMSource source = new DOMSource(element);

            StreamResult terminal = new StreamResult(System.out);
            StreamResult fileOutput = new StreamResult(filePath);
            transformer.transform(source, terminal);
            transformer.transform(source, fileOutput);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}