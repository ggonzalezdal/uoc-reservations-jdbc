// File: src/main/resources/public/js/actions.js

async function apiFetch(path, options = {}) {
    const res = await fetch(path, {
        method: options.method || "GET",
        headers: {
            Accept: "application/json",
            ...(options.body ? { "Content-Type": "application/json" } : {}),
            ...(options.headers || {})
        },
        body: options.body,
    });

    const ct = res.headers.get("content-type") || "";
    const isJson = ct.includes("application/json");
    const payload = isJson ? await res.json().catch(() => null) : await res.text().catch(() => null);

    if (!res.ok) {
        const message =
            payload && typeof payload === "object" && payload.message ? payload.message :
                typeof payload === "string" && payload.trim() ? payload :
                    `HTTP ${res.status}`;
        const err = new Error(message);
        err.status = res.status;
        err.payload = payload;
        throw err;
    }

    return payload;
}

export function health() {
    return apiFetch("/health");
}

/**
 * GET /reservations?from=...&to=...&status=...
 * status is optional, from/to optional
 */
export function listReservations({ from, to, status }) {
    const params = new URLSearchParams();
    if (from) params.set("from", from);
    if (to) params.set("to", to);
    if (status) params.set("status", status);

    const qs = params.toString();
    return apiFetch(qs ? `/reservations?${qs}` : "/reservations");
}

export function confirmReservation(reservationId) {
    return apiFetch(`/reservations/${encodeURIComponent(reservationId)}/confirm`, {
        method: "POST"
    });
}

export function cancelReservation(reservationId, reason /* optional */) {
    const r = (reason ?? "").trim();
    const hasReason = r.length > 0;

    return apiFetch(`/reservations/${encodeURIComponent(reservationId)}/cancel`, {
        method: "POST",
        ...(hasReason ? { body: JSON.stringify({ reason: r }) } : {})
    });
}
