parser grammar BoxScriptGrammar;

options {
	tokenVocab = BoxScriptLexer;
}

import BaseScriptGrammar;

// This is the top level rule, which allow imports always, followed by a component, or an interface, or just a bunch of statements.
script:
	importStatement* (
		boxClass
		| interface
		| functionOrStatement*
	)
	| EOF;

// TODO: This belongs only in the BL grammar. import java:foo.bar.Baz as myAlias;
importStatement:
	IMPORT (prefix = identifier COLON)? fqn (DOT STAR)? (
		AS alias = identifier
	)? eos?;