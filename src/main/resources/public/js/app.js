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

const LS_FILTERS_KEY = "war.ui.reservations.filters";

function saveReservationFilters({ date, status, sort }) {
    try {
        localStorage.setItem(LS_FILTERS_KEY, JSON.stringify({ date, status, sort }));
    } catch (_) {
        // ignore (private mode, quota, etc.)
    }
}

function loadReservationFilters() {
    try {
        const raw = localStorage.getItem(LS_FILTERS_KEY);
        if (!raw) return null;
        const obj = JSON.parse(raw);
        if (!obj || typeof obj !== "object") return null;
        return {
            date: typeof obj.date === "string" ? obj.date : null,
            status: typeof obj.status === "string" ? obj.status : "",
            sort: typeof obj.sort === "string" ? obj.sort : null
        };
    } catch (_) {
        return null;
    }
}

function applyReservationFiltersToControls(filters) {
    const dateEl = document.getElementById("date");
    const statusEl = document.getElementById("status");
    const sortEl = document.getElementById("sort");

    if (filters?.date && dateEl) dateEl.value = filters.date;
    if (typeof filters?.status === "string" && statusEl) statusEl.value = filters.status;
    if (filters?.sort && sortEl) sortEl.value = filters.sort;
}

function initNav() {
    // Sidebar buttons + header clickable blocks
    document.querySelectorAll("[data-target]").forEach((el) => {
        el.addEventListener("click", async () => {
            const target = el.dataset.target;
            if (!target) return;

            hideError();
            state.ui.activeViewId = target;
            setActiveView(target);

            // Auto-load reservations when entering the Reservations view
            if (target === "view-reservations") {
                await loadReservations(false);
            }
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
    const sortEl = document.getElementById("sort");
    const btnLoad = document.getElementById("btn-load");
    const btnClear = document.getElementById("btn-clear");

    // defaults (restore last-used filters if available)
    const saved = loadReservationFilters();

    if (sortEl) sortEl.value = saved?.sort || state.reservations.sort || "time_asc";
    if (dateEl) dateEl.value = saved?.date || todayDateInputValue();
    if (statusEl) statusEl.value = (typeof saved?.status === "string") ? saved.status : "";

    // Load / Clear
    btnLoad?.addEventListener("click", loadReservations);

    btnClear?.addEventListener("click", () => {
        hideError();
        state.reservations.rows = [];
        setLoadedCount(0);
        setLastFetch(null);
        setReservationsMeta("Cleared");
        renderReservationsTable([]);
    });

    // Today button
    document.getElementById("btn-today")?.addEventListener("click", async () => {
        hideError();
        if (dateEl) dateEl.value = todayDateInputValue();
        await loadReservations(false);
    });

    // Sort change (UI-only, no refetch)
    sortEl?.addEventListener("change", () => {
        state.reservations.sort = sortEl.value || "time_asc";
        renderReservationsTable(sortRows(state.reservations.rows, state.reservations.sort));
    });

    // Event delegation for inline Confirm/Cancel buttons
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

    const sortEl = document.getElementById("sort");
    state.reservations.sort = sortEl?.value || state.reservations.sort || "time_asc";

    const { from, to } = dayWindowOffset(selectedDate);

    setReservationsMeta(isRefreshAfterAction ? "Refreshing after action…" : "Loading…");

    try {
        const rows = await api.listReservations({ from, to, status });
        state.reservations.rows = Array.isArray(rows) ? rows : [];

        const sorted = sortRows(state.reservations.rows, state.reservations.sort);
        renderReservationsTable(sorted);

        const nowIso = new Date().toISOString();
        state.reservations.lastFetchIso = nowIso;

        setLoadedCount(state.reservations.rows.length);
        setLastFetch(nowIso);

        const statusLabel = status ? `status=${status}` : "status=ANY";
        setReservationsMeta(`Loaded ${state.reservations.rows.length} · ${selectedDate} · ${statusLabel}`);

        saveReservationFilters({
            date: selectedDate,
            status: status,
            sort: state.reservations.sort
        });

    } catch (e) {
        setReservationsMeta("Load failed");
        renderReservationsTable([]);
        showError(e);
    }
}

function toEpochMs(iso) {
    if (!iso) return Number.NaN;
    const t = Date.parse(iso);
    return Number.isFinite(t) ? t : Number.NaN;
}

function sortRows(rows, sortKey) {
    const copy = Array.isArray(rows) ? [...rows] : [];

    const key = sortKey || "time_asc";

    copy.sort((a, b) => {
        // These are raw API rows; renderReservationsTable will normalize, but sorting needs fields too.
        const aStart = toEpochMs(a.startAt ?? a.start_at);
        const bStart = toEpochMs(b.startAt ?? b.start_at);

        const aPax = Number(a.partySize ?? a.party_size ?? a.pax ?? 0);
        const bPax = Number(b.partySize ?? b.party_size ?? b.pax ?? 0);

        switch (key) {
            case "time_desc":
                return (bStart - aStart) || 0;
            case "pax_asc":
                return (aPax - bPax) || 0;
            case "pax_desc":
                return (bPax - aPax) || 0;
            case "time_asc":
            default:
                return (aStart - bStart) || 0;
        }
    });

    return copy;
}

// ---- Boot ----
(function boot() {
    initNav();
    initDashboard();
    initReservationsControls();
    initCreateControls();

    // default view
    setActiveView(state.ui.activeViewId);

    if (state.ui.activeViewId === "view-reservations") {
        loadReservations(false).catch(() => {});
    }

    // initial health check
    refreshHealth().catch(() => {});
})();
