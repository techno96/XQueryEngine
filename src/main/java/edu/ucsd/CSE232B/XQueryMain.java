package edu.ucsd.CSE232B;

import edu.ucsd.CSE232B.parsers.XQueryGrammarLexer;
import edu.ucsd.CSE232B.parsers.XQueryGrammarParser;
import edu.ucsd.CSE232B.visitor.XQueryModifiedVisitor;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class XQueryMain {
    public static void main(String[] args) {
        final ANTLRInputStream input;
        try {
            //TODO : Replace with FileInputStream
            input = new ANTLRInputStream(new FileInputStream(args[0]));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        final XQueryGrammarLexer lexer = new XQueryGrammarLexer(input);
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final XQueryGrammarParser parser = new XQueryGrammarParser(tokens);
        final ParseTree tree = parser.xq();
        final XQueryModifiedVisitor visitor = new XQueryModifiedVisitor();

        final List<Node> nodes = visitor.visit(tree);

        System.out.println(nodes.size());
        for (Node n : nodes) {
            System.out.println(n.getTextContent() + '\n');
        }

        writeNodesToFile(visitor.output, nodes, args[1]);
    }

    public static void writeNodesToFile(Document doc, List<Node> result, String filePath) {
        doc.appendChild(result.get(0));
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();

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
}