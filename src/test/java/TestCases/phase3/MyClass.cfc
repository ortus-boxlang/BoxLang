
import foo;
import java.lang.System;

/**
 * This is my class description
 *
 * @brad wood
 * @luis
 */
@foo "bar"
component implements="Luis,Jorge" singleton gavin="pickin" inject {

    variables.setup=true;
    System.out.println( "word" );
    request.foo="bar";
    isInitted = false;
    println( "current template is " & getCurrentTemplatePath() );
    printLn( foo() )

    function init() {
       isInitted = true;
    }

    function foo() {
     return "I work! #bar()# #variables.setup# #setup# #request.foo# #isInitted#";
    }

    private function bar() {
     return "whee";
    }

    function getThis() {
     return this;
    }

    function runThisFoo() {
        return this.foo();
    }

 }