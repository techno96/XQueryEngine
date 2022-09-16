# XQueryEngine

* This repository contains the code for an XML XQuery Processor. We consider a subset/modification of XML’s data model and of XQuery.
* The processor receives an XQuery, parses it into an abstract tree representation, optimizes it and finally executes the optimized plan. 
* The Antlr4 tool is used for Java parser generation, and the XML->DOM parser provided with the Java distribution is used for the interface.
* The XML files are accessed using standard DOM interface using JAVA.
* The XQuery processor also includes JOIN optimizations.
