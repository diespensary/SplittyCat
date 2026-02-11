/*
 * SplittyCat Mini App — UI refresh without breaking API / flows.
 * No external dependencies. Works inside Telegram Mini App and in browser.
 */

const webApp = window.Telegram?.WebApp ?? null;
const initData = webApp?.initData ?? '';

const appDiv = document.getElementById('app');
let initState = null;

/* ----------------------------- Theme helpers ----------------------------- */

function applyTelegramTheme() {
  const root = document.documentElement;
  const tp = webApp?.themeParams ?? {};

  // Telegram theme params (may be undefined outside TMA)
  const vars = {
    '--tg-bg': tp.bg_color || '',
    '--tg-text': tp.text_color || '',
    '--tg-hint': tp.hint_color || '',
    '--tg-link': tp.link_color || '',
    '--tg-button': tp.button_color || '',
    '--tg-button-text': tp.button_text_color || '',
    '--tg-secondary-bg': tp.secondary_bg_color || '',
  };

  Object.entries(vars).forEach(([k, v]) => {
    if (v) root.style.setProperty(k, v);
  });

  // Prefer Telegram's colorScheme if present
  const scheme = webApp?.colorScheme; // "light" | "dark"
  if (scheme) {
    root.dataset.scheme = scheme;
  }
}

if (webApp) {
  try {
    applyTelegramTheme();
    if (typeof webApp.onEvent === 'function') {
      webApp.onEvent('themeChanged', applyTelegramTheme);
    }
    if (typeof webApp.ready === 'function') webApp.ready();
    if (typeof webApp.expand === 'function') webApp.expand();
  } catch (_) {
    // ignore
  }
}

/* ----------------------------- UI primitives ----------------------------- */

function el(tag, attrs = null, ...children) {
  const node = document.createElement(tag);
  if (attrs) {
    for (const [k, v] of Object.entries(attrs)) {
      if (v === null || v === undefined) continue;
      if (k === 'className') node.className = v;
      else if (k === 'text') node.textContent = v;
      else if (k === 'html') node.innerHTML = v;
      else if (k.startsWith('on') && typeof v === 'function') node.addEventListener(k.slice(2), v);
      else node.setAttribute(k, String(v));
    }
  }
  for (const child of children) {
    if (child === null || child === undefined) continue;
    node.appendChild(typeof child === 'string' ? document.createTextNode(child) : child);
  }
  return node;
}

function setView(node) {
  appDiv.innerHTML = '';
  appDiv.appendChild(node);
}

function makeContainer(...children) {
  return el('div', { className: 'container' }, ...children);
}

function makeCard(title, subtitle, ...children) {
  const head = el(
      'div',
      { className: 'card__head' },
      el('div', { className: 'card__title', text: title || '' }),
      subtitle ? el('div', { className: 'card__subtitle', text: subtitle }) : null
  );

  return el('div', { className: 'card' }, title || subtitle ? head : null, ...children);
}

function makeRow(left, right) {
  return el('div', { className: 'row' }, el('div', { className: 'row__left' }, left), el('div', { className: 'row__right' }, right));
}

function makeP(text, cls = 'muted') {
  return el('p', { className: cls, text });
}

function showLoading(text = 'Загрузка...') {
  const view = makeContainer(
      el(
          'div',
          { className: 'loading' },
          el('div', { className: 'spinner', 'aria-hidden': 'true' }),
          el('div', { className: 'loading__text', text })
      )
  );
  setView(view);
}

function notify(message) {
  if (webApp && typeof webApp.showAlert === 'function') webApp.showAlert(message);
  else alert(message);
}

function showError(err) {
  const msg = err instanceof Error ? err.message : String(err);
  notify(msg);
  // Useful for debugging in browser
  try { console.error(err); } catch (_) {}
}

async function copyToClipboard(text) {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text);
      return true;
    }
  } catch (_) {}
  // fallback
  try {
    const ta = el('textarea', { className: 'sr-only' }, text);
    document.body.appendChild(ta);
    ta.value = text;
    ta.select();
    const ok = document.execCommand('copy');
    ta.remove();
    return ok;
  } catch (_) {
    return false;
  }
}

/* ----------------------------- Launch context ----------------------------- */

function getInviteCodeFromLaunchContext() {
  const params = new URLSearchParams(window.location.search);
  const rawParam =
      params.get('invite') ||
      params.get('startapp') ||
      params.get('tgWebAppStartParam') ||
      (webApp?.initDataUnsafe ? webApp.initDataUnsafe.start_param : null);

  if (!rawParam) return null;
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
  if (!value) return null;
  const normalized = String(value).trim().replace(/^@+/, '');
  return normalized || null;
}

function getBotUsernameFromLaunchContext() {
  if (initState?.botUsername) {
    const fromInit = normalizeBotUsername(initState.botUsername);
    if (fromInit) return fromInit;
  }

  const receiverUsername = webApp?.initDataUnsafe?.receiver?.username ?? null;
  const fromReceiver = normalizeBotUsername(receiverUsername);
  if (fromReceiver) return fromReceiver;

  const currentUrl = new URL(window.location.href);
  if (currentUrl.hostname === 't.me') {
    const pathUsername = normalizeBotUsername(currentUrl.pathname.replace(/^\/+/, '').split('/')[0]);
    if (pathUsername) return pathUsername;
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

/* ----------------------------- API fetch helper ----------------------------- */

async function apiFetch(path, options = {}) {
  const opts = { ...options };
  opts.headers = opts.headers ? { ...opts.headers } : {};

  if (initData) {
    opts.headers['Authorization'] = `TMA ${initData}`;
    opts.headers['X-TMA-Init-Data'] = initData;
  }

  opts.headers['Accept'] = opts.headers['Accept'] || 'application/json';

  if (opts.body && typeof opts.body === 'object' && !(opts.body instanceof FormData)) {
    opts.headers['Content-Type'] = 'application/json';
    opts.body = JSON.stringify(opts.body);
  }

  const response = await fetch(path, opts);

  if (!response.ok) {
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
    const err = new Error(errText || 'HTTP ' + response.status);
    err.status = response.status;
    throw err;
  }

  if (response.status === 204) return null;

  const contentType = response.headers.get('Content-Type');
  if (contentType && contentType.includes('application/json')) return response.json();
  return response.text();
}

/* ----------------------------- App init & routing ----------------------------- */

async function initApp() {
  showLoading('Инициализация…');

  try {
    // /api/init returns 200 only if user completed onboarding
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

    await loadEvents();
  } catch (e) {
    if (e && typeof e === 'object' && (e.status === 401 || e.status === 403)) {
      renderNotRegistered();
    } else {
      showError(e);
      renderRetry('Не удалось загрузить приложение. Попробуйте ещё раз.');
    }
  }
}

function renderRetry(text) {
  const retryBtn = el('button', { className: 'btn', type: 'button' }, 'Повторить');
  retryBtn.onclick = initApp;

  setView(
      makeContainer(
          makeCard(
              'Ошибка',
              null,
              makeP(text, 'text'),
              el('div', { className: 'actions' }, retryBtn)
          )
      )
  );
}

function renderNotRegistered() {
  const botUsername = getBotUsernameFromLaunchContext();
  const botLink = botUsername ? `https://t.me/${botUsername}` : null;

  const openBot = botLink
      ? el('a', { className: 'btn', href: botLink, target: '_blank', rel: 'noopener noreferrer' }, 'Открыть бота')
      : null;

  const retryBtn = el('button', { className: 'btn secondary', type: 'button' }, 'Я завершил регистрацию — обновить');
  retryBtn.onclick = initApp;

  setView(
      makeContainer(
          makeCard(
              'Регистрация не завершена',
              null,
              el(
                  'p',
                  { className: 'text' },
                  'Вы ещё не завершили регистрацию в боте. Откройте бота SplittyCat в Telegram, завершите процесс регистрации и затем перезапустите мини-приложение.'
              ),
              el('div', { className: 'actions' }, openBot, retryBtn)
          )
      )
  );
}

/* ----------------------------- Events list ----------------------------- */

async function loadEvents() {
  showLoading('Загружаю события…');
  try {
    const events = await apiFetch('/api/events');
    renderEventsList(events);
  } catch (e) {
    showError(e);
    renderRetry('Не удалось загрузить события.');
  }
}

async function loadEventById(eventId) {
  showLoading('Загружаю событие…');
  try {
    const event = await apiFetch(`/api/events/${eventId}`);
    await loadEvent(event);
  } catch (e) {
    showError(e);
    await loadEvents();
  }
}

function renderEventsList(events) {
  const header = el(
      'div',
      { className: 'topbar' },
      el('div', { className: 'topbar__title' }, el('h1', { text: 'Мои события' }), el('div', { className: 'muted' }, 'SplittyCat')),
  );

  const listCard =
      events.length > 0
          ? makeCard(
              'Список событий',
              'Нажмите на событие, чтобы открыть детали.',
              el(
                  'div',
                  { className: 'list' },
                  ...events.map((ev) => {
                    const btn = el('button', { className: 'list__item', type: 'button' },
                        el('div', { className: 'list__main' },
                            el('div', { className: 'list__title', text: ev.title }),
                            el('div', { className: 'list__meta', text: `ID: ${ev.id}` })
                        ),
                        el('div', { className: 'list__chev', 'aria-hidden': 'true' }, '›')
                    );
                    btn.onclick = () => loadEventById(ev.id);
                    return btn;
                  })
              )
          )
          : makeCard(
              'Пока нет событий',
              null,
              makeP('Создайте первое событие ниже.', 'text')
          );

  // Create form
  const titleInput = el('input', {
    name: 'title',
    type: 'text',
    placeholder: 'Название события',
    required: 'true',
    autocomplete: 'off',
    inputmode: 'text'
  });

  const createBtn = el('button', { className: 'btn', type: 'submit' }, 'Создать');

  const createForm = el(
      'form',
      { id: 'create-event-form', className: 'form' },
      el('div', { className: 'form__row' }, titleInput),
      el('div', { className: 'actions' }, createBtn)
  );

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

  const createCard = makeCard('Создать событие', null, createForm);

  setView(makeContainer(header, listCard, createCard));
}

/* ----------------------------- Join / Claim participants ----------------------------- */

function renderClaimParticipants(inviteCode, joinResponse) {
  const title = `Присоединение к событию «${joinResponse.title}»`;

  const infoText =
      joinResponse.unlinkedParticipants.length === 0
          ? 'В событии пока нет свободных участников. Вы можете создать новый слот и сразу привязать его к себе.'
          : 'Выберите свободного участника или создайте новый слот для себя:';

  const list =
      joinResponse.unlinkedParticipants.length > 0
          ? el(
              'div',
              { className: 'list' },
              ...joinResponse.unlinkedParticipants.map((p) => {
                const btn = el(
                    'button',
                    { className: 'list__item', type: 'button' },
                    el('div', { className: 'list__main' }, el('div', { className: 'list__title', text: p.name })),
                    el('div', { className: 'list__chev', 'aria-hidden': 'true' }, '›')
                );

                btn.onclick = async () => {
                  btn.disabled = true;
                  try {
                    const claimedEvent = await apiFetch('/api/events/join/claim', {
                      method: 'POST',
                      body: { inviteCode: inviteCode, participantId: p.id },
                    });
                    await loadEventById(claimedEvent.id);
                  } catch (err) {
                    showError(err);
                  } finally {
                    btn.disabled = false;
                  }
                };
                return btn;
              })
          )
          : null;

  const nameInput = el('input', {
    type: 'text',
    name: 'participantName',
    placeholder: 'Ваше имя в событии',
    required: 'true',
    autocomplete: 'off',
  });

  const createBtn = el('button', { className: 'btn', type: 'submit' }, 'Создать и присоединиться');

  const createForm = el(
      'form',
      { className: 'form' },
      el('div', { className: 'form__row' }, nameInput),
      el('div', { className: 'actions' }, createBtn)
  );

  createForm.onsubmit = async (e) => {
    e.preventDefault();
    const participantName = nameInput.value.trim();
    if (!participantName) return;

    createBtn.disabled = true;
    try {
      const claimedEvent = await apiFetch('/api/events/join/claim', {
        method: 'POST',
        body: { inviteCode: inviteCode, participantName: participantName },
      });
      await loadEventById(claimedEvent.id);
    } catch (err) {
      showError(err);
    } finally {
      createBtn.disabled = false;
    }
  };

  const cancelBtn = el('button', { className: 'btn secondary', type: 'button' }, 'Отменить');
  cancelBtn.onclick = loadEvents;

  setView(
      makeContainer(
          makeCard(title, null, makeP(infoText, 'text'), list),
          makeCard('Создать новый слот', null, createForm),
          el('div', { className: 'actions' }, cancelBtn)
      )
  );
}

/* ----------------------------- Event details ----------------------------- */

async function loadEvent(event) {
  showLoading('Загружаю детали…');
  try {
    const [eventDetails, participants, expenses, balance] = await Promise.all([
      apiFetch(`/api/events/${event.id}`),
      apiFetch(`/api/events/${event.id}/participants`),
      apiFetch(`/api/events/${event.id}/expenses`),
      apiFetch(`/api/events/${event.id}/my-balance`),
    ]);
    renderEventDetails(eventDetails, participants, expenses, balance);
  } catch (e) {
    showError(e);
    await loadEvents();
  }
}

function renderEventDetails(event, participants, expenses, balance) {
  const participantNameById = new Map(participants.map((p) => [p.id, p.name]));

  const backBtn = el('button', { className: 'btn secondary', type: 'button' }, '← Назад');
  backBtn.onclick = loadEvents;

  const title = el('h1', { text: event.title });

  const deleteEventBtn = el('button', { className: 'btn secondary danger', type: 'button' }, 'Удалить событие');
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

  // Invite link card
  const inviteLink = buildInviteLink(event.inviteCode);
  const inviteInput = el('input', { type: 'text', value: inviteLink, readonly: 'true' });
  inviteInput.addEventListener('focus', () => inviteInput.select());

  const copyBtn = el('button', { className: 'btn secondary', type: 'button' }, 'Копировать');
  copyBtn.onclick = async () => {
    const ok = await copyToClipboard(inviteLink);
    notify(ok ? 'Ссылка скопирована в буфер обмена' : 'Не удалось скопировать ссылку');
  };

  const openBtn = el('a', { className: 'btn', href: inviteLink, target: '_blank', rel: 'noopener noreferrer' }, 'Открыть');

  const inviteCard = makeCard(
      'Ссылка-приглашение',
      'Отправьте её друзьям, чтобы они вошли в событие.',
      el('div', { className: 'form' }, inviteInput),
      el('div', { className: 'actions' }, openBtn, copyBtn)
  );

  // Participants
  const pList = el(
      'div',
      { className: 'stack' },
      ...participants.map((p) => {
        const name = el('div', { className: 'row__title' }, p.name, p.linked ? el('span', { className: 'badge' }, 'привязан') : null);

        const del = el('button', { className: 'btn secondary', type: 'button' }, 'Удалить');
        del.onclick = async () => {
          if (!confirm(`Удалить участника «${p.name}»?`)) return;
          del.disabled = true;
          try {
            await apiFetch(`/api/events/${event.id}/participants/${p.id}`, { method: 'DELETE' });
            // сохранено поведение как было: после удаления уходим в список
            await loadEvents();
          } catch (err) {
            showError(err);
          } finally {
            del.disabled = false;
          }
        };

        return makeRow(name, el('div', { className: 'actions actions--tight' }, del));
      })
  );

  const addParticipantName = el('input', { type: 'text', placeholder: 'Имя участника', required: 'true', autocomplete: 'off' });
  const addParticipantBtn = el('button', { className: 'btn', type: 'submit' }, 'Добавить');

  const addParticipantForm = el(
      'form',
      { className: 'form' },
      el('div', { className: 'form__row' }, addParticipantName),
      el('div', { className: 'actions' }, addParticipantBtn)
  );

  addParticipantForm.onsubmit = async (e) => {
    e.preventDefault();
    const n = addParticipantName.value.trim();
    if (!n) return;
    addParticipantBtn.disabled = true;
    try {
      await apiFetch(`/api/events/${event.id}/participants`, { method: 'POST', body: { name: n } });
      await loadEvent(event);
    } catch (err) {
      showError(err);
    } finally {
      addParticipantBtn.disabled = false;
    }
  };

  const participantsCard = makeCard(
      'Участники',
      null,
      participants.length ? pList : makeP('Пока нет участников.', 'text'),
      el('div', { className: 'divider' }),
      el('div', { className: 'card__sectionTitle', text: 'Добавить участника' }),
      addParticipantForm
  );

  // Expenses
  const expensesHeader = makeCard(
      'Расходы',
      expenses.length ? 'Нажмите “Детали”, чтобы посмотреть распределение.' : null
  );

  const expensesList =
      expenses.length > 0
          ? el(
              'div',
              { className: 'stack' },
              ...expenses.map((exp) => {
                const titleLine = el('div', { className: 'row__title' }, exp.title);

                const meta = el(
                    'div',
                    { className: 'row__meta' },
                    `— ${exp.amount} ${exp.currencyCode}, платил ${exp.payerName}`
                );

                const left = el('div', null, titleLine, meta);

                const detailsBtn = el('button', { className: 'btn secondary', type: 'button' }, 'Детали');
                const delBtn = el('button', { className: 'btn secondary', type: 'button' }, 'Удалить');

                const row = el('div', { className: 'row row--card' },
                    el('div', { className: 'row__left' }, left),
                    el('div', { className: 'row__right' }, el('div', { className: 'actions actions--tight' }, detailsBtn, delBtn))
                );

                detailsBtn.onclick = async () => {
                  const existing = row.querySelector('.expense-details');
                  if (existing) {
                    existing.remove();
                    return;
                  }
                  detailsBtn.disabled = true;
                  try {
                    const expenseDetails = await apiFetch(`/api/events/${event.id}/expenses/${exp.id}`);
                    renderExpenseDetails(row, expenseDetails, participantNameById);
                  } catch (err) {
                    showError(err);
                  } finally {
                    detailsBtn.disabled = false;
                  }
                };

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

                return row;
              })
          )
          : makeP('Пока что нет расходов.', 'text');

  // Add expense form (same data / endpoints)
  const tInput = el('input', { type: 'text', placeholder: 'Название расхода', required: 'true', autocomplete: 'off' });
  const amtInput = el('input', { type: 'number', step: '0.01', placeholder: 'Сумма', required: 'true', inputmode: 'decimal' });
  const currencyInput = el('input', { type: 'text', placeholder: 'Валюта (например, RUB)', value: 'RUB', required: 'true', autocomplete: 'off' });

  const dateInput = el('input', { type: 'date', required: 'true' });
  const today = new Date();
  dateInput.value = today.toISOString().split('T')[0];

  const payerSelect = el('select', { required: 'true' });
  participants.forEach((p) => payerSelect.appendChild(el('option', { value: p.id, text: p.name })));

  // Shares UI
  const sharesList = el('div', { className: 'shares' });
  const selectedParticipants = new Set(participants.map((p) => p.id));

  function collectShareDraft() {
    const draft = new Map();
    sharesList.querySelectorAll('input[data-field-type="amount"]').forEach((inp) => {
      const pid = Number(inp.dataset.participantId);
      const desc = sharesList.querySelector(`input[data-field-type="description"][data-participant-id="${pid}"]`);
      draft.set(pid, { amount: inp.value, description: desc ? desc.value : '' });
    });
    return draft;
  }

  function recalcShares() {
    const prev = collectShareDraft();
    sharesList.innerHTML = '';

    const amount = parseFloat(amtInput.value);
    const active = participants.filter((p) => selectedParticipants.has(p.id));
    const perShare = active.length > 0 && !isNaN(amount) ? amount / active.length : 0;

    participants.forEach((p) => {
      const checked = selectedParticipants.has(p.id);
      const row = el('div', { className: 'shareRow' });

      const checkbox = el('input', { type: 'checkbox' });
      checkbox.checked = checked;
      checkbox.onchange = () => {
        if (checkbox.checked) selectedParticipants.add(p.id);
        else selectedParticipants.delete(p.id);
        recalcShares();
      };

      const label = el('div', { className: 'shareRow__name', text: p.name });

      const amountInp = el('input', {
        type: 'number',
        step: '0.01',
        min: '0',
        placeholder: '0.00',
        'data-participant-id': p.id,
        'data-field-type': 'amount',
      });
      amountInp.disabled = !checked;

      const descInp = el('input', {
        type: 'text',
        placeholder: 'Комментарий',
        'data-participant-id': p.id,
        'data-field-type': 'description',
      });
      descInp.disabled = !checked;

      // Restore previous values if possible
      const old = prev.get(p.id);
      if (checked) {
        if (old?.amount) amountInp.value = old.amount;
        else if (perShare) amountInp.value = perShare.toFixed(2);
        if (old?.description) descInp.value = old.description;
      }

      row.appendChild(el('div', { className: 'shareRow__check' }, checkbox));
      row.appendChild(label);
      row.appendChild(el('div', { className: 'shareRow__amount' }, amountInp));
      row.appendChild(el('div', { className: 'shareRow__desc' }, descInp));

      sharesList.appendChild(row);
    });
  }

  amtInput.addEventListener('input', recalcShares);
  recalcShares();

  const addExpBtn = el('button', { className: 'btn', type: 'submit' }, 'Добавить расход');

  const addExpForm = el(
      'form',
      { className: 'form' },
      el('div', { className: 'grid2' }, tInput, amtInput),
      el('div', { className: 'grid2' }, currencyInput, dateInput),
      el('div', { className: 'form__row' }, el('label', { className: 'label', text: 'Плательщик' }), payerSelect),
      el('div', { className: 'divider' }),
      el('div', { className: 'card__sectionTitle', text: 'Доли участников' }),
      sharesList,
      el('div', { className: 'actions' }, addExpBtn)
  );

  addExpForm.onsubmit = async (e) => {
    e.preventDefault();
    const titleVal = tInput.value.trim();
    const amountVal = amtInput.value;
    const currencyVal = currencyInput.value.trim().toUpperCase();
    const dateVal = dateInput.value;
    const payerId = payerSelect.value;

    if (!titleVal || !amountVal || !currencyVal || !dateVal || !payerId) return;

    const shares = [];
    sharesList.querySelectorAll('input[data-field-type="amount"]').forEach((inp) => {
      if (inp.disabled) return;
      const amountValue = inp.value;
      if (!amountValue) return;

      const pid = parseInt(inp.dataset.participantId, 10);
      const desc = sharesList.querySelector(`input[data-field-type="description"][data-participant-id="${pid}"]`);
      shares.push({
        participantId: pid,
        amount: parseFloat(amountValue),
        description: desc ? desc.value.trim() : '',
      });
    });

    if (shares.length === 0) {
      notify('Выберите хотя бы одного участника для распределения траты.');
      return;
    }

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
          title: titleVal,
          amount: parseFloat(amountVal),
          currencyCode: currencyVal,
          expenseDate: dateVal,
          payerParticipantId: parseInt(payerId, 10),
          shares: shares,
        },
      });

      tInput.value = '';
      amtInput.value = '';
      recalcShares();
      await loadEvent(event);
    } catch (err) {
      showError(err);
    } finally {
      addExpBtn.disabled = false;
    }
  };

  const addExpenseCard = makeCard('Добавить расход', null, addExpForm);

  // Balance
  const youOwe = balance.youOwe || [];
  const oweYou = balance.oweYou || [];

  function renderBalanceList(items) {
    return el(
        'div',
        { className: 'stack' },
        ...items.map((e) =>
            el('div', { className: 'row row--card' },
                el('div', { className: 'row__left' },
                    el('div', { className: 'row__title', text: e.participantName }),
                    el('div', { className: 'row__meta', text: `${e.amount} ${e.currencyCode}` })
                )
            )
        )
    );
  }

  const balanceCard = makeCard(
      'Мой баланс',
      null,
      youOwe.length
          ? el('div', null, el('div', { className: 'card__sectionTitle', text: 'Вы должны' }), renderBalanceList(youOwe))
          : null,
      oweYou.length
          ? el('div', { className: youOwe.length ? 'mt' : '' }, el('div', { className: 'card__sectionTitle', text: 'Вам должны' }), renderBalanceList(oweYou))
          : null,
      youOwe.length === 0 && oweYou.length === 0 ? makeP('Баланс по этому событию нулевой.', 'text') : null
  );

  setView(
      makeContainer(
          el('div', { className: 'topbar' }, el('div', { className: 'topbar__left' }, backBtn), el('div', { className: 'topbar__right' }, deleteEventBtn)),
          title,
          inviteCard,
          participantsCard,
          expensesHeader,
          expensesList,
          addExpenseCard,
          balanceCard
      )
  );
}

function renderExpenseDetails(parentNode, expenseDetails, participantNameById = new Map()) {
  const existing = parentNode.querySelector('.expense-details');
  if (existing) {
    existing.remove();
    return;
  }

  const meta = el('div', { className: 'expense-details__meta' }, `Дата: ${expenseDetails.expenseDate}, плательщик: ${expenseDetails.payer.name}`);

  const list = el(
      'div',
      { className: 'expense-details__list' },
      ...expenseDetails.shares.map((share) => {
        const note = share.description ? ` (${share.description})` : '';
        const participantName = participantNameById.get(share.participantId) || `ID участника ${share.participantId}`;
        return el('div', { className: 'expense-details__item' }, `${participantName}: ${share.amount}${note}`);
      })
  );

  const details = el('div', { className: 'expense-details' }, meta, el('div', { className: 'expense-details__title', text: 'Разбивка долей' }), list);
  parentNode.appendChild(details);
}

/* ----------------------------- Start ----------------------------- */

document.addEventListener('DOMContentLoaded', () => {
  // If Telegram is present, we already called webApp.ready() above.
  initApp();
});
