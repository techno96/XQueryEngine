package edu.ucsd.CSE232B.visitor;

import edu.ucsd.CSE232B.parsers.XQueryGrammarParser;
import org.antlr.v4.runtime.misc.Pair;

import java.util.*;

public class Rewriter {

    List<JoinTree> variableTrees = new ArrayList<>();
    HashMap<String, Integer> varTreeMap = new HashMap<>();
    String rewrittenQuery = "";
    HashMap<Pair<Integer, Integer>, ArrayList<Pair<String, String>>> joinConnection;


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


    private void constructTree(XQueryGrammarParser.XqContext xqContext) {

        // We need join only for FLWR cases
        if (!(xqContext instanceof XQueryGrammarParser.XQueryFLWRContext)) {
            return;
        }

        XQueryGrammarParser.ForClauseContext forClause = ((XQueryGrammarParser.XQueryFLWRContext) xqContext).forClause();
        processForClause(forClause);

        XQueryGrammarParser.WhereClauseContext whereClause = ((XQueryGrammarParser.XQueryFLWRContext) xqContext).whereClause();
        processWhereClause(whereClause.cond());

        XQueryGrammarParser.ReturnClauseContext returnClause = ((XQueryGrammarParser.XQueryFLWRContext) xqContext).returnClause();

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
            Integer tableIndex;
            if (left_eq.startsWith("$")) {
                tableIndex = varTreeMap.get(left_eq);
            } else {
                tableIndex = varTreeMap.get(right_eq);
            }

            variableTrees.get(tableIndex).whereMapping.add(condition.getText());
        }
    }

    private void eqWithEq(XQueryGrammarParser.CondContext condition, String left_eq, String right_eq) {
        Integer tableIndex1 = varTreeMap.get(left_eq);
        Integer tableIndex2 = varTreeMap.get(right_eq);

        // TODO : See if we need to swap smaller vs larger
        Pair<Integer, Integer> edge = new Pair<>(tableIndex1, tableIndex2);
        Pair<String, String> queries = new Pair<>(left_eq, right_eq);

        if (tableIndex1.equals(tableIndex2)) {
            //both belong to the same table
            variableTrees.get(tableIndex1).whereMapping.add(condition.getText());
        } else {
            //both belong to different tables
            if(!joinConnection.containsKey(edge)){
                joinConnection.put(edge, new ArrayList<>());
            }
            joinConnection.get(edge).add(queries);
        }
    }
}