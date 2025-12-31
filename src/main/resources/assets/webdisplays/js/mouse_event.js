{
    let mev = new MouseEvent("mousemove", {
        view: window,
        bubbles: true,
        cancelable: false,
        clientX: %xCoord%,
        clientY: %yCoord%,
        movementX: %xDelta%,
        movementY: %yDelta%
    });
    document.pointerLockElement.dispatchEvent(mev);
}