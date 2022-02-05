grammar XQueryGrammar;
import XpathGrammar;

@header {
package edu.ucsd.CSE232B.parsers;
}

xq
	: var													    # XQueryVariable
	| StringConstant											# XQueryStringConstant
	| ap														# XQueryAP
	| LPR xq RPR												# XQueryWithParan
	| xq COMMA xq 											    # XQuerycomma
	| xq SINGLESLASH rp											# XqRp
	| xq DOUBLEBACKSLASH rp 									# XqRpall
	| '<' NAME '>' '{' xq '}' '<' '/' NAME '>'					# XqConstructor
	| forClause letClause? whereClause? returnClause    		# FLWR
	| letClause xq 												# XqLet
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
	: xq EQUAL xq 											        # XqEqual
	| xq EQ xq 											            # XqEqual
	| xq DBLEQUAL xq 											    # XqIs
	| xq IS xq 											            # XqIs
	| 'empty' LPR xq RPR 		 							        # XqEmpty
	| 'some' var 'in' xq (',' var 'in' xq)* 'satisfies' cond        # XqSome
	| LPR cond RPR 											        # XqCondwithP
	| cond AND cond 										        # XqCondAnd
	| cond OR cond 										            # XqCondOr
	| NOT cond 											            # XqCondNot
	;

StringConstant: STRING;

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