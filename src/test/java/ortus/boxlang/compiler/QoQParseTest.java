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
import ortus.boxlang.runtime.types.Struct;

public class QoQParseTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

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
		           select e.*, s.name as supName, d.name as deptname
		        from qryEmployees e
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

}
