/**
* This is my interface description
*
* @brad wood
* @luis
*/
interface singleton gavin="pickin" inject foo="bar" {

    function init();

    function foo();

    private function bar();

    default function myDefaultMethod() {
        return this.foo();
    }

}