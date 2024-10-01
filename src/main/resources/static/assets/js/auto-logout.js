setInterval(function() {
    const div = document.querySelector("#signOutTimerInSeconds");
    const count = div.textContent * 1 - 1;
    div.textContent = count;
    if (count <= 0) {
        window.location.replace(document.querySelector("#signOutLink").textContent);
    }
}, 1000);
