// File: src/main/resources/public/js/app.js

import { state, todayDateInputValue, dayWindowOffset, datetimeLocalToOffset } from "./state.js";
import * as api from "./actions.js";
import {
    hideError,
    showError,
    setHealthValue,
    setLoadedCount,
    setLastFetch,
    setActiveView,
    renderReservationsTable,
    setReservationsMeta,
    renderCustomersSelect,
    renderAvailableTablesCheckboxes,
    setAvailableMeta
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
        const btn = ev.target.closest("button[data-action][data-id]");
        if (!btn) return;

        if (btn.disabled) return;

        const action = btn.dataset.action;
        const id = Number(btn.dataset.id);

        if (!Number.isFinite(id) || id <= 0) return;

        hideError();

        try {
            if (action === "confirm") {
                await api.confirmReservation(id);
            } else if (action === "cancel") {
                const reason = window.prompt("Cancellation reason (optional):", "") ?? "";
                await api.cancelReservation(id, reason);
            }

            await loadReservations(true);
        } catch (e) {
            showError(e);
        }
    });

}

async function loadCustomersIntoSelect() {
    hideError();
    try {
        const customers = await api.listCustomers();
        renderCustomersSelect(customers);
    } catch (e) {
        showError(e);
    }
}

function getCreateMode() {
    const checked = document.querySelector('input[name="cr-mode"]:checked');
    return checked ? checked.value : "auto";
}

function setManualBoxVisible(visible) {
    const box = document.getElementById("cr-manual-box");
    if (!box) return;
    box.style.display = visible ? "block" : "none";
}

async function checkAvailabilityForCreate() {
    hideError();

    const startVal = document.getElementById("cr-start")?.value || "";
    const endVal = document.getElementById("cr-end")?.value || "";

    const start = datetimeLocalToOffset(startVal);
    const end = datetimeLocalToOffset(endVal);

    if (!start) {
        showError(new Error("Start datetime is required to check availability."));
        return;
    }

    setAvailableMeta("Loading…");
    try {
        const tables = await api.listAvailableTables(start, end);
        renderAvailableTablesCheckboxes(tables);
        setAvailableMeta(`Available: ${Array.isArray(tables) ? tables.length : 0}`);
    } catch (e) {
        setAvailableMeta("Failed");
        showError(e);
    }
}

function readSelectedTableIds() {
    return Array.from(document.querySelectorAll(".cr-table:checked"))
        .map(cb => Number(cb.value))
        .filter(n => Number.isFinite(n) && n > 0);
}

async function submitCreateReservation() {
    hideError();

    const customerId = Number(document.getElementById("cr-customer")?.value);
    const partySize = Number(document.getElementById("cr-party")?.value);
    const notes = (document.getElementById("cr-notes")?.value ?? "").trim();

    const start = datetimeLocalToOffset(document.getElementById("cr-start")?.value || "");
    const end = datetimeLocalToOffset(document.getElementById("cr-end")?.value || "");

    if (!Number.isFinite(customerId) || customerId <= 0) {
        showError(new Error("Customer is required."));
        return;
    }
    if (!start) {
        showError(new Error("Start datetime is required."));
        return;
    }
    if (!Number.isFinite(partySize) || partySize <= 0) {
        showError(new Error("Party size must be > 0."));
        return;
    }

    const mode = getCreateMode();

    const payload = {
        customerId,
        startAt: start,
        partySize,
        notes: notes || null
    };

    if (end) payload.endAt = end;

    if (mode === "manual") {
        const tableIds = readSelectedTableIds();
        if (tableIds.length === 0) {
            showError(new Error("Manual mode requires selecting at least one table."));
            return;
        }
        payload.tableIds = tableIds;
    }
    // auto mode: omit tableIds (backend auto-assign)

    try {
        await api.createReservation(payload);

        // After success: jump to Reservations board & refresh that day
        const dateEl = document.getElementById("date");
        if (dateEl) dateEl.value = start.slice(0, 10); // YYYY-MM-DD
        state.ui.activeViewId = "view-reservations";
        setActiveView("view-reservations");
        await loadReservations(true);
    } catch (e) {
        showError(e);
    }
}

function initCreateControls() {
    // defaults
    const startEl = document.getElementById("cr-start");
    if (startEl && !startEl.value) {
        // default start today at 20:00 local
        const today = todayDateInputValue();
        startEl.value = `${today}T20:00`;
    }

    // load customers button
    document.getElementById("cr-load-customers")?.addEventListener("click", loadCustomersIntoSelect);

    // mode toggle
    document.querySelectorAll('input[name="cr-mode"]').forEach(r => {
        r.addEventListener("change", () => {
            const mode = getCreateMode();
            setManualBoxVisible(mode === "manual");
        });
    });

    // availability button
    document.getElementById("cr-check-availability")?.addEventListener("click", checkAvailabilityForCreate);

    // submit
    document.getElementById("cr-submit")?.addEventListener("click", submitCreateReservation);

    // optional: load customers once at startup
    loadCustomersIntoSelect().catch(() => {});
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
    initCreateControls();

    // default view
    setActiveView(state.ui.activeViewId);

    // initial health check
    refreshHealth().catch(() => {});
})();
