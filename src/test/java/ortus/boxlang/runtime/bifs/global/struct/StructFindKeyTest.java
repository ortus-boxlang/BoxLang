
/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ortus.boxlang.runtime.bifs.global.struct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

public class StructFindKeyTest {

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

	@DisplayName( "It tests the BIF StructFindKey" )
	@Test
	public void testBif() {
		instance.executeSource(
		    """
		    myStruct = {
		    	cow: {
		    		total: 12
		    	},
		    	pig: {
		    		total: 5
		    	},
		    	cat: {
		    		total: 3
		    	}
		    };
		       result = StructFindKey( myStruct, "total" );
		    emptyResult = StructFindKey( myStruct, "bird", "all" );
		       """,
		    context );

		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( variables.getAsArray( result ).size(), 1 );
		assertEquals( variables.getAsArray( Key.of( "emptyResult" ) ).size(), 0 );
		instance.executeSource(
		    """
		    myStruct = {
		    	cow: {
		    		total: 12
		    	},
		    	pig: {
		    		total: 5
		    	},
		    	cat: {
		    		total: 3
		    	}
		    };
		       result = StructFindKey( myStruct, "total", "all" );
		       """,
		    context );

		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( variables.getAsArray( result ).size(), 3 );

		instance.executeSource(
		    """
		    myStruct = {
		    	cow: {
		    		total: 12
		    	},
		    	pig: {
		    		total: 5
		    	},
		    	cat: {
		    		total: 3
		    	}
		    };
		       result = StructFindKey( myStruct, "pig.total" );
		       """,
		    context );

		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( variables.getAsArray( result ).size(), 1 );
		IStruct firstItem = StructCaster.cast( variables.getAsArray( result ).get( 0 ) );
		assertTrue( firstItem.containsKey( Key.owner ) );
		assertTrue( firstItem.containsKey( Key.path ) );
		assertTrue( firstItem.containsKey( Key.value ) );
		assertEquals( firstItem.get( Key.value ), 5 );

	}

	@DisplayName( "It tests the member function for StructFindKey" )
	@Test
	public void testMemberFunction() {

		instance.executeSource(
		    """
		    myStruct = {
		    	cow: {
		    		total: 12
		    	},
		    	pig: {
		    		total: 5
		    	},
		    	cat: {
		    		total: 3
		    	}
		    };
		       result = myStruct.findKey( "total" );
		       """,
		    context );

		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( variables.getAsArray( result ).size(), 1 );
		instance.executeSource(
		    """
		    myStruct = {
		    	cow: {
		    		total: 12
		    	},
		    	pig: {
		    		total: 5
		    	},
		    	cat: {
		    		total: 3
		    	}
		    };
		       result = myStruct.findKey( "total", "all" );
		       """,
		    context );

		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( variables.getAsArray( result ).size(), 3 );

		instance.executeSource(
		    """
		    myStruct = {
		    	cow: {
		    		total: 12
		    	},
		    	pig: {
		    		total: 5
		    	},
		    	cat: {
		    		total: 3
		    	}
		    };
		       result = myStruct.findKey( "pig.total" );
		    result[ 1 ].owner.MY_NEW_KEY = "do-something";
		       """,
		    context );

		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( variables.getAsArray( result ).size(), 1 );
		assertEquals( StructCaster.cast( variables.getAsArray( result ).get( 0 ) ).get( Key.value ), 5 );

		IStruct pigStruct = variables.getAsStruct( Key.of( "myStruct" ) ).getAsStruct( Key.of( "pig" ) );
		assertTrue( pigStruct.containsKey( Key.of( "MY_NEW_KEY" ) ) );
		assertEquals( pigStruct.getAsString( Key.of( "MY_NEW_KEY" ) ), "do-something" );
	}

	@DisplayName( "It tests the owner values" )
	@Test
	public void testOwnerValues() {
		//@formatter:off
		instance.executeSource(
		    """
			myStruct = {
			horse: nullValue(),
			bird: {
				total: nullValue(),
				species : {
					parrot: {
						size : "large",
						total: 1,
						names : [
							"Polly",
							"Jack",
							"Fred"
						]
					},
					finch: {
						size : "small",
						total: 2
					},
					duck: {
						size : "large",
						total: 3
					},
				}
			},
			cow: {
				total: 12
			},
			pig: {
				total: 5
			},
			cat: {
				total: 3
			}
		};
		result = StructFindKey( myStruct, "pig.total" );
		resultTop = StructFindKey( myStruct, "bird", "all" );
		resultTopLength = resultTop.len();
		resultBird = resultTop.first();
		resultOwner = resultBird.owner;
		resultNested = structFindKey( myStruct, "size", "all" );
		nestedOwner = resultNested.first().owner;
		resultParrotNames = structFindKey( myStruct, "bird.species.parrot.names", "all" );
		parrotResult = resultParrotNames.first();
		""",
		context );
		//@formatter:on
		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( 1, variables.getAsArray( result ).size() );
		assertEquals( 1, variables.getAsArray( Key.of( "resultTop" ) ).size() );
		assertEquals( Struct.class, variables.get( Key.of( "resultOwner" ) ).getClass() );
		assertEquals( 1, variables.getAsInteger( Key.of( "resultTopLength" ) ) );
		assertEquals( variables.getAsStruct( Key.of( "resultBird" ) ).getAsStruct( Key.of( "value" ) ),
		    variables.getAsStruct( Key.of( "resultOwner" ) ).getAsStruct( Key.of( "bird" ) ) );
		assertEquals( Array.class, variables.get( Key.of( "resultNested" ) ).getClass() );
		assertEquals( 3, variables.getAsArray( Key.of( "resultNested" ) ).size() );
		assertEquals( Struct.class, variables.get( Key.of( "nestedOwner" ) ).getClass() );
		// With the correct owner behavior, nestedOwner should be the immediate parent struct containing "size"
		// For the "size" key, the owner should be the species struct (parrot, finch, duck), not the root
		IStruct nestedOwnerStruct = StructCaster.cast( variables.get( Key.of( "nestedOwner" ) ) );
		// The owner should contain "size" and "total" keys (for parrot, finch, or duck struct)
		assertTrue( nestedOwnerStruct.containsKey( Key.of( "size" ) ) );
		assertTrue( nestedOwnerStruct.containsKey( Key.of( "total" ) ) );
		// The owner should NOT be the root struct (should not contain top-level keys like "bird", "cow")
		assertTrue( !nestedOwnerStruct.containsKey( Key.of( "bird" ) ) );
		assertTrue( !nestedOwnerStruct.containsKey( Key.of( "cow" ) ) );
		assertEquals( Array.class, variables.get( Key.of( "resultParrotNames" ) ).getClass() );
		assertEquals( 1, variables.getAsArray( Key.of( "resultParrotNames" ) ).size() );
		assertEquals( Array.class, variables.getAsStruct( Key.of( "parrotResult" ) ).get( Key.value ).getClass() );
	}

	@DisplayName( "It tests top level struct find with arrays" )
	@Test
	public void testTopLevelArrays() {
		//@formatter:off
		instance.executeSource(
		    """
			myStruct = {
				fruits : nullValue(),
				animals: [
					{
						type: "dog",
						age: 12,
						breed: "collie"
					},
					{
						type: "cat",
						age: 3,
						breed: "siamese"
					},
					{
						type: "pig",
						age: 5,
						breed: nullValue()
					}
				]
			};
			result = StructFindKey( myStruct, "animals", "all" );
			""",
		context );
		//@formatter:on
		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( 1, variables.getAsArray( result ).size() );

	}

	@DisplayName( "It tests that owner modifications are reflected in the original struct" )
	@Test
	public void testOwnerModification() {
		//@formatter:off
		instance.executeSource(
		    """
		    void function appendMyNewKey(required struct results) {
				var matches = StructFindKey(arguments.results, "needle", "all");
				for (var match in matches) {
					match.owner["MY_NEW_KEY"] = "do-something-with:#match.value.id#";
				}
			}

			result = mockResponse();
			appendMyNewKey( result );

			struct function mockResponse() {
				return {
					"alpha" : {
						"charlie" : {
							"needle" : {
								"id" : "acn"
							},
							"delta" : {
								"hotel" : {
									"needle" : {
										"id" : "acd"
									}
								}
							}
						}
					},
					"bravo" : {
						"echo" : {
							"foxtrot"     : {
								"golf" : {
									"needle" : {
										"id" : "befg"
									}
									
								}
							}
						}
					}
				};
			}
		    """,
		    context );
		//@formatter:on

		// Verify that the modifications are reflected in the original myStruct
		IStruct	myStruct	= variables.getAsStruct( result );
		IStruct	level2		= myStruct.getAsStruct( Key.of( "alpha" ) ).getAsStruct( Key.of( "charlie" ) );
		IStruct	level3		= level2.getAsStruct( Key.of( "delta" ) ).getAsStruct( Key.of( "hotel" ) );

		// Check that the new key was added to level2 through the owner reference
		assertTrue( level2.containsKey( Key.of( "MY_NEW_KEY" ) ) );
		assertTrue( level2.getAsString( Key.of( "MY_NEW_KEY" ) ).contains( "do-something" ) );

		assertTrue( level3.containsKey( Key.of( "MY_NEW_KEY" ) ) );
		assertTrue( level3.getAsString( Key.of( "MY_NEW_KEY" ) ).contains( "do-something" ) );

		IStruct levelBravo = myStruct.getAsStruct( Key.of( "bravo" ) ).getAsStruct( Key.of( "echo" ) )
		    .getAsStruct( Key.of( "foxtrot" ) ).getAsStruct( Key.of( "golf" ) );

		// Check that the new key was added to levelBravo through the owner reference
		assertTrue( levelBravo.containsKey( Key.of( "MY_NEW_KEY" ) ) );
		assertTrue( levelBravo.getAsString( Key.of( "MY_NEW_KEY" ) ).contains( "do-something" ) );

	}

}
