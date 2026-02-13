// File: src/main/resources/public/js/state.js

export const state = {
    apiBase: window.location.origin, // same-origin (Javalin serves /public)
    ui: {
        activeViewId: "view-landing"
    },
    reservations: {
        date: "",      // YYYY-MM-DD (from <input type="date">)
        status: "",    // "" means (any)
        sort: "time_asc",   // ✅ new
        rows: [],
        lastFetchIso: null
    }
};

// ---- Date helpers ----

export function todayDateInputValue() {
    const d = new Date();
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, "0");
    const dd = String(d.getDate()).padStart(2, "0");
    return `${yyyy}-${mm}-${dd}`;
}

/**
 * Convert local Date to ISO-8601 OffsetDateTime: "YYYY-MM-DDTHH:mm:ss+01:00"
 */
export function toOffsetDateTime(date) {
    const pad = (n) => String(n).padStart(2, "0");

    const yyyy = date.getFullYear();
    const mm = pad(date.getMonth() + 1);
    const dd = pad(date.getDate());
    const hh = pad(date.getHours());
    const mi = pad(date.getMinutes());
    const ss = pad(date.getSeconds());

    // JS getTimezoneOffset: minutes behind UTC (e.g., Barcelona winter = -60)
    const offsetMinutes = -date.getTimezoneOffset();
    const sign = offsetMinutes >= 0 ? "+" : "-";
    const abs = Math.abs(offsetMinutes);
    const offH = pad(Math.floor(abs / 60));
    const offM = pad(abs % 60);

    return `${yyyy}-${mm}-${dd}T${hh}:${mi}:${ss}${sign}${offH}:${offM}`;
}

/**
 * Build [from,to) window for selected day: local 00:00 -> next day 00:00
 */
export function dayWindowOffset(selectedDate /* YYYY-MM-DD */) {
    const [y, m, d] = selectedDate.split("-").map(Number);
    const from = new Date(y, m - 1, d, 0, 0, 0, 0);
    const to = new Date(y, m - 1, d + 1, 0, 0, 0, 0);
    return { from: toOffsetDateTime(from), to: toOffsetDateTime(to) };
}

export function formatDateTimeForCell(iso) {
    if (!iso) return "";
    const dt = new Date(iso);
    if (Number.isNaN(dt.getTime())) return String(iso);

    const yyyy = dt.getFullYear();
    const mm = String(dt.getMonth() + 1).padStart(2, "0");
    const dd = String(dt.getDate()).padStart(2, "0");
    const hh = String(dt.getHours()).padStart(2, "0");
    const mi = String(dt.getMinutes()).padStart(2, "0");
    return `${yyyy}-${mm}-${dd} ${hh}:${mi}`;
}

export function datetimeLocalToOffset(value) {
    // value like "2026-02-21T20:30"
    if (!value) return null;
    const dt = new Date(value);
    if (Number.isNaN(dt.getTime())) return null;
    return toOffsetDateTime(dt);
}
