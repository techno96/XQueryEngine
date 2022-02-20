package edu.ucsd.CSE232B.visitor;

import edu.ucsd.CSE232B.parsers.XQueryGrammarBaseVisitor;
import edu.ucsd.CSE232B.parsers.XQueryGrammarParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.util.*;

public class XQueryModifiedVisitor extends XQueryGrammarBaseVisitor<List<Node>> {

    public Document output = null;
    List<Node> currentNodes = new ArrayList<>();

    // Maps identifier to list of results
    HashMap<String, List<Node>> ctxMap = new HashMap<>();
    // Stack which maintains the order of traversal
    Stack<HashMap<String, List<Node>>> ctxStack = new Stack<>();

    public Document doc = null;

    @Override
    public List<Node> visitXQueryVariable(XQueryGrammarParser.XQueryVariableContext ctx) {
        return ctxMap.get(ctx.getText());
    }

    @Override
    public List<Node> visitXQueryStringConstant(XQueryGrammarParser.XQueryStringConstantContext ctx) {
        String str = ctx.stringConstant().getText();
        List<Node> resultList = new ArrayList<>();
        resultList.add(doc.createTextNode(str.substring(1, str.length() - 1)));
        return resultList;
    }

    @Override
    public List<Node> visitXQueryAP(XQueryGrammarParser.XQueryAPContext ctx) {
        return visit(ctx.ap());
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
    public List<Node> visitXQuerychild_rp(XQueryGrammarParser.XQuerychild_rpContext ctx) {
        currentNodes = visit(ctx.xq());
        return visit(ctx.rp());
    }

    @Override
    public List<Node> visitXQuerydescen_rp(XQueryGrammarParser.XQuerydescen_rpContext ctx) {
        currentNodes = visit(ctx.xq());
        currentNodes.addAll(getDescendants(currentNodes));
        return visit(ctx.rp());
    }

    @Override
    public List<Node> visitXQueryConstructor(XQueryGrammarParser.XQueryConstructorContext ctx) {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException exception) {
            exception.printStackTrace();
        }
        output = builder.newDocument();
        final List<Node> xqNodes = visit(ctx.xq());
        List<Node> resultList = new ArrayList<>();
        resultList.add(createElement(ctx.IDENTIFIER(0).getText(), xqNodes));
        return resultList;
    }

    @Override
    public List<Node> visitXQueryLet(XQueryGrammarParser.XQueryLetContext ctx) {
        HashMap<String, List<Node>> originalMap = new HashMap<>(ctxMap);
        ctxStack.push(originalMap);
        List<Node> result = visitChildren(ctx);
        // TODO : What is the difference between ctxStack.pop() and originalMap ?
        ctxMap = ctxStack.pop();
        return result;
    }

    private List<Node> constructItems(XQueryGrammarParser.ForClauseContext ctx, int depth) {
        List<Node> initial_xq_list = visit(ctx.xq(depth));
        List<Node> result = new ArrayList<>();
        if (ctx.xq().size() == 1) {
            // for one variable in the for expression
            for (Node n : initial_xq_list) {
                List<Node> tempList = new ArrayList<>();
                tempList.add(n);
                ctxMap.put(ctx.var(depth).getText(), tempList);
                result.add(n);
            }
        } else {
            // for more than one variable in the for expression
            for (Node n : initial_xq_list) {
                HashMap<String, List<Node>> originalMap = new HashMap<>(ctxMap);
                ctxStack.push(originalMap);
                List<Node> tempList = new ArrayList<>();
                tempList.add(n);
                ctxMap.put(ctx.var(depth).getText(), tempList);
                result.addAll(constructItems( ctx, depth + 1));
                ctxMap = ctxStack.pop();
            }
        }
        return result;
    }

    private void generateCombinations(XQueryGrammarParser.XQueryFLWRContext ctx, int depth, List<Node> result) {
        // TODO : Check if amount amount of xq == var
        if (ctx.forClause().var().size() == depth) {
            HashMap<String, List<Node>> originalMap = new HashMap<>(ctxMap);
            if (ctx.letClause() != null) {
                visit(ctx.letClause());
            }

            if (ctx.whereClause() != null) {
                // TODO : What are we doing with these nodes ?
                List<Node> where_Nodes = visit(ctx.whereClause());
                if (where_Nodes == null || where_Nodes.isEmpty()) {
                    return;
                }
                //ctxMap.put(ctx.forClause().var(depth - 1).getText(), where_Nodes);
            }

            List<Node> returnList = visit(ctx.returnClause());
            if (returnList != null && !returnList.isEmpty()) {
                result.addAll(returnList);
            }
            ctxMap = originalMap;
        } else {
            // TODO : Should we use stack here as well ?
            String variable_name = ctx.forClause().var(depth).getText();
            List<Node> for_Nodes = visit(ctx.forClause().xq(depth));
            for (Node n : for_Nodes) {
                HashMap<String, List<Node>> originalMap = new HashMap<>(ctxMap);
                ctxStack.push(originalMap);
                ctxMap.remove(variable_name);
                List<Node> singleList = new ArrayList<>();
                singleList.add(n);
                ctxMap.put(variable_name, singleList);
                generateCombinations(ctx, depth + 1, result);
                ctxMap = ctxStack.pop();
            }

        }
    }

    @Override
    public List<Node> visitXQueryFLWR(XQueryGrammarParser.XQueryFLWRContext ctx) {
        HashMap<String, List<Node>> originalMap = new HashMap<>(ctxMap);
        ctxStack.push(originalMap);
        List<Node> result = new ArrayList<>();
        generateCombinations(ctx, 0, result);
        ctxMap = ctxStack.pop();
        return result;
    }

    @Override
    public List<Node> visitForClause(XQueryGrammarParser.ForClauseContext ctx) {
        return constructItems(ctx, 0);
    }

    // TODO : Check *Some* logic
    @Override
    public List<Node> visitXQuerySome(XQueryGrammarParser.XQuerySomeContext ctx) {
        List<XQueryGrammarParser.VarContext> variables = ctx.var();
        for (int i = 0; i < variables.size(); i++) {
            ctxMap.put(variables.get(i).getText(), visit(ctx.xq(i)));
        }
        return visit(ctx.cond());
    }

    @Override
    public List<Node> visitLetClause(XQueryGrammarParser.LetClauseContext ctx) {
        List<XQueryGrammarParser.VarContext> variables = ctx.var();
        for (int i = 0; i < variables.size(); i++) {
           ctxMap.put(variables.get(i).getText(), visit(ctx.xq(i)));
        }
        return null;
    }

    @Override
    public List<Node> visitWhereClause(XQueryGrammarParser.WhereClauseContext ctx) {
        return visit(ctx.cond());
    }

    @Override
    public List<Node> visitReturnClause(XQueryGrammarParser.ReturnClauseContext ctx) {
        return visit(ctx.xq());
    }

    @Override
    public List<Node> visitXQueryOr(XQueryGrammarParser.XQueryOrContext ctx) {
        List<Node> left_xq = visit(ctx.cond(0));
        List<Node> right_xq = visit(ctx.cond(1));

        if (!left_xq.isEmpty() && !right_xq.isEmpty()) {
            left_xq.addAll(right_xq);
            return left_xq;
        } else if (right_xq.isEmpty()) {
            return left_xq;
        } else {
            return right_xq;
        }
    }

    @Override
    public List<Node> visitXQueryAnd(XQueryGrammarParser.XQueryAndContext ctx) {
        List<Node> left_cond = visit(ctx.cond(0));
        List<Node> right_cond = visit(ctx.cond(1));
        if (!left_cond.isEmpty() && !right_cond.isEmpty()) {
            return left_cond;
        }

        return new ArrayList<>();
    }

    @Override
    public List<Node> visitXQueryNot(XQueryGrammarParser.XQueryNotContext ctx) {
         List<Node> cond_nodes = visit(ctx.cond());
         if (!cond_nodes.isEmpty()) {
             return new ArrayList<>();
         }

         List<Node> resultList = new ArrayList<>();
         resultList.add(doc.createElement("newNode"));
         return resultList;
    }

    @Override
    public List<Node> visitXQueryParen(XQueryGrammarParser.XQueryParenContext ctx) {
        return visit(ctx.cond());
    }

    @Override
    public List<Node> visitXQueryEmpty(XQueryGrammarParser.XQueryEmptyContext ctx) {
        List<Node> cond_nodes = visit(ctx.xq());
        if (!cond_nodes.isEmpty()) {
            return new ArrayList<>();
        }

        List<Node> resultList = new ArrayList<>();
        resultList.add(doc.createElement("newNode"));
        return resultList;

    }

    @Override
    public List<Node> visitXQueryEqual(XQueryGrammarParser.XQueryEqualContext ctx) {
        //TODO : Do we need to maintain current nodes here ?
        List<Node> left_xq = visit(ctx.xq(0));
        List<Node> right_xq = visit(ctx.xq(1));

        List<Node> result = new ArrayList<>();

        for (Node n : left_xq) {
            for (Node n1 : right_xq) {
                if (n.isEqualNode(n1)) {
                    result.add(n);
                    return result;
                }
            }
        }

        return result;
    }

    @Override
    public List<Node> visitXQueryIs(XQueryGrammarParser.XQueryIsContext ctx) {
        List<Node> currentList = currentNodes;
        List<Node> left_xq = visit(ctx.xq(0));
        currentNodes = currentList;
        List<Node> right_xq = visit(ctx.xq(1));
        currentNodes = currentList;

        List<Node> result = new ArrayList<>();

        for (Node n : left_xq) {
            for (Node n1 : right_xq) {
                if (n.isSameNode(n1)) {
                    result.add(n);
                    return result;
                }
            }
        }

        return result;
    }


    // XPath methods from here on

    @Override
    public List<Node> visitChild(XQueryGrammarParser.ChildContext ctx) {
        //double check logic
        visit(ctx.doc());
        return visit(ctx.rp());
    }

    @Override
    public List<Node> visitDescendant(XQueryGrammarParser.DescendantContext ctx) {
        visit(ctx.doc());
        currentNodes.addAll(getDescendants(currentNodes));
        return visit(ctx.rp());
    }

    @Override
    public List<Node> visitDoc(XQueryGrammarParser.DocContext ctx) {
        final File inputXMLFile = new File(ctx.fileName().getText().replace('\"', ' ').strip());
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException exception) {
            exception.printStackTrace();
        }
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
        currentNodes = resultList;
        return resultList;
    }

    @Override
    public List<Node> visitTagName_rp(XQueryGrammarParser.TagName_rpContext ctx) {
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
    public List<Node> visitDescendant_rp(XQueryGrammarParser.Descendant_rpContext ctx) {
        List<Node> resultList = getChildren(currentNodes);
        currentNodes = resultList;
        return resultList;
    }

    @Override
    public List<Node> visitPresent_rp(XQueryGrammarParser.Present_rpContext ctx) {
        return currentNodes;
    }

    @Override
    public List<Node> visitParent_rp(XQueryGrammarParser.Parent_rpContext ctx) {
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
    public List<Node> visitParen_rp(XQueryGrammarParser.Paren_rpContext ctx) {
        return visit(ctx.rp());
    }

    @Override
    public List<Node> visitDescen_rp(XQueryGrammarParser.Descen_rpContext ctx) {
        visit(ctx.rp(0));
        currentNodes.addAll(getDescendants(currentNodes));
        return visit(ctx.rp(1));
    }

    @Override
    public List<Node> visitParen_f(XQueryGrammarParser.Paren_fContext ctx) {
        return visit(ctx.f());
    }

    @Override
    public List<Node> visitEqual_f(XQueryGrammarParser.Equal_fContext ctx) {
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
    public List<Node> visitIs_f(XQueryGrammarParser.Is_fContext ctx) {
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
    public List<Node> visitComma_rp(XQueryGrammarParser.Comma_rpContext ctx) {
        List<Node> currentList = currentNodes;
        List<Node> left_rp = visit(ctx.rp(0));
        currentNodes = currentList;
        List<Node> right_rp = visit(ctx.rp(1));
        left_rp.addAll(right_rp);
        currentNodes = left_rp;
        return left_rp;
    }

    @Override
    public List<Node> visitFilter_rp(XQueryGrammarParser.Filter_rpContext ctx) {
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
    public List<Node> visitStringequilizer_f(XQueryGrammarParser.Stringequilizer_fContext ctx) {
        //make a copy of current nodes
        List<Node> rpResult = visit(ctx.rp());
        //Handle terminal node
        XQueryGrammarParser.StringConstantContext strCtx = ctx.stringConstant();
        String compareString = strCtx.getText();
        compareString = compareString.replace('\"', ' ').strip();
        //continue on this
        List<Node> resultList = new ArrayList<>();

        for (Node n : rpResult) {
            String data = n.getTextContent();
            if (compareString.equals(data)) {
                resultList.add(n);
            }
        }

        return resultList;
    }

    @Override
    public List<Node> visitText_rp(XQueryGrammarParser.Text_rpContext ctx) {
        List<Node> textNodes = new ArrayList<>();
        for (Node n : currentNodes) {
            List<Node> parent = new ArrayList<>();
            parent.add(n);
            List<Node> children = getChildren(parent);
            for (Node child : children) {
                if (child.getNodeType() == Node.TEXT_NODE){
                    textNodes.add(child);
                }
            }
        }
        currentNodes = textNodes;
        return textNodes;
    }

    @Override
    public List<Node> visitChild_rp(XQueryGrammarParser.Child_rpContext ctx) {
        currentNodes = visit(ctx.rp(0));
        currentNodes = visit(ctx.rp(1));
        return currentNodes;
    }

    @Override
    public List<Node> visitAnd_f(XQueryGrammarParser.And_fContext ctx) {
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
    public List<Node> visitOr_f(XQueryGrammarParser.Or_fContext ctx) {
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
    public List<Node> visitNot_f(XQueryGrammarParser.Not_fContext ctx) {
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
    public List<Node> visitRelpath(XQueryGrammarParser.RelpathContext ctx) {
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

    private Node createElement(String tag, List<Node> nodes) {
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

}
