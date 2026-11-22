"use strict";

// A single-value search picker; the existing select still submits the same ID.
document.querySelectorAll("[data-lookup]").forEach(input => {
    const key = input.dataset.lookup;
    const select = document.querySelector(`select[data-options="${key}"]`);
    const status = document.querySelector(`[data-lookup-status="${key}"]`);
    const wrapper = document.createElement("div");
    wrapper.className = "lookup-picker";
    input.before(wrapper);
    wrapper.append(input);
    const results = document.createElement("div");
    results.id = `${input.id}-suggestions`;
    results.className = "product-suggestions";
    results.hidden = true;
    wrapper.append(results);
    select.hidden = true;
    select.required = false;
    input.required = true;
    input.autocomplete = "off";
    input.setAttribute("aria-controls", results.id);
    input.setAttribute("aria-expanded", "false");
    status.textContent = "";
    if (select.value) input.value = select.selectedOptions[0].textContent.trim();
    let timer;
    let pending;
    let revision = 0;

    function close() {
        results.hidden = true;
        input.setAttribute("aria-expanded", "false");
    }
    function choose(option) {
        if (![...select.options].some(item => item.value === String(option.id))) {
            select.add(new Option(option.label, String(option.id)));
        }
        select.value = String(option.id);
        input.value = option.label;
        input.setCustomValidity("");
        status.textContent = "";
        close();
        input.focus();
    }
    async function lookup() {
        if (pending) pending.abort();
        const current = ++revision;
        pending = new AbortController();
        try {
            const url = new URL(input.dataset.url, location.origin);
            url.searchParams.set("query", input.value.trim());
            const response = await fetch(url, {signal: pending.signal, headers: {Accept: "application/json"}});
            if (!response.ok || !response.headers.get("content-type")?.includes("application/json")) throw new Error();
            const options = await response.json();
            if (current !== revision || !wrapper.contains(document.activeElement)) return;
            results.replaceChildren();
            options.forEach(option => {
                const button = document.createElement("button");
                button.type = "button";
                button.textContent = option.label;
                button.addEventListener("click", () => choose(option));
                results.append(button);
            });
            results.hidden = options.length === 0;
            input.setAttribute("aria-expanded", String(options.length > 0));
            status.textContent = options.length === 0 ? input.dataset.empty : options.length === 20 ? input.dataset.hint : "";
        } catch (error) {
            if (error.name !== "AbortError" && current === revision) status.textContent = input.dataset.failed;
        }
    }
    input.addEventListener("input", () => {
        clearTimeout(timer);
        ++revision;
        if (pending) pending.abort();
        select.value = "";
        input.setCustomValidity(input.dataset.required);
        status.textContent = "";
        close();
        timer = setTimeout(lookup, 250);
    });
    input.addEventListener("focus", () => { if (!select.value) lookup(); });
    input.addEventListener("keydown", event => {
        if (event.key === "Enter") {
            event.preventDefault();
            if (!results.hidden && results.firstElementChild) results.firstElementChild.click();
            else { clearTimeout(timer); lookup(); }
        }
        if (event.key === "ArrowDown" && !results.hidden) { event.preventDefault(); results.firstElementChild?.focus(); }
        if (event.key === "Escape") close();
    });
    results.addEventListener("keydown", event => {
        if (event.key === "Escape") { close(); input.focus(); }
        if (event.key === "ArrowDown" || event.key === "ArrowUp") {
            event.preventDefault();
            (event.key === "ArrowDown" ? event.target.nextElementSibling : event.target.previousElementSibling)?.focus();
        }
    });
    wrapper.addEventListener("focusout", event => { if (!wrapper.contains(event.relatedTarget)) close(); });
    document.addEventListener("click", event => { if (!wrapper.contains(event.target)) close(); });
});
