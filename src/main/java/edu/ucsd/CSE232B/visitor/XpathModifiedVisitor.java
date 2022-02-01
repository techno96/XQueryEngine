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
    public Document doc = null;

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
        List<Node> resultList = new ArrayList<>();
        resultList.add(doc);
        this.doc = doc;
        currentNodes = resultList;
        return resultList;
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
    public List<Node> visitDescendant_rp(XpathGrammarParser.Descendant_rpContext ctx) {
        List<Node> resultList = getChildren(currentNodes);
        currentNodes = resultList;
        return resultList;
    }

    @Override
    public List<Node> visitPresent_rp(XpathGrammarParser.Present_rpContext ctx) {
        return currentNodes;
    }

    @Override
    public List<Node> visitParent_rp(XpathGrammarParser.Parent_rpContext ctx) {
        List<Node> resultList = new ArrayList<>();
        for (Node n : currentNodes) {
            Node ownerNode = n.getParentNode();
            if (!resultList.contains(ownerNode)) {
                resultList.add(ownerNode);
            }
        }
        currentNodes = resultList;
        return resultList;
    }

    @Override
    public List<Node> visitParen_rp(XpathGrammarParser.Paren_rpContext ctx) {
        return visit(ctx.rp());
    }

    @Override
    public List<Node> visitDescen_rp(XpathGrammarParser.Descen_rpContext ctx) {
        visit(ctx.rp(0));
        currentNodes.addAll(getDescendants(currentNodes));
        return visit(ctx.rp(1));
    }

    @Override
    public List<Node> visitParen_f(XpathGrammarParser.Paren_fContext ctx) {
        return visit(ctx.f());
    }

    @Override
    public List<Node> visitEqual_f(XpathGrammarParser.Equal_fContext ctx) {
        List<Node> currentList = currentNodes;
        List<Node> left_rp = visit(ctx.rp(0));
        currentNodes = currentList;
        List<Node> right_rp = visit(ctx.rp(1));
        currentNodes = currentList;
        for (Node n : left_rp) {
            for (Node n1 : right_rp) {
                if (!n.isEqualNode(n1)) {
                    return new ArrayList<>();
                }
            }
        }
        currentNodes = left_rp;
        return left_rp;
    }

    @Override
    public List<Node> visitIs_f(XpathGrammarParser.Is_fContext ctx) {
        List<Node> currentList = currentNodes;
        List<Node> left_rp = visit(ctx.rp(0));
        currentNodes = currentList;
        List<Node> right_rp = visit(ctx.rp(1));
        currentNodes = currentList;
        for (Node n : left_rp) {
            for (Node n1 : right_rp) {
                if (!n.isSameNode(n1)) {
                    return new ArrayList<>();
                }
            }
        }
        currentNodes = left_rp;
        return left_rp;
    }

    @Override
    public List<Node> visitComma_rp(XpathGrammarParser.Comma_rpContext ctx) {
        List<Node> currentList = currentNodes;
        List<Node> left_rp = visit(ctx.rp(0));
        currentNodes = currentList;
        List<Node> right_rp = visit(ctx.rp(1));
        left_rp.addAll(right_rp);
        currentNodes = left_rp;
        return left_rp;
    }

    @Override
    public List<Node> visitFilter_rp(XpathGrammarParser.Filter_rpContext ctx) {
        List<Node> rpResult = visit(ctx.rp());
        List<Node> resultList = new ArrayList<>();
        for (Node n : rpResult) {
            List<Node> singleList = new ArrayList<>();
            singleList.add(n);
            currentNodes = singleList;
            List<Node> f_result = visit(ctx.f());
            if (!f_result.isEmpty()) {
                resultList.add(n);
            }
        }
        return resultList;
    }

    @Override
    public List<Node> visitStringequilizer_f(XpathGrammarParser.Stringequilizer_fContext ctx) {
        //make a copy of current nodes
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
        currentNodes = visit(ctx.rp(0));
        currentNodes = visit(ctx.rp(1));
        return currentNodes;
    }

    @Override
    public List<Node> visitAnd_f(XpathGrammarParser.And_fContext ctx) {
        List<Node> currentList = currentNodes;
        List<Node> resultList = new ArrayList<>();
        for (Node n : currentList) {
            List<Node> singleList = new ArrayList<>();
            singleList.add(n);
            currentNodes = singleList;
            List<Node> firstList = visit(ctx.f(0));
            currentNodes = singleList;
            List<Node> secondList = visit(ctx.f(1));
            if (!firstList.isEmpty() && !secondList.isEmpty()) {
                resultList.add(n);
            }
        }
        return resultList;
    }

    @Override
    public List<Node> visitOr_f(XpathGrammarParser.Or_fContext ctx) {
        List<Node> currentList = currentNodes;
        List<Node> resultList = new ArrayList<>();
        for (Node n : currentList) {
            List<Node> singleList = new ArrayList<>();
            singleList.add(n);
            currentNodes = singleList;
            List<Node> firstList = visit(ctx.f(0));
            currentNodes = singleList;
            List<Node> secondList = visit(ctx.f(1));
            if (!firstList.isEmpty() || !secondList.isEmpty()) {
                resultList.add(n);
            }
        }
        return resultList;
    }

    @Override
    public List<Node> visitNot_f(XpathGrammarParser.Not_fContext ctx) {
        List<Node> firstList = currentNodes;
        List<Node> resultList = new ArrayList<>();
        for (Node n : firstList) {
            List<Node> singleList = new ArrayList<>();
            singleList.add(n);
            currentNodes = singleList;
            List<Node> secondList = visit(ctx.f());
            if (secondList.isEmpty()) {
                resultList.add(n);
            }
        }
        return resultList;
    }

    @Override
    public List<Node> visitRelpath(XpathGrammarParser.RelpathContext ctx) {
        List<Node> currentList = currentNodes;
        List<Node> resultList = visit(ctx.rp());
        currentNodes = currentList;
        return resultList;
    }

    public List<Node> getDescendants(List<Node> parents) {
        List<Node> descendants = new ArrayList<>();
        for(Node n : parents) {
            if(n.getChildNodes().getLength() != 0) {
                for(int j = 0; j < n.getChildNodes().getLength(); j++) {
                    descendants.addAll(getAllChildNodes(n.getChildNodes().item(j)));
                }
            }
        }
        return descendants;
    }

    public List<Node> getAllChildNodes(Node parent) {
        List<Node> totalList = new ArrayList<>();
        for(int i = 0; i < parent.getChildNodes().getLength(); i++) {
            totalList.addAll(getAllChildNodes(parent.getChildNodes().item(i)));
        }
        totalList.add(parent);
        return totalList;
    }


    public static List<Node> getChildren(List<Node> parents) {
        List<Node> childrenList = new ArrayList<>();
        for (Node n : parents) {
            for (int i = 0; i < n.getChildNodes().getLength(); i++) {
                childrenList.add(n.getChildNodes().item(i));
            }
        }
        return childrenList;

    }
}
