(function () {
    var STORAGE_KEY = "interview.theme";

    function currentTheme() {
        return document.documentElement.dataset.theme === "light" ? "light" : "dark";
    }

    function applyTheme(theme) {
        var next = theme === "light" ? "light" : "dark";
        document.documentElement.dataset.theme = next;
        localStorage.setItem(STORAGE_KEY, next);
        document.querySelectorAll("[data-theme-choice]").forEach(function (button) {
            button.setAttribute("aria-pressed", button.dataset.themeChoice === next ? "true" : "false");
        });
    }

    document.addEventListener("DOMContentLoaded", function () {
        applyTheme(currentTheme());
        document.querySelectorAll("[data-settings]").forEach(function (button) {
            var panel = button.parentElement.querySelector(".theme-panel");
            button.addEventListener("click", function (event) {
                event.stopPropagation();
                panel.hidden = !panel.hidden;
            });
        });
        document.querySelectorAll("[data-theme-choice]").forEach(function (choice) {
            choice.addEventListener("click", function (event) {
                event.stopPropagation();
                applyTheme(choice.dataset.themeChoice);
            });
        });
        document.addEventListener("click", function () {
            document.querySelectorAll(".theme-panel").forEach(function (panel) {
                panel.hidden = true;
            });
        });
    });
})();
