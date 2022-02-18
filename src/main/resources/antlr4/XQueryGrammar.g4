grammar XQueryGrammar;
import XpathGrammar;

xq
	: var													                                            #XQueryVariable
	| stringConstant											                                        # XQueryStringConstant
	| ap														                                        # XQueryAP
	| LPR xq RPR												                                        # XQueryWithParan
	| xq COMMA xq 											                                            # XQuerycomma
	| xq SINGLESLASH rp											                                        # XQuerychild_rp
	| xq DOUBLEBACKSLASH rp 									                                        # XQuerydescen_rp
	| ANGULARLB IDENTIFIER ANGULARRB CURLYLB xq CURLYRB ANGULARLB SINGLESLASH IDENTIFIER ANGULARRB					# XQueryConstructor
	| forClause letClause? whereClause? returnClause    		                                        #XQueryFLWR
	| letClause xq 												                                        # XQueryLet
	;

var
	: DOLLAR IDENTIFIER
	;

forClause
	: FOR var IN xq (',' var IN xq)*
	;


letClause
	: LET var ':=' xq (',' var ':=' xq)*
	;

whereClause
	: WHERE cond
	;

returnClause
	: RETURN xq
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

stringConstant: STRING;

FOR:
 F O R;

fragment F
:
  [fF]
;

fragment O
:
  [oO]
;

fragment R
:
  [rR]
;

CURLYLB : '{';
CURLYRB : '}';
ANGULARLB : '<';
ANGULARRB : '>';
DOLLAR : '$';
EMPTY : 'empty';
SOME : 'some';
IN : 'in';
SATISFY : 'satisfies';
RETURN : 'return';
WHERE : 'where';
LET : 'let';


