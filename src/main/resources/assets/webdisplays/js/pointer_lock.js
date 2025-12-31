{
    const elemRef = { element: undefined, unadjusted: false };

    Document.prototype.__defineGetter__("pointerLockElement", () => {
        return elemRef['element'];
    });
    Document.prototype.__defineGetter__("webdisplays__unadjustPointerMotion", () => elemRef['unadjusted']);
    Document.prototype.__defineSetter__("pointerLockElement", (v) => {});

    Element.prototype.requestPointerLock = function(unadjustedMovement = false) {
        elemRef['element'] = this;
        elemRef['unadjusted'] = unadjustedMovement;
        document.pointerLockElement = elemRef['element'];

        let bodyRect = document.body.getBoundingClientRect();
        let elemRect = this.getBoundingClientRect();

        let doc = document;
        window.cefQuery({
            request:
            'WebDisplays_PointerElement{' +
                'exists:true,' +
                'x:' + (elemRect.left) + ',' +
                'y:' + (elemRect.top) + ',' +
                'w:' + ((elemRect.right - elemRect.left)) + ',' +
                'h:' + ((elemRect.bottom - elemRect.top)) + ',' +
                'unadjust:' + document.webdisplays__unadjustPointerMotion +
            '}', onSuccess: function(response) {
                doc.dispatchEvent(new Event("pointerlockchange"));
            },
            onFailure: function(error_code, error_message) {
                doc.dispatchEvent(new Event("pointerlockerror"));
            }
        });
    }
    Document.prototype.exitPointerLock = () => {
        elemRef['element'] = undefined;
        elemRef['unadjusted'] = false;
        document.pointerLockElement = elemRef['element'];

        window.cefQuery({
            request: 'WebDisplays_PointerElement{exists: false}',
            onSuccess: function(response) {},
            onFailure: function(error_code, error_message) {}
        });
    }
}
