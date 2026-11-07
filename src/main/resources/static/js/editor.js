(function () {
    const FIELD_IDS = {
        title: "field-title",
        category: "field-category",
        difficulty: "field-difficulty",
        tags: "field-tags",
        question: "field-question",
        solution: "field-solution",
        codeLanguage: "field-language",
        codeExample: "field-code"
    };

    let metadataPromise = null;
    let editingId = null;

    function dialog(id) {
        return document.getElementById(id);
    }

    function metadata() {
        if (!metadataPromise) {
            metadataPromise = Interview.request("/api/metadata").catch(function (error) {
                metadataPromise = null;
                throw error;
            });
        }
        return metadataPromise;
    }

    function fillSelect(select, values, current) {
        select.replaceChildren();
        const options = values.slice();
        if (current && options.indexOf(current) === -1) {
            options.push(current);
        }
        options.forEach(function (value) {
            const option = document.createElement("option");
            option.value = value;
            option.textContent = value;
            select.appendChild(option);
        });
        if (current) {
            select.value = current;
        }
    }

    function clearErrors() {
        const error = document.getElementById("editor-error");
        error.hidden = true;
        error.textContent = "";
        document.querySelectorAll(".field-error").forEach(function (node) {
            node.remove();
        });
    }

    function showErrors(error) {
        const banner = document.getElementById("editor-error");
        banner.hidden = false;
        banner.textContent = error.message || "Could not save the question.";
        if (!error.fields) {
            return;
        }
        Object.keys(error.fields).forEach(function (field) {
            const input = document.getElementById(FIELD_IDS[field] || "");
            if (!input) {
                return;
            }
            const note = document.createElement("p");
            note.className = "field-error";
            note.textContent = error.fields[field];
            input.insertAdjacentElement("afterend", note);
        });
    }

    function updatePreview() {
        const preview = document.getElementById("code-preview");
        if (!preview) {
            return;
        }
        Interview.renderCode(
            preview,
            document.getElementById("field-code").value,
            document.getElementById("field-language").value
        );
    }

    function readForm() {
        return {
            title: document.getElementById("field-title").value,
            category: document.getElementById("field-category").value,
            difficulty: document.getElementById("field-difficulty").value,
            tags: document.getElementById("field-tags").value.split(",").map(function (tag) {
                return tag.trim();
            }).filter(Boolean),
            question: document.getElementById("field-question").value,
            solution: document.getElementById("field-solution").value,
            codeLanguage: document.getElementById("field-language").value,
            codeExample: document.getElementById("field-code").value
        };
    }

    function ask(dialogId) {
        const element = dialog(dialogId);
        return new Promise(function (resolve) {
            let settled = false;
            function finish(value) {
                if (settled) {
                    return;
                }
                settled = true;
                element.close();
                resolve(value);
            }
            element.querySelector("[data-accept]").onclick = function () {
                finish(true);
            };
            element.querySelector("[data-cancel]").onclick = function () {
                finish(false);
            };
            element.addEventListener("cancel", function (event) {
                event.preventDefault();
                finish(false);
            }, { once: true });
            element.showModal();
        });
    }

    async function ensureAdmin() {
        const existing = sessionStorage.getItem("interview.adminKey");
        if (existing) {
            try {
                await Interview.request("/api/admin/verify", { method: "POST", admin: true });
                return;
            } catch (error) {
                if (error.status !== 401) {
                    throw error;
                }
                sessionStorage.removeItem("interview.adminKey");
            }
        }
        const access = dialog("access-dialog");
        const input = document.getElementById("access-key");
        const errorNode = document.getElementById("access-error");
        input.value = "";
        errorNode.hidden = true;
        while (true) {
            const accepted = await ask("access-dialog");
            if (!accepted) {
                const cancelled = new Error("Admin access is required to change questions.");
                cancelled.status = 401;
                throw cancelled;
            }
            sessionStorage.setItem("interview.adminKey", input.value);
            try {
                await Interview.request("/api/admin/verify", { method: "POST", admin: true });
                return;
            } catch (error) {
                sessionStorage.removeItem("interview.adminKey");
                if (error.status !== 401) {
                    throw error;
                }
                errorNode.hidden = false;
                errorNode.textContent = "That admin key was rejected.";
            }
        }
    }

    async function open(options) {
        const question = options.question || null;
        editingId = question ? question.id : null;
        clearErrors();
        const data = await metadata();
        fillSelect(document.getElementById("field-category"), data.categories, question && question.category);
        fillSelect(document.getElementById("field-difficulty"), data.difficulties, question && question.difficulty);
        fillSelect(document.getElementById("field-language"), data.codeLanguages, (question && question.codeLanguage) || "Java");
        document.getElementById("editor-heading").textContent = question ? "Edit question" : "Add question";
        document.getElementById("editor-save").textContent = question ? "Save changes" : "Save question";
        document.getElementById("field-title").value = question ? question.title : "";
        document.getElementById("field-tags").value = question && question.tags ? question.tags.join(", ") : "";
        document.getElementById("field-question").value = question ? question.question : "";
        document.getElementById("field-solution").value = question ? (question.solution || "") : "";
        document.getElementById("field-code").value = question ? (question.codeExample || "") : "";
        updatePreview();
        dialog("editor-dialog").showModal();
        document.getElementById("field-title").focus();
        dialog("editor-dialog").onSaved = options.onSaved;
    }

    async function save(event) {
        event.preventDefault();
        clearErrors();
        const button = document.getElementById("editor-save");
        button.disabled = true;
        const original = button.textContent;
        button.textContent = "Saving…";
        try {
            const body = readForm();
            if (editingId) {
                await Interview.request("/api/questions/" + encodeURIComponent(editingId), {
                    method: "PUT",
                    admin: true,
                    body: body
                });
                Interview.toast("Question updated.");
            } else {
                await Interview.request("/api/questions", { method: "POST", admin: true, body: body });
                Interview.toast("Question saved.");
            }
            const saved = dialog("editor-dialog").onSaved;
            dialog("editor-dialog").close();
            if (saved) {
                await saved();
            }
        } catch (error) {
            showErrors(error);
        } finally {
            button.disabled = false;
            button.textContent = original;
        }
    }

    async function confirmDelete(question) {
        document.getElementById("confirm-title").textContent = "Delete this question?";
        document.getElementById("confirm-message").textContent =
            "“" + question.title + "” will be removed from the JSON library.";
        const accepted = await ask("confirm-dialog");
        if (!accepted) {
            return false;
        }
        await Interview.request("/api/questions/" + encodeURIComponent(question.id), {
            method: "DELETE",
            admin: true
        });
        Interview.toast("Question deleted.");
        return true;
    }

    document.addEventListener("DOMContentLoaded", function () {
        const form = document.getElementById("editor-form");
        if (!form) {
            return;
        }
        form.addEventListener("submit", save);
        document.getElementById("editor-cancel").addEventListener("click", function () {
            dialog("editor-dialog").close();
        });
        document.getElementById("field-code").addEventListener("input", updatePreview);
        document.getElementById("field-language").addEventListener("change", updatePreview);
        document.getElementById("field-code").addEventListener("keydown", function (event) {
            if (event.key !== "Tab") {
                return;
            }
            event.preventDefault();
            const area = event.currentTarget;
            const start = area.selectionStart;
            const end = area.selectionEnd;
            area.value = area.value.slice(0, start) + "    " + area.value.slice(end);
            area.selectionStart = area.selectionEnd = start + 4;
            updatePreview();
        });
    });

    window.QuestionEditor = {
        open: open,
        ensureAdmin: ensureAdmin,
        confirmDelete: confirmDelete
    };
})();
