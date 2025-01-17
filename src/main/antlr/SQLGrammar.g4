// Based on MIT-licensed SQL Lite ANTLR4 grammar
// https://github.com/antlr/grammars-v4/tree/master/sql/sqlite

// $antlr-format alignTrailingComments on, columnLimit 130, minEmptyLines 1, maxEmptyLinesToKeep 1, reflowComments off
// $antlr-format useTab off, allowShortRulesOnASingleLine off, allowShortBlocksOnASingleLine on, alignSemicolons ownLine

parser grammar SQLGrammar;

options {
    tokenVocab = SQLLexer;
}

parse: (sql_stmt_list)* EOF
;

sql_stmt_list:
    SCOL* sql_stmt (SCOL+ sql_stmt)* SCOL*
;

sql_stmt: (select_stmt)
;

type_name:
    name+? (
        OPEN_PAR signed_number CLOSE_PAR
        | OPEN_PAR signed_number COMMA signed_number CLOSE_PAR
    )?
;

signed_number: (PLUS | MINUS)? NUMERIC_LITERAL
;

cte_table_name:
    table_name (OPEN_PAR column_name ( COMMA column_name)* CLOSE_PAR)?
;

recursive_cte:
    cte_table_name AS_ OPEN_PAR initial_select UNION_ ALL_? recursive_select CLOSE_PAR
;

common_table_expression:
    table_name (OPEN_PAR column_name ( COMMA column_name)* CLOSE_PAR)? AS_ OPEN_PAR select_stmt CLOSE_PAR
;

predicate:
    expr (LT | LT_EQ | GT | GT_EQ) expr
    | expr (ASSIGN | EQ | NOT_EQ1 | NOT_EQ2 | IS_ NOT_ | IS_ | LIKE_) expr
    | predicate AND_ predicate
    | predicate OR_ predicate
    | expr NOT_? LIKE_ expr (ESCAPE_ expr)?
    | expr IS_ NOT_? expr
    | expr NOT_? BETWEEN_ expr AND_ expr
    | expr NOT_? IN_ (OPEN_PAR (expr ( COMMA expr)*)? CLOSE_PAR | subquery_no_alias)
;

expr:
    literal_value
    | BIND_PARAMETER
    | (table_name DOT)? column_name
    | unary_operator expr
    // concat operator
    | expr PIPE2 expr
    // bitwise operators
    | expr (CARET | AMP | PIPE) expr
    // math operators
    | expr (STAR | DIV | MOD) expr
    // math or maybe concat operators
    | expr (PLUS | MINUS) expr
    // Special handling of cast to allow cast( foo as number)
    | CAST_ OPEN_PAR expr AS_ (IDENTIFIER | STRING_LITERAL) CLOSE_PAR
    // special handling of convert to allow convert( foo, number ) or convert( foo, 'number' )
    | CONVERT_ OPEN_PAR expr COMMA (IDENTIFIER | STRING_LITERAL) CLOSE_PAR
    | function_name OPEN_PAR ((DISTINCT_? ALL_? expr ( COMMA expr)*) | STAR)? CLOSE_PAR // filter_clause? over_clause?
    | OPEN_PAR expr CLOSE_PAR
    | case_expr
;

case_expr:
    CASE_ initial_expr = expr? case_when_then+ (ELSE_ else_expr = expr)? END_
;

case_when_then:
    WHEN_ (when_expr = expr | when_predicate = predicate) THEN_ then_expr = expr
;

literal_value:
    NUMERIC_LITERAL
    | STRING_LITERAL
    | NULL_
    | TRUE_
    | FALSE_
;

value_row:
    OPEN_PAR expr (COMMA expr)* CLOSE_PAR
;

pragma_value:
    signed_number
    | name
    | STRING_LITERAL
;

select_stmt:
    select_core (union)* order_by_stmt? limit_stmt?
;

union:
    UNION_ ALL_? DISTINCT_? select_core
;

join_clause:
    table_or_subquery join+
;

join:
    join_operator table_or_subquery join_constraint?
;

select_core:
    SELECT_ top? (DISTINCT_ /*| ALL_*/)? result_column (COMMA result_column)* (
        FROM_ (table_or_subquery (COMMA table_or_subquery)* | join_clause)
    )? (WHERE_ whereExpr = predicate)? (
        GROUP_ BY_ groupByExpr += expr (COMMA groupByExpr += expr)* (
            HAVING_ havingExpr = predicate
        )?
    )? limit_stmt?
;

top:
    TOP NUMERIC_LITERAL
;

factored_select_stmt:
    select_stmt
;

table:
    (schema_name DOT)? table_name (AS_? table_alias)?
;

subquery:
    OPEN_PAR select_stmt CLOSE_PAR AS_? table_alias
;

subquery_no_alias:
    OPEN_PAR select_stmt CLOSE_PAR
;

table_or_subquery:
    table
    | subquery
;

result_column:
    STAR
    | table_name DOT STAR
    | expr ( AS_? column_alias)?
;

join_operator:
    ((LEFT_ | RIGHT_ | FULL_) OUTER_? | INNER_ | CROSS_)? JOIN_
;

join_constraint:
    ON_ predicate
;

simple_function_invocation:
    simple_func OPEN_PAR (expr (COMMA expr)* | STAR) CLOSE_PAR
;

order_by_stmt:
    ORDER_ BY_ ordering_term (COMMA ordering_term)*
;

limit_stmt:
    //LIMIT_ expr ((OFFSET_ | COMMA) expr)?
    LIMIT_ NUMERIC_LITERAL
;

ordering_term:
    //expr (COLLATE_ collation_name)? asc_desc? (NULLS_ (FIRST_ | LAST_))?
    expr asc_desc?
;

asc_desc:
    ASC_
    | DESC_
;

offset:
    COMMA signed_number
;

default_value:
    COMMA signed_number
;

order_by_expr:
    ORDER_ BY_ expr+
;

order_by_expr_asc_desc:
    ORDER_ BY_ expr_asc_desc
;

expr_asc_desc:
    expr asc_desc? (COMMA expr asc_desc?)*
;

//TODO BOTH OF THESE HAVE TO BE REWORKED TO FOLLOW THE SPEC

initial_select:
    select_stmt
;

recursive_select:
    select_stmt
;

unary_operator:
    MINUS
    | PLUS
    | TILDE
    | BANG
;

error_message:
    STRING_LITERAL
;

column_alias:
    IDENTIFIER
    | STRING_LITERAL
;

keyword:
    ALL_
    | ALTER_
    | AND_
    | AS_
    | ASC_
    | BEGIN_
    | BETWEEN_
    | BY_
    | CASE_
    | CAST_
    | CONVERT_
    | CROSS_
    | DESC_
    | DISTINCT_
    | ELSE_
    | END_
    | ESCAPE_
    | FROM_
    | FULL_
    | GROUP_
    | HAVING_
    | IN_
    | INNER_
    | IS_
    | JOIN_
    | LEFT_
    | LIKE_
    | LIMIT_
    | NOT_
    | NULL_
    | ON_
    | OR_
    | ORDER_
    | OUTER_
    | SELECT_
    | THEN_
    | WHEN_
    | WHERE_
    | TRUE_
    | FALSE_
;

// TODO: check all names below

name:
    any_name
;

function_name:
    FUNCTION_NAME
;

schema_name:
    any_name
;

table_name:
    IDENTIFIER
;

table_or_index_name:
    any_name
;

column_name:
    IDENTIFIER
;

collation_name:
    any_name
;

foreign_table:
    any_name
;

index_name:
    any_name
;

trigger_name:
    any_name
;

view_name:
    any_name
;

module_name:
    any_name
;

pragma_name:
    any_name
;

savepoint_name:
    any_name
;

table_alias:
    IDENTIFIER
;

transaction_name:
    any_name
;

window_name:
    any_name
;

alias:
    IDENTIFIER
;

filename:
    any_name
;

base_window_name:
    any_name
;

simple_func:
    any_name
;

aggregate_func:
    any_name
;

table_function_name:
    any_name
;

any_name:
    IDENTIFIER
    | keyword
;