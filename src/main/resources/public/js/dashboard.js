// File: src/main/resources/public/js/dashboard.js

import * as api from "./actions.js";
import { state } from "./state.js";
import { hideError, showError, setHealthValue } from "./render.js";

export async function refreshHealth() {
    hideError();

    const btn = document.getElementById("btn-health");
    const originalText = btn?.textContent;

    if (btn) {
        btn.disabled = true;
        btn.textContent = "Checking...";
    }

    setHealthValue("...");

    try {
        await api.health();
        setHealthValue("OK");
    } catch (e) {
        setHealthValue("DOWN");
        showError(e);
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.textContent = originalText;
        }
    }
}

export function initDashboard() {
    const apiBaseEl = document.getElementById("api-base");
    if (apiBaseEl) apiBaseEl.textContent = state.apiBase;

    document.getElementById("btn-health")?.addEventListener("click", refreshHealth);
}
