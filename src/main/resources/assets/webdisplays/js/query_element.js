try {
    let focusedElement = %type%;
    if (focusedElement == null || focusedElement == document.body) {
        window.cefQuery({
            request: 'WebDisplays_%Type%{exists: false}',
            onSuccess: function(response) {},
            onFailure: function(error_code, error_message) {}
        });
    } else {
        let bodyRect = document.body.getBoundingClientRect();
        let elemRect = focusedElement.getBoundingClientRect();

        window.cefQuery({
            request: 'WebDisplays_%Type%{' +
                'exists:true,' +
                'x:' + (elemRect.left) + ',' +
                'y:' + (elemRect.top) + ',' +
                'w:' + ((elemRect.right - elemRect.left)) + ',' +
                'h:' + ((elemRect.bottom - elemRect.top)) + %extra%
            '}', onSuccess: function(response) {},
            onFailure: function(error_code, error_message) {}
        });
    }
} catch (err) {
    console.error(err);
    window.cefQuery({
        request: 'WebDisplays_%Type%{exists: false}',
        onSuccess: function(response) {},
        onFailure: function(error_code, error_message) {}
    });
}