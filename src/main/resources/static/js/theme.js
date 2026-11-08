(function () {
    var STORAGE_KEY = "interview.theme";

    function currentTheme() {
        return document.documentElement.dataset.theme === "light" ? "light" : "dark";
    }

    function applyTheme(theme) {
        var next = theme === "light" ? "light" : "dark";
        document.documentElement.dataset.theme = next;
        localStorage.setItem(STORAGE_KEY, next);
        var label = next === "light" ? "Switch to dark theme" : "Switch to light theme";
        document.querySelectorAll("[data-theme-toggle]").forEach(function (button) {
            button.setAttribute("aria-label", label);
            button.setAttribute("title", label);
        });
    }

    document.addEventListener("DOMContentLoaded", function () {
        applyTheme(currentTheme());
        document.querySelectorAll("[data-theme-toggle]").forEach(function (button) {
            button.addEventListener("click", function () {
                applyTheme(currentTheme() === "light" ? "dark" : "light");
            });
        });
    });
})();
