(function () {
    const LANGUAGE_MAP = {
        Java: "java",
        Python: "python",
        JavaScript: "javascript",
        TypeScript: "typescript",
        SQL: "sql",
        XML: "xml",
        JSON: "json",
        YAML: "yaml",
        Bash: "bash",
        HTML: "xml",
        CSS: "css",
        "Plain Text": "plaintext"
    };

    async function request(path, options) {
        const settings = options || {};
        const headers = new Headers(settings.headers || {});
        if (settings.body !== undefined && !headers.has("Content-Type")) {
            headers.set("Content-Type", "application/json");
        }
        if (settings.admin) {
            const key = sessionStorage.getItem("interview.adminKey") || "";
            if (key) {
                headers.set("X-Admin-Key", key);
            }
        }
        let response;
        try {
            response = await fetch(path, {
                method: settings.method || "GET",
                headers: headers,
                body: settings.body === undefined ? undefined : JSON.stringify(settings.body)
            });
        } catch (error) {
            const failure = new Error("The server could not be reached.");
            failure.cause = error;
            throw failure;
        }
        if (response.status === 204) {
            return null;
        }
        const text = await response.text();
        let payload = null;
        if (text) {
            try {
                payload = JSON.parse(text);
            } catch (error) {
                const failure = new Error("The server returned a response that was not JSON.");
                failure.status = response.status;
                throw failure;
            }
        }
        if (!response.ok) {
            const failure = new Error((payload && payload.message) || response.statusText || "Request failed.");
            failure.status = response.status;
            failure.fields = payload && payload.fields ? payload.fields : null;
            throw failure;
        }
        return payload;
    }

    function toast(message, kind) {
        let element = document.getElementById("toast");
        if (!element) {
            element = document.createElement("div");
            element.id = "toast";
            element.className = "toast";
            element.setAttribute("role", "status");
            document.body.appendChild(element);
        }
        element.textContent = message;
        element.dataset.kind = kind || "ok";
        element.classList.add("show");
        window.clearTimeout(element.hideTimer);
        element.hideTimer = window.setTimeout(function () {
            element.classList.remove("show");
        }, 3200);
    }

    function formatDate(value) {
        if (!value) {
            return "Unknown";
        }
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return "Unknown";
        }
        return date.toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
    }

    function isRecent(value) {
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return false;
        }
        return Date.now() - date.getTime() <= 14 * 24 * 60 * 60 * 1000;
    }

    function excerpt(value) {
        const text = (value || "").replace(/\s+/g, " ").trim();
        if (text.length <= 160) {
            return text;
        }
        return text.slice(0, 157).replace(/\s+\S*$/, "") + "…";
    }

    function difficultyClass(value) {
        if (value === "Beginner") return "beginner";
        if (value === "Intermediate") return "intermediate";
        if (value === "Advanced") return "advanced";
        if (value === "Principal Architect") return "principal";
        return "";
    }

    function fillProse(container, text) {
        container.replaceChildren();
        const blocks = String(text || "").split(/\n\s*\n/).map(function (block) {
            return block.trim();
        }).filter(Boolean);
        if (!blocks.length) {
            const paragraph = document.createElement("p");
            paragraph.className = "muted";
            paragraph.textContent = "Nothing written yet.";
            container.appendChild(paragraph);
            return;
        }
        blocks.forEach(function (block) {
            const paragraph = document.createElement("p");
            paragraph.textContent = block;
            container.appendChild(paragraph);
        });
    }

    function renderCode(pre, source, languageLabel) {
        pre.replaceChildren();
        const code = document.createElement("code");
        const language = LANGUAGE_MAP[languageLabel] || "plaintext";
        if (!source) {
            code.className = "is-empty";
            code.textContent = "No code example.";
            pre.appendChild(code);
            return;
        }
        if (window.hljs && window.hljs.getLanguage && window.hljs.getLanguage(language)) {
            code.className = "hljs language-" + language;
            code.innerHTML = window.hljs.highlight(source, { language: language, ignoreIllegals: true }).value;
        } else {
            code.textContent = source;
        }
        pre.appendChild(code);
    }

    async function copyText(text) {
        try {
            await navigator.clipboard.writeText(text);
        } catch (error) {
            const area = document.createElement("textarea");
            area.value = text;
            area.setAttribute("readonly", "");
            document.body.appendChild(area);
            area.select();
            document.execCommand("copy");
            area.remove();
        }
    }

    window.Interview = {
        request: request,
        toast: toast,
        formatDate: formatDate,
        isRecent: isRecent,
        excerpt: excerpt,
        difficultyClass: difficultyClass,
        fillProse: fillProse,
        renderCode: renderCode,
        copyText: copyText
    };
})();
