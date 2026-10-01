interface displayname="ProfilerInterfaceCF" {

	static function bump() {
		return 1;
	}

	static function display() {
		var x = 1 + 2;
		return displayname;
	}

	// A DEFAULT method (non-static, with a body) — run on an implementing
	// class's instance.
	default function greet() {
		var msg = "hello from cfc interface";
		return msg;
	}

	// A DEFAULT method we NEVER call — its body must stay RED (missed).
	default function greetUnused() {
		var unusedPrefix = "unused";
		return unusedPrefix & "!";
	}

	// An ABSTRACT method — no body, so it has NO spans at all.
	function abstractOnly();

}