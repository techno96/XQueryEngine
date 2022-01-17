grammar XpathGrammar;

@header {
package edu.ucsd.CSE232B.parsers;
}

/*Rules*/
// absolute path
ap
   :doc '/' rp
   |doc '//' rp
   ;

//document
doc
    : DOC '(' '"' filename '"' ')'
    ;

//Filename
filename
        : NAME
        ;

// relative path
rp
    : NAME
    ;
