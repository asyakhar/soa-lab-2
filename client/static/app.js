const ticketBase = "/api/ticket";
const bookingBase = "/api/booking";

const tableBody = document.getElementById("ticketTableBody");
const messageBox = document.getElementById("message");
const clownScare = document.getElementById("clownScare");
const balloonTriggers = document.querySelectorAll(".balloon-trigger");
const tigerShow = document.getElementById("tigerShow");
let clownTimer = null;
let tigerTimer = null;
let lastBalloonTrigger = null;

function hideClowns() {
    clownScare.classList.remove("active");
    clownScare.setAttribute("aria-hidden", "true");
    document.body.classList.remove("scare-open");
    clearTimeout(clownTimer);

    if (lastBalloonTrigger) {
        lastBalloonTrigger.focus();
    }
}

function showClowns(trigger) {
    clearTimeout(clownTimer);
    lastBalloonTrigger = trigger;

    clownScare.classList.remove("active");
    void clownScare.offsetWidth;
    clownScare.classList.add("active");
    clownScare.setAttribute("aria-hidden", "false");
    document.body.classList.add("scare-open");

    clownTimer = setTimeout(hideClowns, 1800);
}

function showTiger() {
    clearTimeout(tigerTimer);
    tigerShow.classList.remove("active");
    void tigerShow.offsetWidth;
    tigerShow.classList.add("active");
    tigerShow.setAttribute("aria-hidden", "false");

    tigerTimer = setTimeout(() => {
        tigerShow.classList.remove("active");
        tigerShow.setAttribute("aria-hidden", "true");
    }, 2400);
}

balloonTriggers.forEach(trigger => {
    trigger.addEventListener("click", () => showClowns(trigger));
});

clownScare.addEventListener("click", event => {
    if (event.target === clownScare) {
        hideClowns();
    }
});

document.addEventListener("keydown", event => {
    if (event.key === "Escape" && clownScare.classList.contains("active")) {
        hideClowns();
    }
});

function lines(value) {
    return value.split("\n").map(item => item.trim()).filter(Boolean);
}

function escapeHtml(value) {
    return String(value ?? "—")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function showMessage(text, type = "success") {
    messageBox.className = `message ${type}`;
    messageBox.textContent = text;
    window.scrollTo({top: 0, behavior: "smooth"});
}

function formatError(data, status) {
    if (!data || typeof data !== "object") {
        return `Ошибка HTTP ${status}.`;
    }

    let text = data.message || data.error || `Ошибка HTTP ${status}.`;
    if (Array.isArray(data.violations) && data.violations.length > 0) {
        const violations = data.violations
            .map(item => `${item.field}: ${item.message}`)
            .join("; ");
        text += ` (${violations})`;
    }
    return text;
}

async function callApi(url, options = {}) {
    const response = await fetch(url, options);
    const text = await response.text();
    let data = null;

    if (text) {
        try {
            data = JSON.parse(text);
        } catch (_) {
            data = text;
        }
    }

    if (!response.ok && response.status !== 304) {
        throw new Error(formatError(data, response.status));
    }

    return {response, data};
}

function eventText(event) {
    if (!event) return "—";
    const type = event.eventType ?? "без типа";
    return `${event.name}, билетов: ${event.ticketsCount}, ${type}`;
}

function renderTickets(tickets) {
    const list = Array.isArray(tickets) ? tickets : tickets ? [tickets] : [];

    if (list.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="8" class="empty">Билеты не найдены</td></tr>';
        return;
    }

    tableBody.innerHTML = list.map(ticket => `
        <tr>
            <td>${escapeHtml(ticket.id)}</td>
            <td>${escapeHtml(ticket.name)}</td>
            <td>${escapeHtml(ticket.coordinates?.x)}, ${escapeHtml(ticket.coordinates?.y)}</td>
            <td>${escapeHtml(ticket.creationDate)}</td>
            <td>${escapeHtml(ticket.price)}</td>
            <td>${escapeHtml(ticket.comment)}</td>
            <td><span class="badge">${escapeHtml(ticket.type)}</span></td>
            <td>${escapeHtml(eventText(ticket.event))}</td>
        </tr>
    `).join("");
}

function addRepeatedParams(params, name, values) {
    values.forEach(value => params.append(name, value));
}

async function loadTickets() {
    const params = new URLSearchParams();
    const page = document.getElementById("page").value;
    const size = document.getElementById("size").value;

    params.set("page", page);
    params.set("size", size);
    addRepeatedParams(params, "sort", lines(document.getElementById("sort").value));
    addRepeatedParams(params, "filter", lines(document.getElementById("filter").value));

    try {
        const {data} = await callApi(`${ticketBase}/tickets?${params}`);
        renderTickets(data);
        document.getElementById("pageLabel").textContent = `Страница ${page}`;
        if (messageBox.classList.contains("error")) {
            messageBox.className = "message hidden";
        }
    } catch (error) {
        showMessage(error.message, "error");
    }
}

function parseJson(elementId) {
    try {
        return JSON.parse(document.getElementById(elementId).value);
    } catch (_) {
        throw new Error("Введён некорректный JSON.");
    }
}

function ticketUrl(method, id, ticket) {
    const params = new URLSearchParams();
    params.set("ticket", JSON.stringify(ticket));
    const suffix = id ? `/${id}` : "";
    return `${ticketBase}/tickets${suffix}?${params}`;
}

document.getElementById("reloadButton").addEventListener("click", loadTickets);
document.getElementById("applyFiltersButton").addEventListener("click", loadTickets);

document.getElementById("previousPageButton").addEventListener("click", () => {
    const input = document.getElementById("page");
    input.value = Math.max(0, Number(input.value) - 1);
    loadTickets();
});

document.getElementById("nextPageButton").addEventListener("click", () => {
    const input = document.getElementById("page");
    input.value = Number(input.value) + 1;
    loadTickets();
});

document.getElementById("createForm").addEventListener("submit", async event => {
    event.preventDefault();
    try {
        const ticket = parseJson("createTicket");
        const {data} = await callApi(ticketUrl("POST", null, ticket), {method: "POST"});
        showMessage(`Билет создан, id: ${data.id}.`);
        await loadTickets();
    } catch (error) {
        showMessage(error.message, "error");
    }
});

document.getElementById("getForm").addEventListener("submit", async event => {
    event.preventDefault();
    const id = document.getElementById("getId").value;
    const etag = document.getElementById("getEtag").value.trim();
    const headers = etag ? {"If-None-Match": etag} : {};

    try {
        const {response, data} = await callApi(`${ticketBase}/tickets/${id}`, {headers});
        if (response.status === 304) {
            showMessage("Билет не изменился: сервер вернул 304.");
            return;
        }
        renderTickets(data);
        showMessage(`Билет получен. ETag: ${response.headers.get("ETag") || "не указан"}.`);
    } catch (error) {
        showMessage(error.message, "error");
    }
});

document.getElementById("deleteForm").addEventListener("submit", async event => {
    event.preventDefault();
    const id = document.getElementById("deleteId").value;
    try {
        await callApi(`${ticketBase}/tickets/${id}`, {method: "DELETE"});
        showMessage(`Билет ${id} удалён или уже отсутствовал.`);
        await loadTickets();
    } catch (error) {
        showMessage(error.message, "error");
    }
});

document.getElementById("putForm").addEventListener("submit", async event => {
    event.preventDefault();
    try {
        const id = document.getElementById("putId").value;
        const ticket = parseJson("putTicket");
        const {data} = await callApi(ticketUrl("PUT", id, ticket), {method: "PUT"});
        renderTickets(data);
        showMessage(`Билет ${id} полностью обновлён.`);
    } catch (error) {
        showMessage(error.message, "error");
    }
});

document.getElementById("patchForm").addEventListener("submit", async event => {
    event.preventDefault();
    try {
        const id = document.getElementById("patchId").value;
        const ticket = parseJson("patchTicket");
        const {data} = await callApi(ticketUrl("PATCH", id, ticket), {method: "PATCH"});
        renderTickets(data);
        showMessage(`Билет ${id} частично обновлён.`);
    } catch (error) {
        showMessage(error.message, "error");
    }
});

document.getElementById("optionsButton").addEventListener("click", async () => {
    try {
        const {response} = await callApi(`${ticketBase}/tickets`, {method: "OPTIONS"});
        showMessage(`Разрешённые методы: ${response.headers.get("Allow") || "не указаны"}.`);
    } catch (error) {
        showMessage(error.message, "error");
    }
});

document.getElementById("maxTypeButton").addEventListener("click", async () => {
    try {
        const {data} = await callApi(`${ticketBase}/tickets/max-type`);
        renderTickets(data);
        showMessage("Найден билет с максимальным типом.");
    } catch (error) {
        showMessage(error.message, "error");
    }
});

document.getElementById("commentForm").addEventListener("submit", async event => {
    event.preventDefault();
    const params = new URLSearchParams({
        substring: document.getElementById("commentSubstring").value,
    });
    try {
        const {data} = await callApi(`${ticketBase}/tickets/comment-contains?${params}`);
        renderTickets(data);
        showMessage("Поиск по комментарию выполнен.");
    } catch (error) {
        showMessage(error.message, "error");
    }
});

document.getElementById("typeForm").addEventListener("submit", async event => {
    event.preventDefault();
    const params = new URLSearchParams({
        type: document.getElementById("greaterType").value,
    });
    try {
        const {data} = await callApi(`${ticketBase}/tickets/type-greater-than?${params}`);
        renderTickets(data);
        showMessage("Поиск по типу выполнен.");
    } catch (error) {
        showMessage(error.message, "error");
    }
});

document.getElementById("sellForm").addEventListener("submit", async event => {
    event.preventDefault();
    const ticketId = document.getElementById("sellTicketId").value;
    const personId = document.getElementById("sellPersonId").value;
    const price = document.getElementById("sellPrice").value;
    try {
        await callApi(`${bookingBase}/sell/${ticketId}/${personId}/${price}`, {method: "POST"});
        showTiger();
        showMessage(`Билет ${ticketId} продан человеку ${personId}.`);
    } catch (error) {
        showMessage(error.message, "error");
    }
});

document.getElementById("cancelForm").addEventListener("submit", async event => {
    event.preventDefault();
    const personId = document.getElementById("cancelPersonId").value;
    try {
        await callApi(`${bookingBase}/person/${personId}/cancel`, {method: "DELETE"});
        showMessage(`Все бронирования человека ${personId} отменены.`);
    } catch (error) {
        showMessage(error.message, "error");
    }
});

loadTickets();
