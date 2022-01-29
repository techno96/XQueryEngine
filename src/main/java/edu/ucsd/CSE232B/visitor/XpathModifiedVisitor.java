package edu.ucsd.CSE232B.visitor;

import edu.ucsd.CSE232B.parsers.XpathGrammarBaseVisitor;
import edu.ucsd.CSE232B.parsers.XpathGrammarParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.util.*;

public class XpathModifiedVisitor extends XpathGrammarBaseVisitor<List<Node>> {
    List<Node> currentNodes = new ArrayList<>();

    @Override
    public List<Node> visitDoc(XpathGrammarParser.DocContext ctx) {
        final File inputXMLFile = new File(ctx.fileName().getText().replace('\"', ' ').strip());
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException exception) {
            exception.printStackTrace();
        }
        Document doc = null;
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
        currentNodes.add(doc);
        return currentNodes;
    }

    @Override
    public List<Node> visitChild(XpathGrammarParser.ChildContext ctx) {
        //double check logic
        visit(ctx.doc());
        return visit(ctx.rp());
    }

    @Override
    public List<Node> visitDescendant(XpathGrammarParser.DescendantContext ctx) {
        visit(ctx.doc());
        currentNodes.addAll(getDescendants(currentNodes));
        return visit(ctx.rp());
    }


    @Override
    public List<Node> visitTagName_rp(XpathGrammarParser.TagName_rpContext ctx) {
        List<Node> results = new ArrayList<>();
        List<Node> childrenList = getChildren(currentNodes);

        for (Node child: childrenList) {
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(ctx.getText())) {
                results.add(child);
            }
        }

        currentNodes = results;
        return results;
    }

    public List<Node> getDescendants(List<Node> list) {
        List<Node> desc = new ArrayList<Node>();
        for(int i = 0; i < list.size(); i++) {
            if(list.get(i).getChildNodes().getLength() != 0) {
                for(int j = 0; j < list.get(i).getChildNodes().getLength(); j++) {
                    desc.addAll(getAllNodes(list.get(i).getChildNodes().item(j)));
                }
            }
        }
        return desc;
    }

    public List<Node> getAllNodes(Node n) {
        List<Node> allNodes = new ArrayList<Node>();
        for(int i = 0; i < n.getChildNodes().getLength(); i++) {
            allNodes.addAll(getAllNodes(n.getChildNodes().item(i)));
        }
        allNodes.add(n);
        return allNodes;
    }

    public static List<Node> getChildren(List<Node> parents) {
        /**
         * return the children of the node (just the next level)
         */
        List<Node> childrenList = new ArrayList<>();
        for(int j = 0; j < parents.size(); j++) {
            Node currentNode = parents.get(j);
            for (int i = 0; i < currentNode.getChildNodes().getLength(); i++) {
                childrenList.add(currentNode.getChildNodes().item(i));
            }
        }
        return childrenList;

    }
}
