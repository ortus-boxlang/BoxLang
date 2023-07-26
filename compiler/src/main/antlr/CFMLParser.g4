parser grammar CFMLParser;

options { tokenVocab=CFMLLexer; }

htmlDocument    
    : (scriptlet | SEA_WS)* xml? (scriptlet | SEA_WS)* dtd? (scriptlet | SEA_WS)* 
    	( cfmlElement | htmlElements)*
    	EOF
    ;

cfmlComment:
    CFML_COMMENT;

cfmlCloseTag:
    TAG_OPEN TAG_SLASH htmlTagName TAG_CLOSE;

htmlElements
    : htmlMisc* htmlElement htmlMisc*
    ; 

htmlElement     
    : cfmlCloseTag
    | TAG_OPEN htmlTagName htmlAttribute* TAG_CLOSE htmlContent TAG_OPEN TAG_SLASH htmlTagName TAG_CLOSE
    | TAG_OPEN htmlTagName htmlAttribute* TAG_SLASH_CLOSE
    | TAG_OPEN htmlTagName htmlAttribute* TAG_CLOSE
    
    | scriptlet
    | script
    | style
    ;

cfmlElement
    : cfmlComment
    | cfset
    | cfcomponent
    | cffunction
    | cfinterface
    | htmlComment
    | cfscript
    ;

cfmlStatement
    : cfset
    | cfdump
    | htmlComment
    | cfscript
    | cfargument
    | cfreturn
    | cfif
    | cfquery
    | cfthrow
    | cfloop
    | cfparam
    | cftry
    | cfcatch
    | cfabort
    | cflock
    | cfinclude
    | cfinvoke
    | cfinvoke1
    | cfinvokeargument
    | cffile
    ;

cfcomponent
    : TAG_OPEN CFCOMPONENT htmlAttribute* (TAG_SLASH_CLOSE | TAG_CLOSE)
        cfmlElement*
      TAG_OPEN TAG_SLASH CFCOMPONENT TAG_CLOSE
    ;
cfinterface
    : TAG_OPEN CFINTERFACE htmlAttribute* (TAG_SLASH_CLOSE | TAG_CLOSE)
        cfmlElement*
      TAG_OPEN TAG_SLASH CFINTERFACE TAG_CLOSE
    ;

cffunction
    :   TAG_OPEN CFFUNCTION htmlAttribute* (TAG_SLASH_CLOSE | TAG_CLOSE)
        cfmlStatement*
        TAG_OPEN TAG_SLASH CFFUNCTION TAG_CLOSE
    ;

cfset
	: TAG_OPEN CFSET cfexpression (TAG_SLASH_CLOSE | TAG_CLOSE)
	;

cfdump
    : TAG_OPEN CFDUMP htmlAttribute* (TAG_SLASH_CLOSE | TAG_CLOSE)
    ;
cfscript
    :   CFSCRIPT_OPEN CFSCRIPT_BODY
    ;
//    : TAG_OPEN CFSCRIPT htmlAttribute* (TAG_SLASH_CLOSE | TAG_CLOSE)
//      cfcode
//      TAG_OPEN TAG_SLASH CFSCRIPT TAG_CLOSE
//    ;

cfcode
    : HTML_TEXT
    ;

cfargument
    : TAG_OPEN CFARGUMENT htmlAttribute* (TAG_SLASH_CLOSE | TAG_CLOSE)
    ;

cfreturn
    : TAG_OPEN CFRETURN cfexpression (TAG_SLASH_CLOSE | TAG_CLOSE)
    ;

cfif
    : TAG_OPEN CFIF cfexpression (TAG_SLASH_CLOSE | TAG_CLOSE)
      cfmlStatement*
      (TAG_OPEN CFELSEIF cfexpression  (TAG_SLASH_CLOSE | TAG_CLOSE)
            cfmlStatement*)*
      (TAG_OPEN CFELSE  (TAG_SLASH_CLOSE | TAG_CLOSE)
      cfmlStatement*)*
     TAG_OPEN TAG_SLASH CFIF TAG_CLOSE
    ;

cfquery
    : TAG_OPEN CFQUERY htmlAttribute* (TAG_SLASH_CLOSE | TAG_CLOSE)
      sqlcode
      TAG_OPEN TAG_SLASH CFQUERY TAG_CLOSE
    ;
sqlcode
    : HTML_TEXT
    ;

cfthrow
    : TAG_OPEN CFTHROW htmlAttribute* (TAG_SLASH_CLOSE | TAG_CLOSE)
    ;

cfloop
    :   TAG_OPEN CFLOOP htmlAttribute* (TAG_SLASH_CLOSE | TAG_CLOSE)
        cfmlStatement*
        TAG_OPEN TAG_SLASH CFLOOP TAG_CLOSE
    ;

cfparam
    : TAG_OPEN CFPARAM htmlAttribute* (TAG_SLASH_CLOSE | TAG_CLOSE)
    ;

cftry
    :   TAG_OPEN CFTRY htmlAttribute* (TAG_SLASH_CLOSE | TAG_CLOSE)
        cfmlStatement*
        TAG_OPEN TAG_SLASH CFTRY TAG_CLOSE
    ;

cfcatch
    :   TAG_OPEN CFCATCH htmlAttribute* (TAG_SLASH_CLOSE | TAG_CLOSE)
        cfmlStatement*
        TAG_OPEN TAG_SLASH CFCATCH TAG_CLOSE
    ;

cfabort
    : TAG_OPEN CFABORT htmlAttribute* (TAG_SLASH_CLOSE | TAG_CLOSE)
    ;

cflock
    :   TAG_OPEN CFLOCK htmlAttribute* (TAG_SLASH_CLOSE | TAG_CLOSE)
        cfmlStatement*
        TAG_OPEN TAG_SLASH CFLOCK TAG_CLOSE
    ;

cfinclude
    : TAG_OPEN CFINCLUDE htmlAttribute* (TAG_SLASH_CLOSE | TAG_CLOSE)
    ;

cfinvoke
    : TAG_OPEN CFINVOKE htmlAttribute* (TAG_SLASH_CLOSE | TAG_CLOSE)
    ;
cfinvoke1
    : TAG_OPEN CFINVOKE htmlAttribute* (TAG_SLASH_CLOSE | TAG_CLOSE)
             cfmlStatement*
      TAG_OPEN TAG_SLASH CFINVOKE TAG_CLOSE
    ;
cfinvokeargument
    : TAG_OPEN CFINVOKEARGUMENT htmlAttribute* (TAG_SLASH_CLOSE | TAG_CLOSE)
    ;

cffile
    : TAG_OPEN CFFILE htmlAttribute* TAG_CLOSE
    ;

cfexpression
	: EXPRESSION
	; 

htmlContent     
    : htmlChardata? ((htmlElement | xhtmlCDATA | htmlComment) htmlChardata?)*
    ;

htmlAttribute   
    : htmlAttributeName TAG_EQUALS htmlAttributeValue
    | htmlAttributeName
    ;

htmlAttributeName
    : TAG_NAME
    ;

htmlAttributeValue
    : ATTVALUE_VALUE
    ;

htmlTagName
    : TAG_NAME
    ;

htmlChardata    
    : HTML_TEXT 
    | SEA_WS
    ;

htmlMisc        
    : htmlComment 
    | SEA_WS
    ;

htmlComment
    : HTML_COMMENT
    | HTML_CONDITIONAL_COMMENT
    ;

xhtmlCDATA
    : CDATA
    ;

dtd
    : DTD
    ;

xml
    : XML_DECLARATION
    ;

scriptlet
    : SCRIPTLET
    ;

script
    : SCRIPT_OPEN ( SCRIPT_BODY | SCRIPT_SHORT_BODY)
    ;

style
    : STYLE_OPEN ( STYLE_BODY | STYLE_SHORT_BODY)
    ;