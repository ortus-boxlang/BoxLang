// Static reference to System (java proxy?)
variables['system'] = createObject('java','java.lang.System');
// Static reference to String
variables.greeting = createObject('java','java.lang.String')
// call constructor to create instance
.init( 'Hello' );

// Conditional, requires operation support
if( variables.greeting == 'Hello' ) {
// De-referencing "out" and "println" and calling Java method via invoke dynamic
variables.system.out.println(
  // Multi-line statement, expression requires concat operator and possible casting
  // Unscoped lookup requires scope search
  greeting & " world"
)
}