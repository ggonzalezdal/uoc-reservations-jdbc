// File: src/main/resources/public/js/reservations.js

import { state, todayDateInputValue, dayWindowOffset } from "./state.js";
import * as api from "./actions.js";
import {
    hideError,
    showError,
    setLoadedCount,
    setLastFetch,
    renderReservationsTable,
    setReservationsMeta
} from "./render.js";
import { loadReservationFilters, saveReservationFilters } from "./storage.js";

function applyReservationFiltersToControls(filters) {
    const dateEl = document.getElementById("date");
    const statusEl = document.getElementById("status");
    const sortEl = document.getElementById("sort");

    if (filters?.date && dateEl) dateEl.value = filters.date;
    if (typeof filters?.status === "string" && statusEl) statusEl.value = filters.status;
    if (filters?.sort && sortEl) sortEl.value = filters.sort;
}

function toEpochMs(iso) {
    if (!iso) return Number.NaN;
    const t = Date.parse(iso);
    return Number.isFinite(t) ? t : Number.NaN;
}

function sortRows(rows, sortKey) {
    const copy = Array.isArray(rows) ? [...rows] : [];
    const key = sortKey ?? "time_asc";

    copy.sort((a, b) => {
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

export async function loadReservations(isRefreshAfterAction = false) {
    hideError();

    const dateEl = document.getElementById("date");
    const statusEl = document.getElementById("status");

    const selectedDate = dateEl?.value || todayDateInputValue();
    const status = statusEl?.value || "";

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

        saveReservationFilters({ date: selectedDate, status, sort: state.reservations.sort });

    } catch (e) {
        setReservationsMeta("Load failed");
        renderReservationsTable([]);
        showError(e);
    }
}

export function initReservationsControls() {
    const dateEl = document.getElementById("date");
    const statusEl = document.getElementById("status");
    const sortEl = document.getElementById("sort");
    const btnLoad = document.getElementById("btn-load");
    const btnClear = document.getElementById("btn-clear");

    const saved = loadReservationFilters();

    // defaults first
    if (sortEl) sortEl.value = state.reservations.sort || "time_asc";
    if (dateEl) dateEl.value = todayDateInputValue();
    if (statusEl) statusEl.value = "";

    // override with saved (if present)
    applyReservationFiltersToControls(saved);

    btnLoad?.addEventListener("click", () => loadReservations(false));

    btnClear?.addEventListener("click", () => {
        hideError();
        state.reservations.rows = [];
        setLoadedCount(0);
        setLastFetch(null);
        setReservationsMeta("Cleared");
        renderReservationsTable([]);
    });

    document.getElementById("btn-today")?.addEventListener("click", async () => {
        hideError();
        if (dateEl) dateEl.value = todayDateInputValue();
        await loadReservations(false);
    });

    sortEl?.addEventListener("change", () => {
        state.reservations.sort = sortEl.value || "time_asc";
        renderReservationsTable(sortRows(state.reservations.rows, state.reservations.sort));
    });

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
