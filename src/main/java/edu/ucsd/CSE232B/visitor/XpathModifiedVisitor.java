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

    @Override
    public List<Node> visitFilter_rp(XpathGrammarParser.Filter_rpContext ctx) {
        List<Node> rpResult = visit(ctx.rp());
        currentNodes = visit(ctx.f());
        //double check this
        List<Node> resultList = new ArrayList<>();
        for (Node n : currentNodes) {
            Node ownerNode = n.getParentNode().getParentNode();
            if (!resultList.contains(ownerNode)) {
                resultList.add(ownerNode);
            }

        }
        return resultList;
    }

    @Override
    public List<Node> visitStringequilizer_f(XpathGrammarParser.Stringequilizer_fContext ctx) {
        List<Node> rpResult = visit(ctx.rp());
        //Handle terminal node
        XpathGrammarParser.StringConstantContext strCtx = ctx.stringConstant();
        String compareString = strCtx.getText();
        compareString = compareString.replace('\"', ' ').strip();
        //continue on this
        List<Node> resultList = new ArrayList<>();

        for (Node n : rpResult) {
            if (n.getFirstChild() != null) {
                String data = n.getFirstChild().getTextContent();
                if (compareString.equals(data)) {
                    resultList.add(n);
                }
            }
        }

        return resultList;
    }

    @Override
    public List<Node> visitText_rp(XpathGrammarParser.Text_rpContext ctx) {
        return currentNodes;
    }

    @Override
    public List<Node> visitChild_rp(XpathGrammarParser.Child_rpContext ctx) {
        visit(ctx.rp(0));
        currentNodes = visit(ctx.rp(1));
        return currentNodes;
    }

    @Override
    public List<Node> visitAnd_f(XpathGrammarParser.And_fContext ctx) {
        List<Node> firstList = visit(ctx.f(0));
        List<Node> secondList = visit(ctx.f(1));
        List<Node> resultList = new ArrayList<>();
        for (Node n : firstList) {
            if (secondList.contains(n)) {
                resultList.add(n);
            }
        }
        return resultList;
    }

    @Override
    public List<Node> visitRelpath(XpathGrammarParser.RelpathContext ctx) {
        currentNodes = visit(ctx.rp());
        return currentNodes;
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
