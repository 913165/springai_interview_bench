(function () {
    function questionId() {
        const parts = window.location.pathname.split("/").filter(Boolean);
        const index = parts.indexOf("questions");
        if (index >= 0 && parts[index + 1]) {
            return decodeURIComponent(parts[index + 1]);
        }
        return new URLSearchParams(window.location.search).get("id");
    }

    function showMissing(message) {
        document.getElementById("loading").hidden = true;
        document.getElementById("article").hidden = true;
        document.getElementById("missing").hidden = false;
        document.getElementById("missing-message").textContent = message;
    }

    function setPager(link, question) {
        if (!question) {
            link.textContent = link.dataset.label;
            link.removeAttribute("href");
            link.setAttribute("aria-disabled", "true");
            return;
        }
        link.href = "/questions/" + encodeURIComponent(question.id);
        link.textContent = link.dataset.label + ": " + question.title;
        link.removeAttribute("aria-disabled");
    }

    document.addEventListener("DOMContentLoaded", async function () {
        const id = questionId();
        if (!id) {
            showMissing("This address does not include a question id.");
            return;
        }
        try {
            const question = await Interview.request("/api/questions/" + encodeURIComponent(id));
            const all = await Interview.request("/api/questions");
            const position = all.findIndex(function (item) { return item.id === question.id; });
            document.title = question.title + " · Spring AI";
            document.getElementById("loading").hidden = true;
            document.getElementById("article").hidden = false;
            document.getElementById("question-title").textContent = question.title;
            document.getElementById("question-category").textContent = question.category;
            const difficulty = document.getElementById("question-difficulty");
            difficulty.textContent = question.difficulty;
            difficulty.className = "chip " + Interview.difficultyClass(question.difficulty);
            document.getElementById("question-updated").textContent = "Updated " + Interview.formatDate(question.updatedAt);
            const tags = document.getElementById("question-tags");
            tags.replaceChildren();
            (question.tags || []).forEach(function (tag) {
                const chip = document.createElement("span");
                chip.className = "tag";
                chip.textContent = tag;
                tags.appendChild(chip);
            });
            Interview.fillProse(document.getElementById("question-body"), question.question);
            Interview.fillProse(document.getElementById("solution-body"), question.solution);
            document.getElementById("code-language").textContent = question.codeLanguage || "Plain Text";
            Interview.renderCode(document.getElementById("code-block"), question.codeExample, question.codeLanguage);
            const solution = document.getElementById("solution");
            const show = document.getElementById("show-solution");
            const hide = document.getElementById("hide-solution");
            show.addEventListener("click", function () {
                solution.hidden = false;
                show.hidden = true;
                hide.hidden = false;
                solution.scrollIntoView();
            });
            hide.addEventListener("click", function () {
                solution.hidden = true;
                show.hidden = false;
                hide.hidden = true;
            });
            document.getElementById("copy-code").addEventListener("click", async function () {
                await Interview.copyText(question.codeExample || "");
                Interview.toast(question.codeExample ? "Code copied." : "There is no code to copy.");
            });
            document.getElementById("copy-link").addEventListener("click", async function () {
                await Interview.copyText(window.location.href);
                Interview.toast("Link copied.");
            });
            setPager(document.getElementById("prev-link"), position > 0 ? all[position - 1] : null);
            setPager(document.getElementById("next-link"), position >= 0 && position < all.length - 1 ? all[position + 1] : null);
            document.querySelectorAll("[data-count]").forEach(function (node) {
                const name = node.dataset.count;
                if (name === "all") {
                    node.textContent = String(all.length);
                    return;
                }
                node.textContent = String(all.filter(function (item) {
                    return (item.tags || []).some(function (tag) {
                        return tag.toLowerCase() === name.toLowerCase();
                    });
                }).length);
            });
            const activeTopic = (question.tags || [])[0] || "";
            document.querySelectorAll(".sidebar [data-topic]").forEach(function (link) {
                link.classList.toggle("is-active", link.dataset.topic === activeTopic);
            });
        } catch (error) {
            showMissing(error.message);
        }
    });
})();
