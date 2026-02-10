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

// Данные инициализации приложения с бэкенда (в т.ч. username бота).
let initState = null;

// Попытка получить invite-код из deep-link параметров запуска mini app.
function getInviteCodeFromLaunchContext() {
  const params = new URLSearchParams(window.location.search);
  const rawParam =
      params.get('invite') ||
      params.get('startapp') ||
      params.get('tgWebAppStartParam') ||
      (webApp && webApp.initDataUnsafe ? webApp.initDataUnsafe.start_param : null);

  if (!rawParam) {
    return null;
  }

  return String(rawParam).trim() || null;
}

function clearInviteParamsFromUrl() {
  const url = new URL(window.location.href);
  url.searchParams.delete('invite');
  url.searchParams.delete('startapp');
  url.searchParams.delete('tgWebAppStartParam');
  window.history.replaceState({}, '', url);
}

function normalizeBotUsername(value) {
  if (!value) {
    return null;
  }
  const normalized = String(value).trim().replace(/^@+/, '');
  return normalized || null;
}

function getBotUsernameFromLaunchContext() {
  if (initState && initState.botUsername) {
    const fromInit = normalizeBotUsername(initState.botUsername);
    if (fromInit) {
      return fromInit;
    }
  }

  const receiverUsername = webApp && webApp.initDataUnsafe && webApp.initDataUnsafe.receiver
      ? webApp.initDataUnsafe.receiver.username
      : null;
  const fromReceiver = normalizeBotUsername(receiverUsername);
  if (fromReceiver) {
    return fromReceiver;
  }

  const currentUrl = new URL(window.location.href);
  if (currentUrl.hostname === 't.me') {
    const pathUsername = normalizeBotUsername(currentUrl.pathname.replace(/^\/+/, '').split('/')[0]);
    if (pathUsername) {
      return pathUsername;
    }
  }

  return null;
}

function buildInviteLink(inviteCode) {
  const botUsername = getBotUsernameFromLaunchContext();
  if (botUsername) {
    const tgLink = new URL(`https://t.me/${botUsername}`);
    tgLink.searchParams.set('startapp', inviteCode);
    return tgLink.toString();
  }

  const url = new URL(window.location.href);
  url.search = '';
  url.hash = '';
  url.searchParams.set('invite', inviteCode);
  return url.toString();
}

// Общая функция для выполнения запросов к API.
async function apiFetch(path, options = {}) {
  const opts = { ...options };
  opts.headers = opts.headers ? { ...opts.headers } : {};
  if (initData) {
    // Telegram mini‑app initData contains a signed payload that the backend
    // uses to authenticate the current user. Historically we sent this as
    // the custom header X-TMA-Init-Data, but some reverse proxies (ngrok,
    // certain CDNs) strip or mangle non‑standard headers. To make
    // authentication more robust, always include the payload as an
    // Authorization header with the "TMA " prefix. The backend’s
    // TmaAuthFilter checks Authorization before falling back to
    // X-TMA-Init-Data, so sending both covers all cases.
    opts.headers['Authorization'] = `TMA ${initData}`;
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
    // Attempt to extract a readable message from JSON error responses.
    let errText;
    try {
      const rawBody = await response.text();
      if (rawBody) {
        try {
          const parsed = JSON.parse(rawBody);
          errText = parsed.message || parsed.error || rawBody;
        } catch (_) {
          errText = rawBody;
        }
      }
    } catch (_) {
      errText = response.statusText;
    }
    // Attach the HTTP status code to the error so callers can
    // distinguish 401/403 (unauthorized/unregistered) from other errors.
    const err = new Error(errText || 'HTTP ' + response.status);
    err.status = response.status;
    throw err;
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

// Отображает сообщение об ошибке только через системное уведомление Telegram.
// Не сохраняем ошибку в DOM, чтобы она не "прилипала" к текущему экрану.
function showError(err) {
  const msg = err instanceof Error ? err.message : String(err);
  notify(msg);
}

// Запускаем инициализацию приложения: проверяем, что пользователь зарегистрирован,
// и загружаем список его событий. Если пользователь ещё не завершил
// регистрацию в Telegram боте, сервер вернёт ошибку 403/401.
async function initApp() {
  showLoading();
  try {
    // /api/init returns 200 only if the user has completed onboarding.
    initState = await apiFetch('/api/init');

    const inviteCode = getInviteCodeFromLaunchContext();
    if (inviteCode) {
      try {
        const res = await apiFetch('/api/events/join', { method: 'POST', body: { inviteCode } });
        clearInviteParamsFromUrl();
        if (res.alreadyJoined) {
          await loadEventById(res.eventId);
        } else {
          renderClaimParticipants(inviteCode, res);
        }
        return;
      } catch (joinError) {
        showError(joinError);
      }
    }

    // When init succeeds, fetch and render the user's events.
    await loadEvents();
  } catch (e) {
    // If the server returns 401 or 403, the user hasn't finished registration.
    if (e && typeof e === 'object' && (e.status === 401 || e.status === 403)) {
      // Inform the user to complete registration in the bot.
      appDiv.innerHTML = '';
      const msgDiv = document.createElement('div');
      msgDiv.innerHTML = `
        <p>Вы ещё не завершили регистрацию в боте. Пожалуйста, откройте бота SplittyCat в Telegram, завершите процесс регистрации и затем перезапустите мини‑приложение.</p>
      `;
      appDiv.append(msgDiv);
    } else {
      // For other errors, show a generic error and allow retry.
      showError(e);
    }
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

// Загружает событие по ID через отдельную ручку деталей.
async function loadEventById(eventId) {
  showLoading();
  try {
    const event = await apiFetch(`/api/events/${eventId}`);
    await loadEvent(event);
  } catch (e) {
    showError(e);
    await loadEvents();
  }
}

// Рендер списка событий. Позволяет создавать новые события.
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
      // При клике загружаем детали события через отдельный endpoint.
      loadEventById(ev.id);
    };
    li.appendChild(title);
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
      const createdEvent = await apiFetch('/api/events', { method: 'POST', body: { title: name } });
      titleInput.value = '';
      await loadEventById(createdEvent.id);
    } catch (err) {
      showError(err);
    } finally {
      createBtn.disabled = false;
    }
  };
  appDiv.appendChild(createForm);
}

// Отрисовывает выбор участника при присоединении к событию.
function renderClaimParticipants(inviteCode, joinResponse) {
  appDiv.innerHTML = '';
  const h = document.createElement('h2');
  h.textContent = `Присоединение к событию «${joinResponse.title}»`;
  appDiv.appendChild(h);
  const info = document.createElement('p');
  info.textContent = joinResponse.unlinkedParticipants.length === 0
      ? 'В событии пока нет свободных участников. Вы можете создать новый слот и сразу привязать его к себе.'
      : 'Выберите свободного участника или создайте новый слот для себя:';
  appDiv.appendChild(info);
  if (joinResponse.unlinkedParticipants.length > 0) {
    const list = document.createElement('ul');
    joinResponse.unlinkedParticipants.forEach(p => {
      const li = document.createElement('li');
      const btn = document.createElement('button');
      btn.className = 'btn';
      btn.style.width = '100%';
      btn.textContent = p.name;
      btn.onclick = async () => {
        try {
          const claimedEvent = await apiFetch('/api/events/join/claim', { method: 'POST', body: { inviteCode: inviteCode, participantId: p.id } });
          await loadEventById(claimedEvent.id);
        } catch (err) {
          showError(err);
        }
      };
      li.appendChild(btn);
      list.appendChild(li);
    });
    appDiv.appendChild(list);
  }

  const createForm = document.createElement('form');
  createForm.className = 'section';
  const createTitle = document.createElement('h3');
  createTitle.textContent = 'Создать новый слот';
  createForm.appendChild(createTitle);

  const nameInput = document.createElement('input');
  nameInput.type = 'text';
  nameInput.name = 'participantName';
  nameInput.placeholder = 'Ваше имя в событии';
  nameInput.required = true;
  createForm.appendChild(nameInput);

  const createBtn = document.createElement('button');
  createBtn.type = 'submit';
  createBtn.className = 'btn';
  createBtn.textContent = 'Создать и присоединиться';
  createForm.appendChild(createBtn);

  createForm.onsubmit = async (e) => {
    e.preventDefault();
    const participantName = nameInput.value.trim();
    if (!participantName) {
      return;
    }
    createBtn.disabled = true;
    try {
      const claimedEvent = await apiFetch('/api/events/join/claim', {
        method: 'POST',
        body: { inviteCode: inviteCode, participantName: participantName }
      });
      await loadEventById(claimedEvent.id);
    } catch (err) {
      showError(err);
    } finally {
      createBtn.disabled = false;
    }
  };
  appDiv.appendChild(createForm);
  const cancelBtn = document.createElement('button');
  cancelBtn.className = 'btn secondary';
  cancelBtn.textContent = 'Отменить';
  cancelBtn.onclick = loadEvents;
  appDiv.appendChild(cancelBtn);
}

// Загружает детали события (участники, расходы, баланс) и отображает их.
async function loadEvent(event) {
  showLoading();
  try {
    // Параллельно запрашиваем детали события, участников, расходы и баланс
    const [eventDetails, participants, expenses, balance] = await Promise.all([
      apiFetch(`/api/events/${event.id}`),
      apiFetch(`/api/events/${event.id}/participants`),
      apiFetch(`/api/events/${event.id}/expenses`),
      apiFetch(`/api/events/${event.id}/my-balance`),
    ]);
    renderEventDetails(eventDetails, participants, expenses, balance);
  } catch (e) {
    showError(e);
    // Если ошибка, возвращаемся на список
    await loadEvents();
  }
}

// Рендерит страницу одного события.
function renderEventDetails(event, participants, expenses, balance) {
  const participantNameById = new Map(participants.map((participant) => [participant.id, participant.name]));
  appDiv.innerHTML = '';
  // Кнопка назад
  const backBtn = document.createElement('button');
  backBtn.className = 'btn secondary';
  backBtn.textContent = '← Назад';
  backBtn.onclick = loadEvents;
  appDiv.appendChild(backBtn);
  // Заголовок события
  const h1 = document.createElement('h1');
  h1.textContent = event.title;
  appDiv.appendChild(h1);

  const deleteEventBtn = document.createElement('button');
  deleteEventBtn.className = 'btn secondary danger';
  deleteEventBtn.textContent = 'Удалить событие';
  deleteEventBtn.onclick = async () => {
    if (!confirm('Удалить событие целиком? Это действие нельзя отменить.')) return;
    deleteEventBtn.disabled = true;
    try {
      await apiFetch(`/api/events/${event.id}`, { method: 'DELETE' });
      notify('Событие удалено.');
      await loadEvents();
    } catch (err) {
      showError(err);
    } finally {
      deleteEventBtn.disabled = false;
    }
  };
  appDiv.appendChild(deleteEventBtn);
  // Ссылка-приглашение и копирование
  const codeDiv = document.createElement('div');
  codeDiv.style.marginBottom = '8px';
  const inviteLink = buildInviteLink(event.inviteCode);
  const codeLabel = document.createElement('span');
  codeLabel.textContent = 'Ссылка-приглашение:';
  codeDiv.appendChild(codeLabel);
  const linkEl = document.createElement('a');
  linkEl.href = inviteLink;
  linkEl.textContent = inviteLink;
  linkEl.target = '_blank';
  linkEl.rel = 'noopener noreferrer';
  linkEl.style.marginLeft = '6px';
  codeDiv.appendChild(linkEl);
  const copyBtn = document.createElement('button');
  copyBtn.className = 'btn secondary';
  copyBtn.style.marginLeft = '8px';
  copyBtn.textContent = 'Копировать ссылку';
  copyBtn.onclick = async () => {
    try {
      await navigator.clipboard.writeText(inviteLink);
      notify('Ссылка скопирована в буфер обмена');
    } catch {
      notify('Не удалось скопировать ссылку');
    }
  };
  codeDiv.appendChild(copyBtn);
  appDiv.appendChild(codeDiv);

  // Секция участников
  const participantsSection = document.createElement('div');
  participantsSection.className = 'section';
  const ph = document.createElement('h2');
  ph.textContent = 'Участники';
  participantsSection.appendChild(ph);
  const pList = document.createElement('ul');
  participants.forEach(p => {
    const li = document.createElement('li');
    const name = document.createElement('span');
    name.textContent = p.name + (p.linked ? ' (привязан)' : '');
    li.appendChild(name);

    const deleteParticipantBtn = document.createElement('button');
    deleteParticipantBtn.className = 'btn secondary';
    deleteParticipantBtn.style.marginLeft = '8px';
    deleteParticipantBtn.textContent = 'Удалить';
    deleteParticipantBtn.onclick = async () => {
      if (!confirm(`Удалить участника «${p.name}»?`)) return;
      deleteParticipantBtn.disabled = true;
      try {
        await apiFetch(`/api/events/${event.id}/participants/${p.id}`, { method: 'DELETE' });
        await loadEvents();
      } catch (err) {
        showError(err);
      } finally {
        deleteParticipantBtn.disabled = false;
      }
    };
    li.appendChild(deleteParticipantBtn);

    pList.appendChild(li);
  });
  participantsSection.appendChild(pList);
  // Форма добавления участника
  const addParticipantForm = document.createElement('form');
  const addHeader = document.createElement('h3');
  addHeader.textContent = 'Добавить участника';
  addParticipantForm.appendChild(addHeader);
  const nameInput = document.createElement('input');
  nameInput.type = 'text';
  nameInput.placeholder = 'Имя участника';
  nameInput.required = true;
  addParticipantForm.appendChild(nameInput);
  const addBtn = document.createElement('button');
  addBtn.type = 'submit';
  addBtn.className = 'btn';
  addBtn.textContent = 'Добавить';
  addParticipantForm.appendChild(addBtn);
  addParticipantForm.onsubmit = async (e) => {
    e.preventDefault();
    const n = nameInput.value.trim();
    if (!n) return;
    addBtn.disabled = true;
    try {
      await apiFetch(`/api/events/${event.id}/participants`, { method: 'POST', body: { name: n } });
      await loadEvent(event);
    } catch (err) {
      showError(err);
    } finally {
      addBtn.disabled = false;
    }
  };
  participantsSection.appendChild(addParticipantForm);
  appDiv.appendChild(participantsSection);

  // Секция расходов
  const expensesSection = document.createElement('div');
  expensesSection.className = 'section';
  const eh = document.createElement('h2');
  eh.textContent = 'Расходы';
  expensesSection.appendChild(eh);
  if (expenses.length > 0) {
    const eList = document.createElement('ul');
    expenses.forEach(exp => {
      const li = document.createElement('li');
      const spanTitle = document.createElement('span');
      spanTitle.style.fontWeight = 'bold';
      spanTitle.textContent = exp.title;
      li.appendChild(spanTitle);
      const info = document.createElement('span');
      info.textContent = ` — ${exp.amount} ${exp.currencyCode}, платил ${exp.payerName}`;
      info.style.marginLeft = '4px';
      li.appendChild(info);

      const detailsBtn = document.createElement('button');
      detailsBtn.className = 'btn secondary';
      detailsBtn.style.marginLeft = '8px';
      detailsBtn.textContent = 'Детали';
      detailsBtn.onclick = async () => {
        detailsBtn.disabled = true;
        try {
          const expenseDetails = await apiFetch(`/api/events/${event.id}/expenses/${exp.id}`);
          renderExpenseDetails(li, expenseDetails, participantNameById);
        } catch (err) {
          showError(err);
        } finally {
          detailsBtn.disabled = false;
        }
      };
      li.appendChild(detailsBtn);

      // Кнопка удаления расхода
      const delBtn = document.createElement('button');
      delBtn.className = 'btn secondary';
      delBtn.style.marginLeft = '8px';
      delBtn.textContent = 'Удалить';
      delBtn.onclick = async () => {
        if (!confirm('Удалить этот расход?')) return;
        delBtn.disabled = true;
        try {
          await apiFetch(`/api/events/${event.id}/expenses/${exp.id}`, { method: 'DELETE' });
          await loadEvent(event);
        } catch (err) {
          showError(err);
        } finally {
          delBtn.disabled = false;
        }
      };
      li.appendChild(delBtn);
      eList.appendChild(li);
    });
    expensesSection.appendChild(eList);
  } else {
    const noExp = document.createElement('p');
    noExp.textContent = 'Пока что нет расходов.';
    expensesSection.appendChild(noExp);
  }
  // Форма добавления расхода
  const addExpForm = document.createElement('form');
  const expHeader = document.createElement('h3');
  expHeader.textContent = 'Добавить расход';
  addExpForm.appendChild(expHeader);
  const tInput = document.createElement('input');
  tInput.type = 'text';
  tInput.placeholder = 'Название расхода';
  tInput.required = true;
  addExpForm.appendChild(tInput);
  const amtInput = document.createElement('input');
  amtInput.type = 'number';
  amtInput.step = '0.01';
  amtInput.placeholder = 'Сумма';
  amtInput.required = true;
  addExpForm.appendChild(amtInput);
  const currencyInput = document.createElement('input');
  currencyInput.type = 'text';
  currencyInput.placeholder = 'Валюта (например, RUB)';
  currencyInput.value = 'RUB';
  currencyInput.required = true;
  addExpForm.appendChild(currencyInput);
  const dateInput = document.createElement('input');
  dateInput.type = 'date';
  // По умолчанию устанавливаем сегодняшнюю дату
  const today = new Date();
  dateInput.value = today.toISOString().split('T')[0];
  dateInput.required = true;
  addExpForm.appendChild(dateInput);
  // Выбор плательщика
  const payerSelect = document.createElement('select');
  participants.forEach(p => {
    const opt = document.createElement('option');
    opt.value = p.id;
    opt.textContent = p.name;
    payerSelect.appendChild(opt);
  });
  addExpForm.appendChild(payerSelect);
  // Динамический список долей
  const sharesDiv = document.createElement('div');
  sharesDiv.className = 'section';
  const shHeader = document.createElement('h4');
  shHeader.textContent = 'Доли участников';
  sharesDiv.appendChild(shHeader);
  const sharesList = document.createElement('ul');
  sharesDiv.appendChild(sharesList);

  const selectedParticipants = new Set(participants.map((p) => p.id));

  // Функция для перерасчёта равных долей
  function recalcShares() {
    sharesList.innerHTML = '';
    const amount = parseFloat(amtInput.value);
    const activeParticipants = participants.filter((p) => selectedParticipants.has(p.id));
    const perShare = activeParticipants.length > 0 && !isNaN(amount) ? amount / activeParticipants.length : 0;
    participants.forEach(p => {
      const li = document.createElement('li');
      const includeCheckbox = document.createElement('input');
      includeCheckbox.type = 'checkbox';
      includeCheckbox.checked = selectedParticipants.has(p.id);
      includeCheckbox.style.marginRight = '8px';
      includeCheckbox.onchange = () => {
        if (includeCheckbox.checked) {
          selectedParticipants.add(p.id);
        } else {
          selectedParticipants.delete(p.id);
        }
        recalcShares();
      };
      li.appendChild(includeCheckbox);
      const nameSpan = document.createElement('span');
      nameSpan.textContent = p.name;
      li.appendChild(nameSpan);
      const amountInput = document.createElement('input');
      amountInput.type = 'number';
      amountInput.step = '0.01';
      amountInput.min = '0';
      amountInput.value = includeCheckbox.checked && perShare ? perShare.toFixed(2) : '';
      amountInput.style.marginLeft = '8px';
      amountInput.style.width = '80px';
      amountInput.dataset.participantId = p.id;
      amountInput.dataset.fieldType = 'amount';
      amountInput.disabled = !includeCheckbox.checked;
      li.appendChild(amountInput);

      const descriptionInput = document.createElement('input');
      descriptionInput.type = 'text';
      descriptionInput.placeholder = 'Комментарий к доле';
      descriptionInput.style.marginLeft = '8px';
      descriptionInput.style.width = '220px';
      descriptionInput.dataset.participantId = p.id;
      descriptionInput.dataset.fieldType = 'description';
      descriptionInput.disabled = !includeCheckbox.checked;
      li.appendChild(descriptionInput);
      sharesList.appendChild(li);
    });
  }
  // Вызываем при изменении суммы
  amtInput.addEventListener('input', recalcShares);
  // Инициализируем доли
  recalcShares();

  addExpForm.appendChild(sharesDiv);
  const addExpBtn = document.createElement('button');
  addExpBtn.type = 'submit';
  addExpBtn.className = 'btn';
  addExpBtn.textContent = 'Добавить расход';
  addExpForm.appendChild(addExpBtn);
  addExpForm.onsubmit = async (e) => {
    e.preventDefault();
    const title = tInput.value.trim();
    const amountVal = amtInput.value;
    const currency = currencyInput.value.trim().toUpperCase();
    const date = dateInput.value;
    const payerId = payerSelect.value;
    if (!title || !amountVal || !currency || !date || !payerId) return;
    // Собираем доли
    const shares = [];
    sharesList.querySelectorAll('li').forEach(shareLi => {
      const amountInput = shareLi.querySelector('input[data-field-type="amount"]');
      const descriptionInput = shareLi.querySelector('input[data-field-type="description"]');
      if (!amountInput || amountInput.disabled) return;
      const amountValue = amountInput ? amountInput.value : '';
      if (!amountValue) return;
      const descriptionValue = descriptionInput ? descriptionInput.value.trim() : '';
      shares.push({
        participantId: parseInt(amountInput.dataset.participantId),
        amount: parseFloat(amountValue),
        description: descriptionValue
      });
    });
    if (shares.length === 0) {
      notify('Выберите хотя бы одного участника для распределения траты.');
      return;
    }
    // Проверяем, что сумма долей совпадает с общей суммой
    const sumShares = shares.reduce((acc, s) => acc + parseFloat(s.amount), 0);
    const total = parseFloat(amountVal);
    if (Math.abs(sumShares - total) > 0.01) {
      notify('Сумма долей не равна общей сумме. Проверьте ввод.');
      return;
    }
    addExpBtn.disabled = true;
    try {
      await apiFetch(`/api/events/${event.id}/expenses`, {
        method: 'POST',
        body: {
          title: title,
          amount: parseFloat(amountVal),
          currencyCode: currency,
          expenseDate: date,
          payerParticipantId: parseInt(payerId),
          shares: shares
        }
      });
      // Сбросим форму
      tInput.value = '';
      amtInput.value = '';
      recalcShares();
      // Перезагрузим данные события
      await loadEvent(event);
    } catch (err) {
      showError(err);
    } finally {
      addExpBtn.disabled = false;
    }
  };
  expensesSection.appendChild(addExpForm);
  appDiv.appendChild(expensesSection);

  // Секция баланса
  const balSection = document.createElement('div');
  balSection.className = 'section';
  const bh = document.createElement('h2');
  bh.textContent = 'Мой баланс';
  balSection.appendChild(bh);
  // Вы должны
  const youOwe = balance.youOwe || [];
  if (youOwe.length > 0) {
    const subtitle = document.createElement('h3');
    subtitle.textContent = 'Вы должны';
    balSection.appendChild(subtitle);
    const ulOwe = document.createElement('ul');
    youOwe.forEach(e => {
      const li = document.createElement('li');
      li.textContent = `${e.participantName}: ${e.amount} ${e.currencyCode}`;
      ulOwe.appendChild(li);
    });
    balSection.appendChild(ulOwe);
  }
  // Вам должны
  const oweYou = balance.oweYou || [];
  if (oweYou.length > 0) {
    const subtitle2 = document.createElement('h3');
    subtitle2.textContent = 'Вам должны';
    balSection.appendChild(subtitle2);
    const ulOweYou = document.createElement('ul');
    oweYou.forEach(e => {
      const li = document.createElement('li');
      li.textContent = `${e.participantName}: ${e.amount} ${e.currencyCode}`;
      ulOweYou.appendChild(li);
    });
    balSection.appendChild(ulOweYou);
  }
  if (youOwe.length === 0 && oweYou.length === 0) {
    const p = document.createElement('p');
    p.textContent = 'Баланс по этому событию нулевой.';
    balSection.appendChild(p);
  }
  appDiv.appendChild(balSection);
}

function renderExpenseDetails(parentLi, expenseDetails, participantNameById = new Map()) {
  const existing = parentLi.querySelector('.expense-details');
  if (existing) {
    existing.remove();
    return;
  }

  const detailsDiv = document.createElement('div');
  detailsDiv.className = 'expense-details';

  const meta = document.createElement('p');
  meta.textContent = `Дата: ${expenseDetails.expenseDate}, плательщик: ${expenseDetails.payer.name}`;
  detailsDiv.appendChild(meta);

  const sharesHeader = document.createElement('strong');
  sharesHeader.textContent = 'Разбивка долей:';
  detailsDiv.appendChild(sharesHeader);

  const sharesList = document.createElement('ul');
  expenseDetails.shares.forEach((share) => {
    const shareLi = document.createElement('li');
    const note = share.description ? ` (${share.description})` : '';
    const participantName = participantNameById.get(share.participantId) || `ID участника ${share.participantId}`;
    shareLi.textContent = `${participantName}: ${share.amount}${note}`;
    sharesList.appendChild(shareLi);
  });
  detailsDiv.appendChild(sharesList);
  parentLi.appendChild(detailsDiv);
}

// Запускаем приложение после загрузки DOM
document.addEventListener('DOMContentLoaded', () => {
  // Открываем встроенную клавиатуру mini app, чтобы пользователь видел UI
  if (webApp && typeof webApp.ready === 'function') {
    webApp.ready();
  }
  initApp();
});