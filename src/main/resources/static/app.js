// What each role may do. The backend enforces the same rules (SecurityConfig.java);
// this table only decides which forms and buttons to show.
const PERMISSIONS = {
    ADMIN:   { add: true,  edit: true,  remove: true },
    TEACHER: { add: false, edit: true,  remove: false },
    STUDENT: { add: false, edit: false, remove: false },
};

// { header, username, role } while logged in. Kept in sessionStorage so a page refresh
// doesn't log you out; it is cleared when the tab is closed.
let auth = JSON.parse(sessionStorage.getItem('auth'));

const loginView = document.getElementById('login-view');
const loginForm = document.getElementById('login-form');
const loginError = document.getElementById('login-error');
const appView = document.getElementById('app-view');
const userBar = document.getElementById('user-bar');
const message = document.getElementById('message');

const sections = [...document.querySelectorAll('.entity-section')].map(createSection);

// ---------- API ----------

async function api(method, url, body) {
    const response = await fetch(url, {
        method,
        headers: { 'Authorization': auth.header, 'Content-Type': 'application/json' },
        body: body ? JSON.stringify(body) : undefined,
    });

    if (response.status === 401) {
        logout('Your session has expired. Please log in again.');
        throw new Error('Not logged in');
    }
    if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.detail || error.error || `Request failed with status ${response.status}`);
    }
    return response.status === 204 ? null : response.json();
}

// Runs an action and shows any error at the top of the page
async function run(action) {
    message.textContent = '';
    try {
        await action();
    } catch (error) {
        message.textContent = error.message;
    }
}

// ---------- Login / logout ----------

loginForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    loginError.textContent = '';

    const { username, password } = loginForm.elements;
    const header = 'Basic ' + btoa(`${username.value}:${password.value}`);

    try {
        const response = await fetch('/api/auth/me', { headers: { 'Authorization': header } });
        if (!response.ok) {
            loginError.textContent = 'Invalid username or password.';
            return;
        }
        const user = await response.json();
        auth = { header, username: user.username, role: user.role };
        sessionStorage.setItem('auth', JSON.stringify(auth));
        loginForm.reset();
        showApp();
    } catch {
        loginError.textContent = 'Could not reach the server.';
    }
});

document.getElementById('logout-btn').addEventListener('click', () => logout());

function showApp() {
    loginView.hidden = true;
    appView.hidden = false;
    userBar.hidden = false;
    document.getElementById('user-info').textContent = `${auth.username} (${auth.role})`;
    message.textContent = '';
    sections.forEach(section => section.show());
}

function logout(reason = '') {
    auth = null;
    sessionStorage.removeItem('auth');
    // Don't leave the previous user's data and buttons in the page for the next login
    document.querySelectorAll('.entity-section tbody').forEach(tbody => tbody.replaceChildren());
    appView.hidden = true;
    userBar.hidden = true;
    loginView.hidden = false;
    loginError.textContent = reason;
}

// ---------- Students / Teachers sections ----------

// Both sections work the same way, only the endpoint and label differ (see data-* in index.html)
function createSection(element) {
    const endpoint = element.dataset.endpoint;
    const label = element.dataset.label;
    const roles = element.dataset.roles.split(' ');
    const form = element.querySelector('.entity-form');
    const formTitle = element.querySelector('.form-title');
    const tbody = element.querySelector('tbody');
    const can = () => PERMISSIONS[auth.role];

    function show() {
        element.hidden = !roles.includes(auth.role);
        if (element.hidden) return;
        element.classList.toggle('read-only', !can().edit && !can().remove);
        resetForm();
        run(load);
    }

    // Back to "Add" mode. Only admins see the form unless they are editing a row.
    function resetForm() {
        form.reset();
        form.elements.id.value = '';
        formTitle.textContent = `Add ${label}`;
        form.hidden = !can().add;
    }

    function startEdit(item) {
        form.elements.id.value = item.id;
        form.elements.name.value = item.name;
        form.elements.department.value = item.department;
        formTitle.textContent = `Edit ${label} #${item.id}`;
        form.hidden = false;
        form.elements.name.focus();
    }

    async function load() {
        const items = await api('GET', endpoint);
        tbody.replaceChildren(...items.map(renderRow));
    }

    function renderRow(item) {
        const row = document.createElement('tr');
        for (const value of [item.id, item.name, item.department]) {
            const cell = document.createElement('td');
            cell.textContent = value; // textContent (not innerHTML) so names can't inject HTML
            row.appendChild(cell);
        }

        const actions = document.createElement('td');
        actions.className = 'actions-col';
        if (can().edit) actions.appendChild(makeButton('Edit', '', () => startEdit(item)));
        if (can().remove) actions.appendChild(makeButton('Delete', 'danger', () => remove(item)));
        row.appendChild(actions);
        return row;
    }

    function remove(item) {
        if (!confirm(`Delete ${label.toLowerCase()} "${item.name}"?`)) return;
        run(async () => {
            await api('DELETE', `${endpoint}/${item.id}`);
            await load();
        });
    }

    form.addEventListener('submit', (event) => {
        event.preventDefault();
        const id = form.elements.id.value;
        const body = {
            name: form.elements.name.value.trim(),
            department: form.elements.department.value.trim(),
        };
        run(async () => {
            if (id) {
                await api('PUT', `${endpoint}/${id}`, body);
            } else {
                await api('POST', endpoint, body);
            }
            resetForm();
            await load();
        });
    });

    element.querySelector('.cancel-btn').addEventListener('click', resetForm);

    return { show };
}

function makeButton(text, className, onClick) {
    const button = document.createElement('button');
    button.type = 'button';
    button.textContent = text;
    if (className) button.className = className;
    button.addEventListener('click', onClick);
    return button;
}

// Still logged in from before a page refresh?
if (auth) showApp();
