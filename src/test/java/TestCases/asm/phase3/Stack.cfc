component {
    function init() {
        stack = [];
        return this;
    }

    function empty() {
        return stack.len() == 0;
    }
}