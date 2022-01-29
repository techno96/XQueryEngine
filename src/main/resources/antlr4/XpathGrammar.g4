grammar XpathGrammar;

@header {
package edu.ucsd.CSE232B.parsers;
}

/*Rules*/
// absolute path
ap
    :doc SINGLESLASH rp         #child
    |doc DOUBLEBACKSLASH rp     #descendant
    ;

//document
doc:
  DOC LPR fileName RPR
;

// relative path
rp
    : tagName                   #tagName_rp
    | STAR                      #descendant_rp
    | DOT                       #present_rp
    | DOUBLEDOT                 #parent_rp
    | TEXTFUNTION               #text_rp
    | AT attName               #attribute_rp
    | LPR rp RPR                #paren_rp
    | rp SINGLESLASH rp         #child_rp
    | rp DOUBLEBACKSLASH rp     #descen_rp
    | rp LSQREPR f RSQREPR              #filer_rp
    | rp COMMA rp               #comma_rp
    ;

// path filter
f
    : rp                            #relpath
     | rp EQUAL stringConstant      #stringequilizer_f
     | rp EQUAL rp                  #equal_f
     | rp EQ rp                     #eq_f
     | rp DBLEQUAL rp               #douequal_f
     | rp IS rp                     #is_f
     | LPR f RPR                    #paren_f
     | f AND f                      #and_f
     | f OR f                       #or_f
     | NOT f                        #not_f
     ;


SINGLESLASH : '/';
DOUBLEBACKSLASH : '//';

//Filename
fileName:
STRING
;

LPR : '(';
RPR : ')';
LSQREPR : '[';
RSQREPR : ']';
AT : '@';
DOC:
 D O C;

fragment D
:
  [dD]
;

fragment O
:
  [oO]
;

fragment C
:
  [cC]
;

STAR : '*';
DOT : '.';
DOUBLEDOT : '..';
TEXTFUNTION : 'text()';
AND : 'and';
OR : 'or';
NOT : 'not';
EQUAL : '=';
EQ : 'eq';
DBLEQUAL : '==';
IS : 'is';
COMMA : ',';
tagName : IDENTIFIER;
attName : IDENTIFIER;
stringConstant :  STRING;

STRING
:
   '"' [A-Za-z0-9./_]* '"'
;

ESCAPE
:
   '\\'
   (
      ['"\\]
   )

;

IDENTIFIER : [a-zA-Z0-9_]+;
FLNAME : [a-zA-Z0-9_]+'.xml';

SKIPCHARS : [ \t\n\r]+ -> skip;


