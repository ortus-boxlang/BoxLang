import ortus.boxlang.runtime.scopes.Key;

component extends="testbox.system.BaseSpec" {

    function beforeAll() {
        // setup code that runs before all tests
    }

    function afterAll() {
        // cleanup code that runs after all tests
    }

    function run() {
        describe("A suite", function() {
            it("contains spec with an expectation", function() {
          //  debugBoxContexts(); //abort;
                expect(true).toBeTrue();
            });

         /*    describe("Nested suite", function() {
                it("can have a spec", function() {
                    expect(1).toBe(1);
                });
            }); */
        });
    }
}