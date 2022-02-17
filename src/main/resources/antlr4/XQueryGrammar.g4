grammar XQueryGrammar;
import XpathGrammar;

xq
	: var													                                            # XQueryVariable
	| StringConstant											                                        # XQueryStringConstant
	| ap														                                        # XQueryAP
	| LPR xq RPR												                                        # XQueryWithParan
	| xq COMMA xq 											                                            # XQuerycomma
	| xq SINGLESLASH rp											                                        # XQuerychild_rp
	| xq DOUBLEBACKSLASH rp 									                                        # XQuerydescen_rp
	| ANGULARLB NAME ANGULARRB CURLYLB xq CURLYRB ANGULARLB SINGLESLASH NAME ANGULARRB					# XQueryConstructor
	| forClause letClause? whereClause? returnClause    		                                        # XQueryFLWR
	| letClause xq 												                                        # XQueryLet
	;

var
	: DOLLAR NAME
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
	| EMPTY LPR xq RPR 		 							            # XQueryEmpty
	| SOME var IN xq (',' var IN xq)* SATISFY cond                  # XQuerySome
	| LPR cond RPR 											        # XQueryParen
	| cond AND cond 										        # XQueryAnd
	| cond OR cond 										            # XQueryOr
	| NOT cond 											            # XQueryNot
	;

StringConstant: STRING;
NAME: IDENTIFIER;

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

ESCAPE
:
   '\\'
   (
      ['"\\]
   )

;


LPR : '(';
RPR : ')';
CURLYLB : '{';
CURLYRB : '}';
ANGULARLB : '<';
ANGULARRB : '>';
DOLLAR : '$';
EMPTY : 'empty';
SOME : 'some';
IN : 'in';
SATISFY : 'satisfies';

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

IDENTIFIER : [a-zA-Z0-9_]+;