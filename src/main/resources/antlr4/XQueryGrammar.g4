grammar XQueryGrammar;
import XpathGrammar;

xq
	: var													                                            # XQueryVariable
	| StringConstant											                                        # XQueryStringConstant
	| ap														                                        # XQueryAP
	| LPR xq RPR												                                        # XQueryWithParan
	| xq COMMA xq 											                                            # XQuerycomma
	| xq SINGLESLASH rp											                                        # XQuerychild_rp
	| xq DOUBLEBACKSLASH rp 									                                        # XQuerydescen_rpl
	| ANGULARLB NAME ANGULARRB CURLYLB xq CURLYRB ANGULARLB SINGLESLASH NAME ANGULARRB					# XQueryConstructor
	| forClause letClause? whereClause? returnClause    		                                        # XQueryFLWR
	| letClause xq 												                                        # XQueryLet
	;

var
	: '$' NAME
	;

forClause
	: 'for' var 'in' xq (',' var 'in' xq)*
	;

letClause
	: 'let' var ':=' xq (',' var ':=' xq)*
	;

whereClause
	: 'where' cond
	;

returnClause
	: 'return' xq
	;

cond
	: xq EQUAL xq 											        # XQueryEqual
	| xq EQ xq 											            # XQueryEqual
	| xq DBLEQUAL xq 											    # XQueryIs
	| xq IS xq 											            # XQueryIs
	| 'empty' LPR xq RPR 		 							        # XQueryEmpty
	| 'some' var 'in' xq (',' var 'in' xq)* 'satisfies' cond        # XQuerySome
	| LPR cond RPR 											        # XQueryCondwithP
	| cond AND cond 										        # XQueryCondAnd
	| cond OR cond 										            # XQueryCondOr
	| NOT cond 											            # XQueryCondNot
	;

StringConstant: STRING;
NAME: STRING;

STRING
:
   '"'
   (
      ESCAPE
      | ~["\\]
   )* '"'
   | '\''
   (
      ESCAPE
      | ~['\\]
   )* '\''
;


LPR : '(';
RPR : ')';
CURLYLB : '{';
CURLYRB : '}';
ANGULARLB : '<';
ANGULARRB : '>';

SINGLESLASH : '/';
DOUBLEBACKSLASH : '//';

EQUAL : '=';
EQ : 'eq';
DBLEQUAL : '==';
IS : 'is';
COMMA : ',';
AND : 'and';
OR : 'or';
NOT : 'not';