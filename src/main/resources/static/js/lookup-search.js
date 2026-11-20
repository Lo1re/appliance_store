"use strict";

document.querySelectorAll("[data-lookup]").forEach(input => {
    const status = document.querySelector(`[data-lookup-status="${input.dataset.lookup}"]`);
    let timer;
    let pending;
    let revision = 0;
    status.textContent = input.dataset.hint;
    input.addEventListener("input", () => {
        clearTimeout(timer);
        if (pending) pending.abort();
        const current = ++revision;
        timer = setTimeout(async () => {
            pending = new AbortController();
            try {
                const url = new URL(input.dataset.url, window.location.origin);
                url.searchParams.set("query", input.value);
                const response = await fetch(url, {signal: pending.signal, headers: {Accept: "application/json"}});
                if (!response.ok || !response.headers.get("content-type")?.includes("application/json")) {
                    throw new Error("Lookup unavailable");
                }
                const options = await response.json();
                if (revision !== current) return;
                const selector = `select[data-options="${input.dataset.lookup}"]`;
                const selects = [...document.querySelectorAll(selector)];
                document.querySelectorAll("template").forEach(template => selects.push(...template.content.querySelectorAll(selector)));
                selects.forEach(select => {
                    const selected = select.selectedOptions[0];
                    const value = select.value;
                    const placeholder = select.querySelector('option[value=""]')?.cloneNode(true) || new Option("", "");
                    select.replaceChildren(placeholder);
                    options.forEach(option => select.add(new Option(option.label, String(option.id))));
                    if (value && !options.some(option => String(option.id) === value) && selected) {
                        select.add(selected.cloneNode(true));
                    }
                    select.value = value;
                });
                status.textContent = `${options.length}. ${input.dataset.hint}`;
            } catch (error) {
                if (error.name !== "AbortError" && revision === current) status.textContent = input.dataset.failed;
            }
        }, 250);
    });
});
