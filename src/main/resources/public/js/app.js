// File: src/main/resources/public/js/app.js

import { state, todayDateInputValue, dayWindowOffset } from "./state.js";
import * as api from "./actions.js";
import {
    hideError,
    showError,
    setHealthValue,
    setLoadedCount,
    setLastFetch,
    setActiveView,
    renderReservationsTable,
    setReservationsMeta
} from "./render.js";

function initNav() {
    // Sidebar buttons + header clickable blocks
    document.querySelectorAll("[data-target]").forEach((el) => {
        el.addEventListener("click", () => {
            const target = el.dataset.target;
            if (!target) return;
            hideError();
            state.ui.activeViewId = target;
            setActiveView(target);
        });
    });
}

async function refreshHealth() {
    hideError();
    try {
        await api.health();
        setHealthValue("OK");
    } catch (e) {
        setHealthValue("DOWN");
        showError(e);
    }
}

function initDashboard() {
    const apiBaseEl = document.getElementById("api-base");
    if (apiBaseEl) apiBaseEl.textContent = state.apiBase;

    document.getElementById("btn-health")?.addEventListener("click", refreshHealth);
}

function initReservationsControls() {
    const dateEl = document.getElementById("date");
    const statusEl = document.getElementById("status");
    const btnLoad = document.getElementById("btn-load");
    const btnClear = document.getElementById("btn-clear");

    // defaults
    dateEl.value = todayDateInputValue();

    btnLoad?.addEventListener("click", loadReservations);
    btnClear?.addEventListener("click", () => {
        hideError();
        state.reservations.rows = [];
        setLoadedCount(0);
        setLastFetch(null);
        setReservationsMeta("Cleared");
        renderReservationsTable([]);
    });

    // Event delegation for inline Confirm/Cancel buttons rendered in table
    document.getElementById("reservations-tbody")?.addEventListener("click", async (ev) => {
        const btn = ev.target?.closest?.("button[data-action]");
        if (!btn) return;

        const action = btn.dataset.action;
        const id = btn.dataset.id;
        if (!id) return;

        hideError();

        try {
            if (action === "confirm") {
                await api.confirmReservation(id);
            } else if (action === "cancel") {
                // UI remains thin; backend authoritative. Reason optional.
                const reason = window.prompt("Cancellation reason (optional):", "") ?? "";
                await api.cancelReservation(id, reason);
            }

            // After action: refresh board
            await loadReservations(true);
        } catch (e) {
            showError(e);
        }
    });
}

async function loadReservations(isRefreshAfterAction = false) {
    hideError();

    const dateEl = document.getElementById("date");
    const statusEl = document.getElementById("status");

    const selectedDate = dateEl?.value || todayDateInputValue();
    const status = statusEl?.value || ""; // "" = (any)

    state.reservations.date = selectedDate;
    state.reservations.status = status;

    const { from, to } = dayWindowOffset(selectedDate);

    setReservationsMeta(isRefreshAfterAction ? "Refreshing after action…" : "Loading…");

    try {
        const rows = await api.listReservations({ from, to, status });
        state.reservations.rows = Array.isArray(rows) ? rows : [];

        renderReservationsTable(state.reservations.rows);

        const nowIso = new Date().toISOString();
        state.reservations.lastFetchIso = nowIso;

        setLoadedCount(state.reservations.rows.length);
        setLastFetch(nowIso);

        const statusLabel = status ? `status=${status}` : "status=ANY";
        setReservationsMeta(`Loaded ${state.reservations.rows.length} · ${selectedDate} · ${statusLabel}`);
    } catch (e) {
        setReservationsMeta("Load failed");
        renderReservationsTable([]);
        showError(e);
    }
}

// ---- Boot ----
(function boot() {
    initNav();
    initDashboard();
    initReservationsControls();

    // default view
    setActiveView(state.ui.activeViewId);

    // initial health check
    refreshHealth().catch(() => {});
})();
