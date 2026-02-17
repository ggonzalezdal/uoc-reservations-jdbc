// File: src/main/resources/public/js/storage.js

const LS_FILTERS_KEY = "war.ui.reservations.filters";
const LS_ACTIVE_VIEW_KEY = "war.ui.activeViewId";

export function saveActiveViewId(viewId) {
    try {
        localStorage.setItem(LS_ACTIVE_VIEW_KEY, String(viewId || ""));
    } catch (_) {}
}

export function loadActiveViewId() {
    try {
        const v = localStorage.getItem(LS_ACTIVE_VIEW_KEY);
        return (typeof v === "string" && v.trim()) ? v : null;
    } catch (_) {
        return null;
    }
}

export function saveReservationFilters({ date, status, sort }) {
    try {
        localStorage.setItem(LS_FILTERS_KEY, JSON.stringify({ date, status, sort }));
    } catch (_) {
        // ignore (private mode, quota, etc.)
    }
}

export function loadReservationFilters() {
    try {
        const raw = localStorage.getItem(LS_FILTERS_KEY);
        if (!raw) return null;

        const obj = JSON.parse(raw);
        if (!obj || typeof obj !== "object" || Array.isArray(obj)) return null;

        return {
            date: typeof obj.date === "string" ? obj.date : null,
            status: typeof obj.status === "string" ? obj.status : "",
            sort: typeof obj.sort === "string" ? obj.sort : null
        };
    } catch (_) {
        return null;
    }
}
