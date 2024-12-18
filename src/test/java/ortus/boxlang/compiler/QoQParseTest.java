/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.compiler;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.compiler.parser.SQLParser;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.Struct;

public class QoQParseTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );
	static Key			q		= new Key( "q" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@Test
	public void testMetadataVisitor() {
		ParsingResult	result	= new SQLParser().parse(
		    """
		    select foo, bar b, bum as b2, *
		    from mytable t
		    where true
		    order by t.baz
		    limit 5
		      """
		);
		IStruct			data	= Struct.of(
		    "file", null,
		    "result", result
		);
		BoxRuntime.getInstance().announce( "onParse", data );

		assertThat( result ).isNotNull();
		System.out.println( result.getIssues() );
		assertThat( result.isCorrect() ).isTrue();
		System.out.println( result.getRoot().toJSON() );
	}

	@Test
	public void testRunQoQ() {
		instance.executeSource(
		    """
		                 qryEmployees = queryNew(
		     	"name,age,dept,supervisor",
		     	"varchar,integer,varchar,varchar",
		     	[
		     		["luis",43,"Exec","luis"],
		     		["brad",44,"IT","luis"],
		     		["Jon",45,"HR","luis"]
		     		]
		     	)
		                 qryDept = queryNew( "name,code", "varchar,integer", [["IT",404],["Exec",200],["Janitor",200]] )
		                         q = queryExecute( "
		           select e.*, [s].[name] as [supName], d.name as deptname
		        from [variables].[qryEmployees] e
		     inner join qryEmployees s on e.supervisor = s.name
		      full join qryDept d on e.dept = d.name
		    where d.name in ('IT','HR')
		                ",
		                      	[],
		                      	{ dbType : "query" }
		                      );
		                   println( q )
		                      """,
		    context );
	}

	@Test
	public void testRunQoQUnion() {
		instance.executeSource(
		    """
		                 qryDept = queryNew( "name,code", "varchar,integer", [["IT",404],["Exec",200],["Janitor",200]] )
		                q = queryExecute( "
		                select name as col from qryDept
		          union all select 'bar' as sfd
		       union select 'foo' as col
		    order by col desc
		                        ",
		                              	[],
		                              	{ dbType : "query" }
		                              );
		                           println( q )
		                              """,
		    context );
	}

	@Test
	public void testRunQoQUnionDistinct() {
		instance.executeSource(
		    """
		             q = queryExecute( "
		       select 'foo' as col
		       union select 'foo'
		    union select 'foo'
		    union select 'foo'
		    union select 'foo'
		    union select 'foo'
		    union select 'foo'  -- Actual de-duplication runs here
		    union all select 'foo'
		    union all select 'foo'
		                     ",
		                           	[],
		                           	{ dbType : "query" }
		                           );
		                        println( q )
		                           """,
		    context );
		assertThat( variables.getAsQuery( q ).size() ).isEqualTo( 3 );
	}

	@Test
	public void testSubquery() {
		instance.executeSource(
		    """
		                  q = queryExecute( "
		         select col as brad from (
		        			select 'foo' as col
		    			union select 'bar'
		       ) as t
		    order by brad asc

		                         ",
		                               	[],
		                               	{ dbType : "query" }
		                               );
		                            println( q )
		                               """,
		    context );
	}

	@Test
	public void testSubquery2() {
		instance.executeSource(
		    """
		                 qryEmployees = queryNew(
		     	"name,age,dept,supervisor",
		     	"varchar,integer,varchar,varchar",
		     	[
		     		["luis",43,"Exec","luis"],
		     		["brad",44,"IT","luis"],
		     		["Jon",45,"HR","luis"]
		     		]
		     	)
		                 qryDept = queryNew( "name,code", "varchar,integer", [["IT",404],["Exec",200],["Janitor",200]] )
		                         q = queryExecute( "
		           select e.*, s.name as supName, d.name as deptname
		        from (select * from qryEmployees) e
		     inner join (select * from qryEmployees) s on e.supervisor = s.name
		      full join (select * from qryDept) d on e.dept = d.name
		    where d.name in ('IT','HR')
		                ",
		                      	[],
		                      	{ dbType : "query" }
		                      );
		                   println( q )
		                      """,
		    context );
	}

	@Test
	public void testInSubquery() {
		instance.executeSource(
		    """

		    qryMen = queryNew( "name", "varchar", [["Luis"],["Jon"],["Brad"]] )
		    qryAll = queryNew( "name", "varchar", [["Luis"],["Jon"],["Brad"],["Esme"],["Myrna"]] )
		                     q = queryExecute( "
		            select *
		    	 from qryAll
		    	 where name in ( select name from qryMen )

		                            ",
		                                  	[],
		                                  	{ dbType : "query" }
		                                  );
		                               println( q )
		                     q = queryExecute( "
		            select *
		    	 from qryAll
		    	 where name not in ( select name from qryMen )

		                            ",
		                                  	[],
		                                  	{ dbType : "query" }
		                                  );
		                               println( q )
		                                  """,
		    context );
	}

	@Test
	public void testcustomFunc() {
		instance.executeSource(
		    """
		       import ortus.boxlang.runtime.jdbc.qoq.QoQFunctionService;
		       import ortus.boxlang.runtime.scopes.Key;
		       import ortus.boxlang.runtime.types.QueryColumnType;

		    // Register a custom function
		       QoQFunctionService.registerCustom( Key.of("reverse"), ::reverse, QueryColumnType.VARCHAR, 1, getBoxContext() );

		                           q = queryExecute( "
		                  select reverse( 'Brad' ) as rev
		                                  ",
		                                        	[],
		                                        	{ dbType : "query" }
		                                        );
		                                     println( q )

		                                        """,
		    context );
	}

	@Test
	public void testAggregate() {
		instance.executeSource(
		    """
		              qryEmployees = queryNew(
		        	"name,age,dept,supervisor",
		        	"varchar,integer,varchar,varchar",
		        	[
		        		["luis",43,"Exec","luis"],
		        		["brad",44,"IT","luis"],
		        		["Jon",45,"HR","luis"]
		           		]
		           	)

		        q = queryExecute( "
		                        select count( 1 ) count,
		        	[max](age) maxAge,
		        	min([e].[age]) minAge,
		        	min(age+0)+1 minAgePlusOne,
		        	concat( 'foo', cast( min(age) as [string])) aggregateInScalar,
		        	concat( 'foo', cast( max(age) as string)) aggregateInScalar2,
		      	sum( age ) sumAge,
		    avg(age) avgAge
		        		  from qryEmployees [e]
		                                        ",
		                                              	[],
		                                              	{ dbType : "query" }
		                                              );
		                                           println( q )

		                                              """,
		    context );
	}

	@Test
	public void testCast() {
		instance.executeSource(
		    """
		    q = queryExecute( "
		       select cast( 5 as string) + 4 as result, 5 as result2, cast( 5 as 'string') as result3
		    ",
		                                          	[],
		                                          	{ dbType : "query" }
		                                          );
		    						  println( q )
		    						  result = q

		                                          """,
		    context );
		Query q = variables.getAsQuery( result );
		assertThat( q.getColumn( result ).getType() ).isEqualTo( QueryColumnType.VARCHAR );
		assertThat( q.getColumn( Key.of( "result2" ) ).getType() ).isEqualTo( QueryColumnType.DOUBLE );
		assertThat( q.getColumn( Key.of( "result3" ) ).getType() ).isEqualTo( QueryColumnType.VARCHAR );
		instance.executeSource(
		    """
		    q = queryExecute( "
		       select convert( 5, [string]) + 4 as result, 5 as result2, convert( 5, 'string') as result3
		    ",
		                                          	[],
		                                          	{ dbType : "query" }
		                                          );
		    						  println( q )
		    						  result = q

		                                          """,
		    context );
		q = variables.getAsQuery( result );
		assertThat( q.getColumn( result ).getType() ).isEqualTo( QueryColumnType.VARCHAR );
		assertThat( q.getColumn( Key.of( "result2" ) ).getType() ).isEqualTo( QueryColumnType.DOUBLE );
		assertThat( q.getColumn( Key.of( "result3" ) ).getType() ).isEqualTo( QueryColumnType.VARCHAR );
	}

	@Test
	public void testAggregateGroup() {
		instance.executeSource(
		    """
		                    qryEmployees = queryNew(
		              	"name,age,dept,supervisor",
		              	"varchar,integer,varchar,varchar",
		              	[
		              		["luis",43,"Exec","luis"],
		              		["brad",44,"IT","luis"],
		              		["jacob",35,"IT","luis"],
		              		["Jon",45,"HR","luis"]
		                 		]
		                 	)

		              q = queryExecute( "
		          select  upper( dept) as dept, count(1), max(name), min(name), GROUP_CONCAT( name) as names, GROUP_CONCAT( name, ' | ') as namesPipe
		          from qryEmployees as t
		          group by dept
		       having (count(1)+1) > 1
		    order by count(1) desc
		                                              ",
		                                                    	[],
		                                                    	{ dbType : "query" }
		                                                    );
		                                                 println( q )

		                                                    """,
		    context );
	}

}
