(function () {
    const filters = { search: "", category: "", difficulty: "" };
    let metadataCache = null;

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

    function showWorkspace() {
        document.getElementById("gate").hidden = true;
        document.getElementById("workspace").hidden = false;
    }

    function showGate() {
        document.getElementById("workspace").hidden = true;
        document.getElementById("gate").hidden = false;
    }

    async function unlock(key) {
        sessionStorage.setItem("interview.adminKey", key);
        await Interview.request("/api/admin/verify", { method: "POST", admin: true });
        showWorkspace();
        await reload();
    }

    function render(questions) {
        const body = document.getElementById("admin-body");
        body.replaceChildren();
        if (!questions.length) {
            const row = document.createElement("tr");
            const cell = document.createElement("td");
            cell.colSpan = 5;
            cell.textContent = "No questions match.";
            row.appendChild(cell);
            body.appendChild(row);
            return;
        }
        questions.forEach(function (question) {
            const row = document.createElement("tr");
            function cell(label, text) {
                const item = document.createElement("td");
                item.dataset.label = label;
                item.textContent = text;
                return item;
            }
            const actions = document.createElement("td");
            actions.dataset.label = "Actions";
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
            actions.append(edit, document.createTextNode(" "), remove);
            row.append(
                cell("Title", question.title),
                cell("Category", question.category),
                cell("Difficulty", question.difficulty),
                cell("Updated", Interview.formatDate(question.updatedAt)),
                actions
            );
            body.appendChild(row);
        });
    }

    async function reload() {
        const params = new URLSearchParams();
        if (filters.category) params.set("category", filters.category);
        if (filters.difficulty) params.set("difficulty", filters.difficulty);
        if (filters.search) params.set("search", filters.search);
        const query = params.toString();
        try {
            const questions = await Interview.request("/api/questions" + (query ? "?" + query : ""));
            banner("");
            render(questions);
        } catch (error) {
            banner(error.message);
        }
    }

    async function fillFilters() {
        metadataCache = metadataCache || await Interview.request("/api/metadata");
        ["admin-category", "admin-difficulty"].forEach(function (id) {
            const select = document.getElementById(id);
            if (select.options.length > 1) {
                return;
            }
            const values = id === "admin-category" ? metadataCache.categories : metadataCache.difficulties;
            values.forEach(function (value) {
                const option = document.createElement("option");
                option.value = value;
                option.textContent = value;
                select.appendChild(option);
            });
        });
    }

    async function editQuestion(id) {
        try {
            const question = await Interview.request("/api/questions/" + encodeURIComponent(id));
            await QuestionEditor.open({ mode: "edit", question: question, onSaved: reload });
        } catch (error) {
            Interview.toast(error.message, "error");
        }
    }

    async function deleteQuestion(question) {
        try {
            const deleted = await QuestionEditor.confirmDelete(question);
            if (deleted) {
                await reload();
            }
        } catch (error) {
            Interview.toast(error.message, "error");
        }
    }

    document.addEventListener("DOMContentLoaded", async function () {
        document.getElementById("gate-form").addEventListener("submit", async function (event) {
            event.preventDefault();
            const error = document.getElementById("gate-error");
            error.hidden = true;
            try {
                await unlock(document.getElementById("gate-key").value);
            } catch (failure) {
                sessionStorage.removeItem("interview.adminKey");
                error.hidden = false;
                error.textContent = failure.status === 401 ? "That admin key was rejected." : failure.message;
            }
        });
        document.getElementById("add-button").addEventListener("click", async function () {
            try {
                await QuestionEditor.open({ mode: "add", onSaved: reload });
            } catch (error) {
                Interview.toast(error.message, "error");
            }
        });
        document.getElementById("lock-button").addEventListener("click", function () {
            sessionStorage.removeItem("interview.adminKey");
            showGate();
        });
        let timer = 0;
        document.getElementById("admin-search").addEventListener("input", function (event) {
            window.clearTimeout(timer);
            timer = window.setTimeout(function () {
                filters.search = event.target.value.trim();
                reload();
            }, 200);
        });
        document.getElementById("admin-category").addEventListener("change", function (event) {
            filters.category = event.target.value;
            reload();
        });
        document.getElementById("admin-difficulty").addEventListener("change", function (event) {
            filters.difficulty = event.target.value;
            reload();
        });
        fillFilters().catch(function (error) {
            banner(error.message);
        });
        const existing = sessionStorage.getItem("interview.adminKey");
        if (!existing) {
            showGate();
            return;
        }
        try {
            await Interview.request("/api/admin/verify", { method: "POST", admin: true });
            await fillFilters();
            showWorkspace();
            await reload();
        } catch (error) {
            sessionStorage.removeItem("interview.adminKey");
            showGate();
        }
    });
})();
