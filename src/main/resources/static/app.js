// Страница банка крови. Один скрипт, без библиотек.
// Токен живёт ТОЛЬКО в этой переменной: не в localStorage, не в адресе, не на странице.
let token = null;
let expiresAt = 0;        // когда токен перестанет работать (миллисекунды)
let timerId = null;

// Пакет, с которым работаем. Не секрет, поэтому после выхода остаётся:
// так главврач после входа выдаст тот же пакет, который довёл до RELEASED лаборант
let unitId = null;
let unitNumber = null;
let unitStatus = null;

const STEPS = ['COLLECTED', 'TESTED', 'RELEASED', 'ISSUED'];
const NO_UNIT = '00000000-0000-0000-0000-000000000000';

function byId(id) {
    return document.getElementById(id);
}

// ---------- запросы ----------

// Все обращения к серверу идут через эту функцию: токен и X-Correlation-Id добавляются здесь
async function call(method, path, body) {
    const cid = 'web-' + Math.random().toString(16).slice(2, 10);
    const headers = { 'X-Correlation-Id': cid };
    if (body) headers['Content-Type'] = 'application/json';
    if (token) headers['Authorization'] = 'Bearer ' + token;

    try {
        const response = await fetch(path, {
            method: method,
            headers: headers,
            body: body ? JSON.stringify(body) : undefined
        });
        let data = null;
        try { data = await response.json(); } catch (e) { }   // тело может быть пустым
        return { status: response.status, data: data, cid: cid };
    } catch (e) {
        return { status: 0, data: { code: 'NETWORK', message: 'сервер не отвечает', correlationId: cid }, cid: cid };
    }
}

// ---------- вывод ответа (только textContent, никакого innerHTML) ----------

function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
}

function badgeClass(status) {
    if (status >= 200 && status < 300) return 'ok';
    if (status === 401 || status === 409) return 'warn';
    return 'bad';
}

function renderResult(box, r, note) {
    box.replaceChildren();
    box.hidden = false;
    box.appendChild(el('h2', null, 'Ответ сервера'));

    const line = el('div', 'line');
    line.appendChild(el('span', 'badge ' + badgeClass(r.status), r.status === 0 ? 'Нет связи' : 'HTTP ' + r.status));
    if (note) line.appendChild(el('span', 'note', note));
    box.appendChild(line);

    const data = r.data || {};
    const rows = [];
    if (data.code) rows.push(['code', data.code]);
    if (data.message) rows.push(['message', data.message]);
    if (data.status) rows.push(['status', data.status]);
    if (data.businessKey) rows.push(['businessKey', data.businessKey]);
    rows.push([data.correlationId ? 'correlationId' : 'X-Correlation-Id', data.correlationId || r.cid]);

    const list = el('dl', 'fields');
    for (const row of rows) {
        const item = el('div', 'field');
        item.appendChild(el('dt', null, row[0]));
        item.appendChild(el('dd', null, row[1]));
        list.appendChild(item);
    }
    box.appendChild(list);
}

function renderMessage(box, text) {
    box.replaceChildren();
    box.hidden = false;
    box.appendChild(el('h2', null, 'Ответ сервера'));
    box.appendChild(el('p', 'empty', text));
}

// ---------- экраны ----------

function showScreen(name) {
    byId('screen-login').hidden = name !== 'login';
    byId('screen-work').hidden = name !== 'work';
    byId('session').hidden = name !== 'work';
}

function drawChain() {
    const chain = byId('chain');
    chain.replaceChildren();
    for (const step of STEPS) {
        chain.appendChild(el('span', step === unitStatus ? 'step current' : 'step', step));
    }
    byId('unit-line').textContent = unitId
        ? 'Пакет ' + unitNumber + ' (' + unitId.slice(0, 8) + '…)'
        : 'Пакета ещё нет';
}

function randomNumber() {
    return 'BU-2026-' + String(Math.floor(Math.random() * 900000) + 100000);
}

// ---------- вход и выход ----------

function startTimer() {
    clearInterval(timerId);
    timerId = setInterval(function () {
        const left = Math.floor((expiresAt - Date.now()) / 1000);
        if (left <= 0) {
            expired();
            return;
        }
        const minutes = String(Math.floor(left / 60)).padStart(2, '0');
        const seconds = String(left % 60).padStart(2, '0');
        byId('timer').textContent = 'токен ещё ' + minutes + ':' + seconds;
    }, 1000);
}

// Сбросить всё, что связано с токеном, и вернуться к форме
function dropSession() {
    token = null;
    clearInterval(timerId);
    byId('who').textContent = '';
    byId('timer').textContent = '';
    byId('password').value = '';
    showScreen('login');
}

function logout() {
    dropSession();
    byId('banner').hidden = true;
    byId('login-result').hidden = true;
}

function expired() {
    dropSession();
    byId('login-result').hidden = true;
    const banner = byId('banner');
    banner.textContent = 'Токен истёк, войдите заново';
    banner.hidden = false;
}

async function login(username, password) {
    const r = await call('POST', '/api/v1/auth/login', { username: username, password: password });
    if (r.status !== 200) {
        renderResult(byId('login-result'), r, 'Форма осталась, токена нет');
        return;
    }

    token = r.data.accessToken;
    expiresAt = Date.now() + r.data.expiresIn * 1000;

    // кто я: имя и роль приходят от сервера, а не из того, что ввёл человек
    const me = await call('GET', '/api/me');
    if (me.status !== 200) {
        token = null;
        renderResult(byId('login-result'), me, 'Не удалось узнать роль');
        return;
    }
    const role = me.data.authorities[0].replace('ROLE_', '');
    byId('who').textContent = me.data.username + ' · ' + role;

    byId('banner').hidden = true;
    byId('login-result').hidden = true;
    byId('work-result').replaceChildren();
    byId('work-result').appendChild(el('h2', null, 'Ответ сервера'));
    byId('work-result').appendChild(el('p', 'empty', 'Пока нет запросов'));
    startTimer();
    byId('timer').textContent = 'токен ещё 15:00';
    drawChain();
    showScreen('work');
}

// Запрос без токена: показывает, что защита закрывает API
async function checkProtection() {
    const r = await call('GET', '/api/v1/blood-units/' + (unitId || NO_UNIT));
    renderResult(byId('login-result'), r, 'Запрос ушёл без токена');
}

// ---------- работа с пакетом ----------

function setUnit(data) {
    unitId = data.id;
    unitNumber = data.businessKey;
    unitStatus = data.status;
    drawChain();
}

async function createUnit() {
    const body = {
        businessKey: byId('unit-number').value.trim(),
        title: byId('unit-title').value.trim()
    };
    const r = await call('POST', '/api/v1/blood-units', body);
    if (r.status === 201) {
        setUnit(r.data);
        byId('unit-number').value = randomNumber();
    }
    renderResult(byId('work-result'), r, r.status === 201 ? 'Пакет создан' : '');
    if (r.status === 401 && token) expired();
    return r;
}

// Создать, отправить на анализы и допустить к выдаче: три запроса подряд, стоп на первой ошибке
async function createReady() {
    const created = await createUnit();
    if (created.status !== 201) return;

    for (const verb of ['test', 'release']) {
        const r = await call('POST', '/api/v1/blood-units/' + unitId + '/' + verb);
        if (r.status === 200) setUnit(r.data);
        renderResult(byId('work-result'), r, r.status === 200 ? 'Новый статус: ' + r.data.status : '');
        if (r.status === 401 && token) expired();
        if (r.status !== 200) return;
    }
}

// Прочитать пакет заново: после отказа видно, что в таблице ничего не изменилось
async function rereadStatus() {
    const r = await call('GET', '/api/v1/blood-units/' + unitId);
    if (r.status !== 200) return '';
    unitStatus = r.data.status;
    drawChain();
    return r.data.status;
}

async function runAction(verb) {
    if (!unitId) {
        renderMessage(byId('work-result'), 'Сначала создайте пакет');
        return;
    }

    const r = await call('POST', '/api/v1/blood-units/' + unitId + '/' + verb);
    let note = '';
    if (r.status === 200) {
        setUnit(r.data);
        note = 'Новый статус: ' + r.data.status;
    } else if (r.status === 403) {
        note = 'Статус в таблице не изменился: ' + (await rereadStatus());
    } else if (r.status === 409) {
        note = 'Правила не разрешают переход, статус в таблице: ' + (await rereadStatus());
    }
    renderResult(byId('work-result'), r, note);

    // токен был, а сервер его не принял: значит истёк
    if (r.status === 401 && token) expired();
}

// ---------- подключение кнопок ----------

byId('login-form').addEventListener('submit', function (event) {
    event.preventDefault();
    login(byId('username').value.trim(), byId('password').value);
});

for (const row of document.querySelectorAll('.dev-row')) {
    row.addEventListener('click', function () {
        byId('username').value = row.dataset.user;
        byId('password').value = row.dataset.pass;
    });
}

byId('logout-btn').addEventListener('click', logout);
byId('check-btn').addEventListener('click', checkProtection);
byId('create-btn').addEventListener('click', createUnit);
byId('create-ready-btn').addEventListener('click', createReady);

for (const button of document.querySelectorAll('[data-verb]')) {
    button.addEventListener('click', function () {
        runAction(button.dataset.verb);
    });
}

byId('unit-number').value = randomNumber();
drawChain();
showScreen('login');
