/*
 * Основной клиентский код для мини‑приложения SplittyCat.
 *
 * Этот файл реализует простой одностраничный интерфейс,
 * который работает внутри Telegram Mini App и обращается
 * к REST API приложения SplittyCat. Пользователь может
 * просматривать свои события, создавать новые, присоединяться
 * по коду приглашения, а также управлять участниками и расходами
 * конкретного события. Код написан без внешних зависимостей
 * и использует стандартные Web API.
 */

// Telegram Mini App API доступен на объекте window.Telegram.WebApp.
const webApp = window.Telegram ? window.Telegram.WebApp : null;
// Строка initData используется для аутентификации на бэкенде.
const initData = webApp && webApp.initData ? webApp.initData : '';

// Корневой контейнер, в который мы рендерим содержимое приложения.
const appDiv = document.getElementById('app');

// Общая функция для выполнения запросов к API.
async function apiFetch(path, options = {}) {
  const opts = { ...options };
  opts.headers = opts.headers ? { ...opts.headers } : {};
  if (initData) {
    // Бэкенд проверяет заголовок X-TMA-Init-Data и извлекает
    // идентификатор Telegram пользователя из подписи.
    opts.headers['X-TMA-Init-Data'] = initData;
  }
  // При отправке JSON автоматически сериализуем тело
  if (opts.body && typeof opts.body === 'object' && !(opts.body instanceof FormData)) {
    opts.headers['Content-Type'] = 'application/json';
    opts.body = JSON.stringify(opts.body);
  }
  // По умолчанию считаем, что ответ JSON (кроме 204).
  const response = await fetch(path, opts);
  if (!response.ok) {
    // Попробуем прочитать сообщение об ошибке
    let errText;
    try {
      errText = await response.text();
    } catch (_) {
      errText = response.statusText;
    }
    throw new Error(errText || 'HTTP ' + response.status);
  }
  if (response.status === 204) {
    return null;
  }
  const contentType = response.headers.get('Content-Type');
  if (contentType && contentType.includes('application/json')) {
    return response.json();
  }
  return response.text();
}

// Выводит временное уведомление. По возможности используем Telegram API.
function notify(message) {
  if (webApp && typeof webApp.showAlert === 'function') {
    webApp.showAlert(message);
  } else {
    alert(message);
  }
}

// Показывает индикатор загрузки. Вызывайте перед асинхронными операциями.
function showLoading(text = 'Загрузка...') {
  appDiv.innerHTML = `<p>${text}</p>`;
}

// Отображает сообщение об ошибке. Помимо уведомления, вставляет текст в DOM.
function showError(err) {
  const msg = err instanceof Error ? err.message : String(err);
  notify(msg);
  const p = document.createElement('p');
  p.className = 'error';
  p.textContent = msg;
  appDiv.prepend(p);
}

// Запускаем инициализацию приложения: проверяем, что пользователь зарегистрирован,
// и загружаем список его событий. Если пользователь ещё не завершил
// регистрацию в Telegram боте, сервер вернёт ошибку 403/401.
async function initApp() {
  showLoading();
  try {
    const userStatus = await apiFetch('/api/check-user-status');
    if (!userStatus.isRegistered) {
      throw new Error('Пользователь не зарегистрирован');
    }
    await loadEvents();
  } catch (e) {
    showError(e);
    // Поясняем пользователю, что нужно завершить регистрацию в боте
    const div = document.createElement('div');
    div.innerHTML = `
      <p>Кажется, вы ещё не прошли регистрацию в боте. Пожалуйста, откройте бот SplittyCat в Telegram и завершите процесс регистрации, затем вернитесь в мини‑приложение.</p>
    `;
    appDiv.append(div);
  }
}

// Загружает список событий текущего пользователя и рендерит их.
async function loadEvents() {
  showLoading();
  try {
    const events = await apiFetch('/api/events');
    renderEventsList(events);
  } catch (e) {
    showError(e);
  }
}

// Рендер списка событий. Позволяет создавать новые события и
// присоединяться к существующим по invite‑коду.
function renderEventsList(events) {
  appDiv.innerHTML = '';
  const header = document.createElement('h1');
  header.textContent = 'Мои события';
  appDiv.appendChild(header);

  // Список существующих событий
  const list = document.createElement('ul');
  events.forEach(ev => {
    const li = document.createElement('li');
    const title = document.createElement('span');
    title.textContent = ev.title;
    title.style.cursor = 'pointer';
    title.style.fontWeight = 'bold';
    title.onclick = () => {
      // При клике загружаем детали события
      loadEvent(ev);
    };
    li.appendChild(title);
    // Показываем код приглашения более бледным шрифтом
    const code = document.createElement('span');
    code.textContent = ` (код: ${ev.inviteCode})`;
    code.style.marginLeft = '8px';
    code.style.color = '#666';
    li.appendChild(code);
    list.appendChild(li);
  });
  if (events.length > 0) {
    const section = document.createElement('div');
    section.className = 'section';
    const h = document.createElement('h2');
    h.textContent = 'Список событий';
    section.appendChild(h);
    section.appendChild(list);
    appDiv.appendChild(section);
  }

  // Форма создания события
  const createForm = document.createElement('form');
  createForm.id = 'create-event-form';
  const createHeader = document.createElement('h2');
  createHeader.textContent = 'Создать событие';
  createForm.appendChild(createHeader);
  const titleInput = document.createElement('input');
  titleInput.name = 'title';
  titleInput.type = 'text';
  titleInput.placeholder = 'Название события';
  titleInput.required = true;
  createForm.appendChild(titleInput);
  const createBtn = document.createElement('button');
  createBtn.type = 'submit';
  createBtn.className = 'btn';
  createBtn.textContent = 'Создать';
  createForm.appendChild(createBtn);
  createForm.onsubmit = async (e) => {
    e.preventDefault();
    const name = titleInput.value.trim();
    if (!name) return;
    createBtn.disabled = true;
    try {
      await apiFetch('/api/events', { method: 'POST', body: { title: name } });
      titleInput.value = '';
      await loadEvents();
    } catch (err) {
      showError(err);
    } finally {
      createBtn.disabled = false;
    }
  };
  appDiv.appendChild(createForm);

  // Форма присоединения по invite‑коду
  const joinForm = document.createElement('form');
  joinForm.id = 'join-event-form';
  const joinHeader = document.createElement('h2');
  joinHeader.textContent = 'Присоединиться к событию';
  joinForm.appendChild(joinHeader);
  const codeInput = document.createElement('input');
  codeInput.name = 'code';
  codeInput.type = 'text';
  codeInput.placeholder = 'Код приглашения';
  codeInput.required = true;
  joinForm.appendChild(codeInput);
  const joinBtn = document.createElement('button');
  joinBtn.type = 'submit';
  joinBtn.className = 'btn';
  joinBtn.textContent = 'Присоединиться';
  joinForm.appendChild(joinBtn);
  joinForm.onsubmit = async (e) => {
    e.preventDefault();
    const code = codeInput.value.trim();
    if (!code) return;
    joinBtn.disabled = true;
    try {
      const res = await apiFetch('/api/events/join', { method: 'POST', body: { inviteCode: code } });
      if (res.alreadyJoined) {
        // Событие уже привязано, загружаем его
        await loadEvent({ id: res.eventId, title: res.title, inviteCode: res.inviteCode });
      } else {
        // Нужно выбрать участника и привязать
        renderClaimParticipants(code, res);
      }
    } catch (err) {
      showError(err);
    } finally {
      joinBtn.disabled = false;
    }
  };
  appDiv.appendChild(joinForm);
}

// Запускаем приложение после загрузки DOM
document.addEventListener('DOMContentLoaded', () => {
  // Открываем встроенную клавиатуру mini app, чтобы пользователь видел UI
  if (webApp && typeof webApp.ready === 'function') {
    webApp.ready();
  }
  initApp();
});
