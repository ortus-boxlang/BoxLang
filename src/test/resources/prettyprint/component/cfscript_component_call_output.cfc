component {

	function send() {
		cfhttp(
			method       = "post"
			url          = "#variables.POSTMARK_APIURL#"
			charset      = "utf-8"
			result       = "httpResults"
			redirect     = "#true#"
			throwOnError = "#true#"
			timeout      = "#variables.DEFAULT_TIMEOUT#"
			useragent    = "ColdFusion-cbMailServices"
		){
			cfhttpparam( type = "header" name = "Accept" value = "application/json" );
			cfhttpparam( type = "header" name = "Content-type" value = "application/json" );
			cfhttpparam( type = "header" name = "X-Postmark-Server-Token" value = "#getProperty( "apiKey" )#" );
			cfhttpparam( type = "body" encoded = "no" value = "#arguments.jsonPayload#" );
		}
	}

}