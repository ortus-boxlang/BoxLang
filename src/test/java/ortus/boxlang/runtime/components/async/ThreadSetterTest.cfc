component accessors=true {

	property name="myValue" type="string";

	function doThreadedSet( required string newValue ) {
		thread name="setterThread" newValue=arguments.newValue {
			try {
				setMyValue( newValue );
			} catch ( any e ) {
				myvalue = e.message;
			}
		}
		thread name="setterThread" action="join";
	}

}
