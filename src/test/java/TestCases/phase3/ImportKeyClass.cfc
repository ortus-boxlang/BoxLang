component {
	import ortus.boxlang.runtime.scopes.Key;
	any function main(){
		println( Key::object.getName() );
	}
}