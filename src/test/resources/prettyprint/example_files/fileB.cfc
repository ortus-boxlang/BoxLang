component {
    public function doSomething( required struct params ){
        var data = "";
        switch ( requestType ) {
			case "group": {
				data &= generateData(
					propA    = params.propA,
					propB    = params.propB,
					propC = packageInfo.propC,
					params      = params
				);

				data &= appendExtraData( params );

				break;
			}
			default: {
				data &= generateData(
					propA    = params.propA,
					propB    = params.propB,
					propC = "otherValue",
					params      = params
				);
			}
		}

		var response = doRequest(
			"/endpoint",
			data,
			"http://somedomain.example.com"
		);

        return response;
    }
}