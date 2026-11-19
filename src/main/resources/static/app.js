let token = null;   // токен живёт только здесь

async function call(method, path, body) {
    const headers = { 'X-Correlation-Id': 'web-' + Math.random().toString(16).slice(2, 10) };
    if (body) headers['Content-Type'] = 'application/json';
    if (token) headers['Authorization'] = 'Bearer ' + token;

    const response = await fetch(path, {
        method: method,
        headers: headers,
        body: body ? JSON.stringify(body) : undefined
    });

    let data = null;
    try { data = await response.json(); } catch (e) { }   // тело может быть пустым
    return { status: response.status, data: data };
}
