package edu.ucsd.CSE232B.visitor;

import edu.ucsd.CSE232B.parsers.XQueryGrammarParser;
import org.antlr.v4.runtime.misc.Pair;

import java.util.*;

public class Rewriter {

    List<JoinTree> variableTrees = new ArrayList<>();
    HashMap<String, Integer> varTreeMap = new HashMap<>();
    String rewrittenQuery = "";
    HashMap<Pair<Integer, Integer>, ArrayList<Pair<String, String>>> joinConnection = new HashMap<>();

    public class JoinTree {
        List<Pair<String, String>> varXqueryPair;
        List<String> whereMapping;
        int index;

        JoinTree (int index){
            varXqueryPair = new ArrayList<>();
            whereMapping = new ArrayList<>();
            this.index = index;
        }
    }

    public void constructTree(XQueryGrammarParser.XqContext xqContext) {
        XQueryGrammarParser.ForClauseContext forClause = ((XQueryGrammarParser.XQueryFLWRContext) xqContext).forClause();
        processForClause(forClause);

        XQueryGrammarParser.WhereClauseContext whereClause = ((XQueryGrammarParser.XQueryFLWRContext) xqContext).whereClause();
        processWhereClause(whereClause.cond());

        XQueryGrammarParser.ReturnClauseContext returnClause = ((XQueryGrammarParser.XQueryFLWRContext) xqContext).returnClause();
        processReturnClause(returnClause);
    }

    private void processReturnClause(XQueryGrammarParser.ReturnClauseContext returnClause) {
        rewrittenQuery = returnClause.getText().replace("return", "return ");
        rewrittenQuery = rewrittenQuery.replaceAll("\\$([A-Za-z0-9_]+)", "\\$tuple/$1/*");
    }

    private void processForClause(XQueryGrammarParser.ForClauseContext forClause) {
        for (int i = 0; i < forClause.var().size() ; i++) {
            String variable = forClause.var(i).getText();
            String xquery = forClause.xq(i).getText();

            // Maintain a pair of variable and its corresponding XQuery
            Pair<String, String> var_xq_Pair = new Pair<>(variable, xquery);

            Integer treeIndex;
            if (xquery.startsWith("doc")) {
                treeIndex = variableTrees.size();
                variableTrees.add(new JoinTree(treeIndex));
            } else {
                int slash_index = xquery.indexOf('/') == -1 ? xquery.length() : xquery.indexOf('/');
                treeIndex = varTreeMap.get(xquery.substring(0, slash_index));
            }
            variableTrees.get(treeIndex).varXqueryPair.add(var_xq_Pair);
            varTreeMap.put(variable, treeIndex);
        }
    }

    private void processWhereClause(XQueryGrammarParser.CondContext condition) {
        if (condition instanceof XQueryGrammarParser.XQueryAndContext) {
            //Parse multiple and conditions
            XQueryGrammarParser.XQueryAndContext andCtx = (XQueryGrammarParser.XQueryAndContext) condition;
            processWhereClause(andCtx.cond(0));
            processWhereClause(andCtx.cond(1));
        } else if (condition instanceof XQueryGrammarParser.XQueryEqualContext) {
            processWhereEqCondition(condition);
        }
    }

    private void processWhereEqCondition(XQueryGrammarParser.CondContext condition) {
        //Processing within each where condition for an equals expression
        XQueryGrammarParser.XQueryEqualContext equalCtx = (XQueryGrammarParser.XQueryEqualContext) condition;
        String left_eq = equalCtx.xq(0).getText();
        String right_eq = equalCtx.xq(1).getText();
        if (left_eq.startsWith("$") && right_eq.startsWith("$")) {
            // eg : $a eq $b
            eqWithEq(condition, left_eq, right_eq);
        } else {
            // eg : $a = stringConstant
            Integer treeIndex;
            if (left_eq.startsWith("$")) {
                treeIndex = varTreeMap.get(left_eq);
            } else {
                treeIndex = varTreeMap.get(right_eq);
            }

            variableTrees.get(treeIndex).whereMapping.add(condition.getText());
        }
    }

    private void eqWithEq(XQueryGrammarParser.CondContext condition, String left_eq, String right_eq) {
        Integer treeIndex1 = varTreeMap.get(left_eq);
        Integer treeIndex2 = varTreeMap.get(right_eq);

        // Swapping indices to avoid duplicates
        if (treeIndex1 > treeIndex2) {
            Integer temp1 = treeIndex1;
            treeIndex1 = treeIndex2;
            treeIndex2 = temp1;

            String temp2 = left_eq;
            left_eq = right_eq;
            right_eq = temp2;
        }

        Pair<Integer, Integer> edge = new Pair<>(treeIndex1, treeIndex2);
        Pair<String, String> queries = new Pair<>(left_eq, right_eq);

        if (treeIndex1.equals(treeIndex2)) {
            //both belong to the same tree
            variableTrees.get(treeIndex1).whereMapping.add(condition.getText());
        } else {
            //both belong to different trees
            if(!joinConnection.containsKey(edge)){
                joinConnection.put(edge, new ArrayList<>());
            }
            joinConnection.get(edge).add(queries);
        }
    }

    public String printRewrittenQuery() {
        if (variableTrees.size() < 1) {
            return "";
        }

        String queryStart = "for $tuple in ";
        String innerJoin = joinForWhereReturn(variableTrees.get(0));

        // Create a new join statement
        for (int j = 1; j < variableTrees.size(); j++) {
            List<Pair<String, String>> edgeCollection = new ArrayList<>();
            for (int i = 0 ; i < j ; i++) {
                if (joinConnection.containsKey(new Pair<>(i, j))) {
                    edgeCollection.addAll(joinConnection.get(new Pair<>(i, j)));
                }
            }

            // Combine outer and inner join statements
            String outerJoin = "join ( \n" + innerJoin + ",\n" + joinForWhereReturn(variableTrees.get(j)) + ",\n";

            StringBuilder leftVars = new StringBuilder();
            StringBuilder rightVars = new StringBuilder();
            for (Pair<String, String> pair: edgeCollection) {
                leftVars.append(pair.a.substring(1)).append(", ");
                rightVars.append(pair.b.substring(1)).append(", ");
            }

            if (leftVars.length() > 0){
                leftVars = new StringBuilder(leftVars.substring(0, leftVars.length() - 2));
                rightVars = new StringBuilder(rightVars.substring(0, rightVars.length() - 2));
            }

            innerJoin = outerJoin + "[" + leftVars + "], [" + rightVars + "]" + ")";
        }

        return queryStart + innerJoin + "\n" + rewrittenQuery;
    }

    private String joinForWhereReturn(JoinTree tree) {
        StringBuilder clause = new StringBuilder("for ");
        StringBuilder resultString = new StringBuilder();
        List<Pair<String, String>> varXqueryPair = tree.varXqueryPair;

        // Add variables and XQuery
        for (Pair<String, String> entry : varXqueryPair) {
            String variable = entry.a;
            String identifier = variable.substring(1);
            clause.append(variable).append(" in ").append(entry.b).append(",\n");
            resultString.append("<").append(identifier).append(">{").append(variable).append("} </").append(identifier).append(">,\n");
        }

        clause = new StringBuilder(clause.substring(0, clause.length() - 2)).append("\n");
        resultString = new StringBuilder(resultString.substring(0, resultString.length() - 2)).append("\n");


        // Add the where clauses, if any
        List<String> whereClauses = tree.whereMapping;
        if (whereClauses.size() > 0) {
            clause.append("where ");
            for (String entry : whereClauses) {
                clause.append(entry).append("\n");
            }
        }

        // Return as tuples
        clause.append("return <tuple> {").append(resultString).append("} </tuple>\n");
        return clause.toString();
    }


}