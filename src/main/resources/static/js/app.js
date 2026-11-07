(function () {
    const filters = { category: "", difficulty: "", search: "" };

    function banner(message) {
        const element = document.getElementById("banner");
        if (!message) {
            element.hidden = true;
            element.textContent = "";
            return;
        }
        element.hidden = false;
        element.textContent = message;
    }

    function setSidebar(topic) {
        document.querySelectorAll("[data-topic]").forEach(function (button) {
            button.classList.toggle("is-active", button.dataset.topic === topic);
        });
    }

    function renderQuestions(questions) {
        const list = document.getElementById("question-list");
        list.replaceChildren();
        if (!questions.length) {
            const empty = document.createElement("div");
            empty.className = "empty";
            empty.textContent = filters.category || filters.difficulty || filters.search
                ? "No questions match this search and these filters."
                : "The library is empty. Add the first question to data/questions.json or use Add question.";
            list.appendChild(empty);
            return;
        }
        questions.forEach(function (question) {
            const card = document.createElement("article");
            card.className = "card";

            const meta = document.createElement("div");
            meta.className = "card-meta";
            const category = document.createElement("span");
            category.className = "chip";
            category.textContent = question.category || "Uncategorized";
            const difficulty = document.createElement("span");
            difficulty.className = "chip " + Interview.difficultyClass(question.difficulty);
            difficulty.textContent = question.difficulty || "Unspecified";
            meta.append(category, difficulty);

            const title = document.createElement("h2");
            title.textContent = question.title || "Untitled";

            const description = document.createElement("p");
            description.textContent = Interview.excerpt(question.question);

            const tags = document.createElement("div");
            tags.className = "chip-row";
            (question.tags || []).forEach(function (tag) {
                const chip = document.createElement("span");
                chip.className = "tag";
                chip.textContent = tag;
                tags.appendChild(chip);
            });

            const actions = document.createElement("div");
            actions.className = "card-actions";
            const view = document.createElement("a");
            view.className = "button-primary";
            view.href = "/questions/" + encodeURIComponent(question.id);
            view.textContent = "View question";
            const edit = document.createElement("button");
            edit.type = "button";
            edit.className = "button";
            edit.textContent = "Edit";
            edit.addEventListener("click", function () {
                editQuestion(question.id);
            });
            const remove = document.createElement("button");
            remove.type = "button";
            remove.className = "button-danger";
            remove.textContent = "Delete";
            remove.addEventListener("click", function () {
                deleteQuestion(question);
            });
            actions.append(view, edit, remove);
            card.append(meta, title, description, tags, actions);
            list.appendChild(card);
        });
    }

    function renderStats(questions, categories) {
        document.getElementById("stat-total").textContent = String(questions.length);
        const topics = new Set();
        questions.forEach(function (question) {
            (question.tags || []).forEach(function (tag) {
                topics.add(tag.toLowerCase());
            });
        });
        document.getElementById("stat-categories").textContent = String(topics.size);
        const advanced = questions.filter(function (question) { return question.difficulty === "Advanced"; }).length;
        document.getElementById("stat-advanced").textContent = String(advanced);
        const recent = questions.filter(function (question) { return Interview.isRecent(question.createdAt); });
        document.getElementById("stat-recent").textContent = String(recent.length);

        const list = document.getElementById("recent-list");
        list.replaceChildren();
        const ordered = questions.slice().sort(function (left, right) {
            return new Date(right.createdAt || 0) - new Date(left.createdAt || 0);
        }).slice(0, 3);
        ordered.forEach(function (question) {
            const item = document.createElement("li");
            const link = document.createElement("a");
            link.href = "/questions/" + encodeURIComponent(question.id);
            link.textContent = question.title;
            item.append(link, document.createTextNode(" · " + Interview.formatDate(question.createdAt)));
            list.appendChild(item);
        });

        document.querySelectorAll("[data-count]").forEach(function (node) {
            const name = node.dataset.count;
            if (name === "all") {
                node.textContent = String(questions.length);
                return;
            }
            const count = questions.filter(function (question) {
                return (question.tags || []).some(function (tag) {
                    return tag.toLowerCase() === name.toLowerCase();
                });
            }).length;
            node.textContent = String(count);
        });
    }

    function fillCategoryFilter(categories) {
        const select = document.getElementById("category-filter");
        const current = filters.category;
        select.replaceChildren();
        const all = document.createElement("option");
        all.value = "";
        all.textContent = "All categories";
        select.appendChild(all);
        categories.forEach(function (category) {
            const option = document.createElement("option");
            option.value = category.name;
            option.textContent = category.name + " (" + category.count + ")";
            select.appendChild(option);
        });
        select.value = current;
    }

    async function reload() {
        const list = document.getElementById("question-list");
        list.replaceChildren();
        const loading = document.createElement("div");
        loading.className = "loading-state";
        loading.textContent = "Opening the library…";
        list.appendChild(loading);
        const params = new URLSearchParams();
        if (filters.category) params.set("category", filters.category);
        if (filters.difficulty) params.set("difficulty", filters.difficulty);
        if (filters.search) params.set("search", filters.search);
        const query = params.toString();
        try {
            const questions = await Interview.request("/api/questions" + (query ? "?" + query : ""));
            const allQuestions = query
                ? await Interview.request("/api/questions")
                : questions;
            const categories = await Interview.request("/api/categories");
            banner("");
            document.getElementById("result-count").textContent = questions.length + (questions.length === 1 ? " question" : " questions");
            fillCategoryFilter(categories);
            renderStats(allQuestions, categories);
            renderQuestions(questions);
        } catch (error) {
            list.replaceChildren();
            banner(error.message);
        }
    }

    async function editQuestion(id) {
        try {
            await QuestionEditor.ensureAdmin();
            const question = await Interview.request("/api/questions/" + encodeURIComponent(id));
            await QuestionEditor.open({ mode: "edit", question: question, onSaved: reload });
        } catch (error) {
            Interview.toast(error.message, "error");
        }
    }

    async function deleteQuestion(question) {
        try {
            await QuestionEditor.ensureAdmin();
            const deleted = await QuestionEditor.confirmDelete(question);
            if (deleted) {
                await reload();
            }
        } catch (error) {
            Interview.toast(error.message, "error");
        }
    }

    document.addEventListener("DOMContentLoaded", function () {
        const search = document.getElementById("search-input");
        const topic = new URLSearchParams(window.location.search).get("topic");
        if (topic) {
            filters.search = topic;
            filters.category = "Spring AI";
            search.value = topic;
            setSidebar(topic);
        }
        let timer = 0;
        document.getElementById("search-form").addEventListener("submit", function (event) {
            event.preventDefault();
        });
        search.addEventListener("input", function () {
            window.clearTimeout(timer);
            timer = window.setTimeout(function () {
                filters.search = search.value.trim();
                setSidebar(filters.search);
                reload();
            }, 200);
        });
        document.getElementById("category-filter").addEventListener("change", function (event) {
            filters.category = event.target.value;
            reload();
        });
        document.getElementById("difficulty-filter").addEventListener("change", function (event) {
            filters.difficulty = event.target.value;
            reload();
        });
        document.getElementById("clear-filters").addEventListener("click", function () {
            filters.category = "";
            filters.difficulty = "";
            filters.search = "";
            search.value = "";
            document.getElementById("difficulty-filter").value = "";
            setSidebar("");
            reload();
        });
        document.getElementById("nav-dashboard").addEventListener("click", function () {
            filters.category = "";
            filters.search = "";
            search.value = "";
            setSidebar("");
            document.getElementById("category-filter").value = "";
            document.getElementById("overview").scrollIntoView();
            reload();
        });
        document.querySelectorAll(".sidebar [data-topic]").forEach(function (button) {
            button.addEventListener("click", function () {
                filters.search = button.dataset.topic;
                filters.category = "Spring AI";
                search.value = filters.search;
                document.getElementById("category-filter").value = "Spring AI";
                setSidebar(filters.search);
                document.getElementById("question-list").scrollIntoView();
                closeSidebar();
                reload();
            });
        });
        document.getElementById("add-button").addEventListener("click", async function () {
            try {
                await QuestionEditor.ensureAdmin();
                await QuestionEditor.open({ mode: "add", onSaved: reload });
            } catch (error) {
                Interview.toast(error.message, "error");
            }
        });
        const menu = document.getElementById("menu-button");
        const sidebar = document.getElementById("sidebar");
        const scrim = document.getElementById("scrim");
        function closeSidebar() {
            sidebar.classList.remove("is-open");
            scrim.hidden = true;
        }
        menu.addEventListener("click", function () {
            sidebar.classList.add("is-open");
            scrim.hidden = false;
        });
        scrim.addEventListener("click", closeSidebar);
        reload();
    });
})();
