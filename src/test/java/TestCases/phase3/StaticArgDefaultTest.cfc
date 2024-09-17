component {

    static {
        static.FIXED = 200
    }

    public function init(required string content, numeric status=StaticArgDefaultTest::FIXED ) {
        this.content = arguments.content
        this.status = arguments.status
    }

    public static function create(required string message) {
        return new StaticArgDefaultTest(message);
    }
}