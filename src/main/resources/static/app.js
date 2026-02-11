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

  const scheme = webApp?.colorScheme;
  if (scheme) root.dataset.scheme = scheme;
}

if (webApp) {
  try {
    applyTelegramTheme();
    if (typeof webApp.onEvent === 'function') webApp.onEvent('themeChanged', applyTelegramTheme);
    if (typeof webApp.ready === 'function') webApp.ready();
    if (typeof webApp.expand === 'function') webApp.expand();
  } catch (_) {}
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
  const head =
      title || subtitle
          ? el(
              'div',
              { className: 'card__head' },
              title ? el('div', { className: 'card__title', text: title }) : null,
              subtitle ? el('div', { className: 'card__subtitle', text: subtitle }) : null
          )
          : null;

  return el('div', { className: 'card' }, head, ...children);
}

function makeRow(left, right) {
  return el(
      'div',
      { className: 'row' },
      el('div', { className: 'row__left' }, left),
      el('div', { className: 'row__right' }, right)
  );
}

function pad(...children) {
  return el('div', { className: 'pad' }, ...children);
}

function showLoading(text = 'Загрузка...') {
  setView(
      makeContainer(
          el(
              'div',
              { className: 'loading' },
              el('div', { className: 'spinner', 'aria-hidden': 'true' }),
              el('div', { className: 'loading__text', text })
          )
      )
  );
}

function notify(message) {
  if (webApp && typeof webApp.showAlert === 'function') webApp.showAlert(message);
  else alert(message);
}

function showError(err) {
  const msg = err instanceof Error ? err.message : String(err);
  notify(msg);
  try { console.error(err); } catch (_) {}
}

async function copyToClipboard(text) {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text);
      return true;
    }
  } catch (_) {}
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

/* ----------------------------- Formatting ----------------------------- */

function formatAmount(n) {
  const x = Number(n);
  if (!isFinite(x)) return String(n);
  const isInt = Math.abs(x - Math.round(x)) < 1e-9;
  return isInt ? String(Math.round(x)) : x.toFixed(2);
}

function sumByCurrency(items) {
  const m = new Map();
  (items || []).forEach((it) => {
    const cur = (it.currencyCode || '').toUpperCase() || '???';
    const val = Number(it.amount);
    const prev = m.get(cur) || 0;
    m.set(cur, prev + (isFinite(val) ? val : 0));
  });
  return m;
}

function formatCurrencyMap(map) {
  if (!map || map.size === 0) return '0';
  const parts = [];
  for (const [cur, val] of map.entries()) {
    parts.push(`${formatAmount(val)} ${cur}`);
  }
  return parts.join(', ');
}

async function fetchEventDebtSummary(eventId) {
  try {
    const bal = await apiFetch(`/api/events/${eventId}/my-balance`);
    const owe = formatCurrencyMap(sumByCurrency(bal.youOwe));
    const owed = formatCurrencyMap(sumByCurrency(bal.oweYou));
    return { owe, owed };
  } catch (_) {
    return null;
  }
}

/* ----------------------------- App init & routing ----------------------------- */

async function initApp() {
  showLoading('Инициализация…');

  try {
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
  setView(makeContainer(makeCard('Ошибка', null, pad(el('p', { className: 'text', text })), pad(retryBtn))));
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
              pad(
                  el('p', {
                    className: 'text',
                    text:
                        'Вы ещё не завершили регистрацию в боте. Откройте бота SplittyCat в Telegram, завершите процесс регистрации и затем перезапустите мини-приложение.',
                  })
              ),
              pad(el('div', { className: 'actions actions--tight' }, openBot))
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
  const listContent =
      events.length > 0
          ? el(
              'div',
              { className: 'list' },
              ...events.map((ev) => {
                const meta = el('div', { className: 'list__meta', text: 'Вы должны: … · Вам должны: …' });

                const btn = el(
                    'button',
                    { className: 'list__item', type: 'button' },
                    el('div', { className: 'list__main' }, el('div', { className: 'list__title', text: ev.title }), meta),
                    el('div', { className: 'list__chev', 'aria-hidden': 'true' }, '›')
                );

                btn.onclick = () => loadEventById(ev.id);

                fetchEventDebtSummary(ev.id).then((sum) => {
                  if (!sum) return;
                  meta.textContent = `Вы должны: ${sum.owe} · Вам должны: ${sum.owed}`;
                });

                return btn;
              })
          )
          : pad(el('p', { className: 'text', text: 'Пока нет событий. Создайте первое событие ниже.' }));

  const listCard = makeCard('Мои события', null, listContent);

  const titleInput = el('input', {
    name: 'title',
    type: 'text',
    placeholder: 'Название события',
    required: 'true',
    autocomplete: 'off',
    inputmode: 'text',
  });

  const createBtn = el('button', { className: 'btn', type: 'submit' }, 'Создать');

  const createForm = el(
      'form',
      { id: 'create-event-form', className: 'form' },
      el('div', { className: 'form__row' }, titleInput),
      el('div', { className: 'actions actions--tight' }, createBtn)
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

  const createCard = makeCard('Создать событие', null, pad(createForm));

  setView(makeContainer(listCard, createCard));
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
      el('div', { className: 'actions actions--tight' }, createBtn)
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
          makeCard(title, null, pad(el('p', { className: 'text', text: infoText })), list),
          makeCard('Создать новый слот', null, pad(createForm)),
          pad(cancelBtn)
      )
  );
}

/* ----------------------------- Add expense page ----------------------------- */

function buildAddExpenseForm(event, participants, onCancel, onSuccess) {
  const tInput = el('input', { type: 'text', placeholder: 'Название', required: 'true', autocomplete: 'off' });
  const amtInput = el('input', { type: 'number', step: '0.01', placeholder: 'Сумма', required: 'true', inputmode: 'decimal' });
  const currencyInput = el('input', { type: 'text', placeholder: 'Валюта (например, RUB)', value: 'RUB', required: 'true', autocomplete: 'off' });

  const dateInput = el('input', { type: 'date', required: 'true' });
  dateInput.value = new Date().toISOString().split('T')[0];

  const payerSelect = el('select', { required: 'true' });
  participants.forEach((p) => payerSelect.appendChild(el('option', { value: p.id, text: p.name })));

  const sharesList = el('div', { className: 'shares' });
  const selectedParticipants = new Set(participants.map((p) => p.id));

  // что редактировали последним: общую сумму или доли
  let lastEditSource = null; // 'total' | 'share' | null

  function toCents(value) {
    if (value === null || value === undefined) return null;
    let s = String(value).trim();
    if (!s) return null;
    s = s.replace(',', '.');

    const m = s.match(/^(-?\d+)(?:\.(\d+))?$/);
    if (!m) {
      const n = Number(s);
      if (!isFinite(n)) return null;
      return Math.round(n * 100);
    }

    let whole = parseInt(m[1], 10);
    let frac = m[2] || '';

    // округление до 2 знаков
    let carry = 0;
    if (frac.length > 2) {
      const third = frac[2];
      frac = frac.slice(0, 2);
      if (third >= '5') {
        let fp = parseInt(frac || '0', 10) + 1;
        if (fp >= 100) {
          fp -= 100;
          carry = 1;
        }
        frac = String(fp).padStart(2, '0');
      }
    }

    frac = frac.padEnd(2, '0');
    const sign = whole < 0 ? -1 : 1;
    whole = Math.abs(whole) + carry;

    const cents = whole * 100 + parseInt(frac, 10);
    return sign * cents;
  }

  function centsToInput(cents) {
    if (!isFinite(cents)) return '';
    const abs = Math.abs(cents);
    const whole = Math.floor(abs / 100);
    const frac = abs % 100;
    const sign = cents < 0 ? '-' : '';
    // не показываем .00 если целое
    if (frac === 0) return `${sign}${whole}`;
    return `${sign}${whole}.${String(frac).padStart(2, '0')}`;
  }

  // распределение totalCents на count частей в центах
  // остаток (+1 цент) уходит к последним участникам (детерминированно)
  function distributeCents(totalCents, count) {
    if (count <= 0) return [];
    const base = Math.floor(totalCents / count);
    const rem = totalCents - base * count; // 0..count-1
    const out = new Array(count).fill(base);
    for (let i = 0; i < rem; i++) {
      out[count - 1 - i] += 1;
    }
    return out;
  }

  function collectDraft() {
    const draft = new Map();
    sharesList.querySelectorAll('input[data-field-type="amount"]').forEach((inp) => {
      const pid = Number(inp.dataset.participantId);
      const desc = sharesList.querySelector(
          `input[data-field-type="description"][data-participant-id="${pid}"]`
      );

      draft.set(pid, {
        amount: inp.value,
        amountDirty: inp.dataset.dirty === '1',
        description: desc ? desc.value : '',
        descDirty: desc ? desc.dataset.dirty === '1' : false,
      });
    });
    return draft;
  }

  function sumSharesCentsFromDOM() {
    let sum = 0;
    sharesList.querySelectorAll('input[data-field-type="amount"]').forEach((inp) => {
      if (inp.disabled) return;
      const c = toCents(inp.value);
      sum += c === null ? 0 : c;
    });
    return sum;
  }

  function updateTotalFromShares() {
    const total = sumSharesCentsFromDOM();
    amtInput.value = centsToInput(total);
  }

  function recalcShares() {
    const prev = collectDraft();
    sharesList.innerHTML = '';

    const active = participants.filter((p) => selectedParticipants.has(p.id));
    const totalCents = toCents(amtInput.value);

    // Если редактировали общую сумму — делаем авто-распределение (в центах) только для НЕ dirty полей.
    const assignedById = new Map();
    if (lastEditSource !== 'share' && totalCents !== null && active.length > 0) {
      const dirtyActive = active.filter((p) => prev.get(p.id)?.amountDirty);
      const nondirtyActive = active.filter((p) => !prev.get(p.id)?.amountDirty);

      const dirtySum = dirtyActive.reduce((acc, p) => {
        const c = toCents(prev.get(p.id)?.amount);
        return acc + (c === null ? 0 : c);
      }, 0);

      let remaining = totalCents - dirtySum;
      if (remaining < 0) remaining = 0;

      const dist = distributeCents(remaining, nondirtyActive.length);
      nondirtyActive.forEach((p, idx) => assignedById.set(p.id, dist[idx]));
    }

    participants.forEach((p) => {
      const checked = selectedParticipants.has(p.id);
      const row = el('div', { className: 'shareRow shareRow--stack' });

      const header = el('label', { className: 'shareRow__header shareRow__header--clickable' });

      const checkbox = el('input', { type: 'checkbox' });
      checkbox.checked = checked;

      checkbox.onchange = () => {
        if (checkbox.checked) selectedParticipants.add(p.id);
        else selectedParticipants.delete(p.id);

        recalcShares();

        // если мы “в режиме долей” — общая сумма должна следовать за долями
        if (lastEditSource === 'share') updateTotalFromShares();
      };

      header.appendChild(el('div', { className: 'shareRow__check' }, checkbox));
      header.appendChild(el('div', { className: 'shareRow__name', text: p.name }));
      row.appendChild(header);

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

      const old = prev.get(p.id);

      // восстановление description
      if (checked) {
        if (old?.description) descInp.value = old.description;
        if (old?.descDirty) descInp.dataset.dirty = '1';
      }

      // обработчики
      descInp.addEventListener('input', () => { descInp.dataset.dirty = '1'; });

      amountInp.addEventListener('input', () => {
        amountInp.dataset.dirty = '1';
        lastEditSource = 'share';
        updateTotalFromShares();
      });

      // восстановление/авто-назначение amount
      if (checked) {
        if (lastEditSource === 'share') {
          // режим “редактирую доли”: никого не перезаписываем, просто возвращаем прошлые значения
          if (old?.amount !== undefined && old.amount !== null && old.amount !== '') amountInp.value = old.amount;
          if (old?.amountDirty) amountInp.dataset.dirty = '1';
        } else {
          // режим “редактирую общую сумму”: dirty сохраняем, остальные — авто-распределяем
          if (old?.amountDirty) {
            if (old.amount) amountInp.value = old.amount;
            amountInp.dataset.dirty = '1';
          } else if (assignedById.has(p.id)) {
            amountInp.value = centsToInput(assignedById.get(p.id));
          } else if (old?.amount) {
            // когда общая сумма пустая/нечисло — просто восстанавливаем
            amountInp.value = old.amount;
          }
        }
      }

      row.appendChild(el('div', { className: 'shareRow__field' }, amountInp));
      row.appendChild(el('div', { className: 'shareRow__field' }, descInp));
      sharesList.appendChild(row);
    });
  }

  // Общая сумма → перераспределение
  amtInput.addEventListener('input', () => {
    lastEditSource = 'total';
    recalcShares();
  });

  // первый рендер
  recalcShares();

  const saveBtn = el('button', { className: 'btn', type: 'submit' }, 'Добавить расход');
  const cancelBtn = el('button', { className: 'btn secondary', type: 'button' }, 'Отмена');
  cancelBtn.onclick = onCancel;

  const form = el(
      'form',
      { className: 'form' },
      el('div', { className: 'grid2' }, tInput, amtInput),
      el('div', { className: 'grid2' }, currencyInput, dateInput),
      el('div', { className: 'form__row' }, el('label', { className: 'label', text: 'Кто заплатил' }), payerSelect),
      el('div', { className: 'divider' }),
      el('div', { className: 'card__sectionTitle inpad', text: 'Доли участников' }),
      sharesList,
      el('div', { className: 'actions actions--tight' }, cancelBtn, saveBtn)
  );

  form.onsubmit = async (e) => {
    e.preventDefault();

    const titleVal = tInput.value.trim();
    const currencyVal = currencyInput.value.trim().toUpperCase();
    const dateVal = dateInput.value;
    const payerId = payerSelect.value;

    if (!titleVal || !currencyVal || !dateVal || !payerId) return;

    const shares = [];
    let sumSharesCents = 0;

    sharesList.querySelectorAll('input[data-field-type="amount"]').forEach((inp) => {
      if (inp.disabled) return;

      const pid = parseInt(inp.dataset.participantId, 10);
      const desc = sharesList.querySelector(`input[data-field-type="description"][data-participant-id="${pid}"]`);

      const c = toCents(inp.value);
      const cents = c === null ? 0 : c;
      sumSharesCents += cents;

      shares.push({
        participantId: pid,
        amount: cents / 100,
        description: desc ? desc.value.trim() : '',
      });
    });

    if (shares.length === 0) {
      notify('Выберите хотя бы одного участника для распределения траты.');
      return;
    }

    // если общая сумма пустая — берём её из суммы долей
    let totalCents = toCents(amtInput.value);
    if (totalCents === null) {
      totalCents = sumSharesCents;
      amtInput.value = centsToInput(totalCents);
    }

    // сравнение в центах (без float-ошибок)
    if (Math.abs(sumSharesCents - totalCents) > 1) {
      notify('Сумма долей не равна общей сумме. Проверьте ввод.');
      return;
    }

    saveBtn.disabled = true;
    try {
      await apiFetch(`/api/events/${event.id}/expenses`, {
        method: 'POST',
        body: {
          title: titleVal,
          amount: totalCents / 100,
          currencyCode: currencyVal,
          expenseDate: dateVal,
          payerParticipantId: parseInt(payerId, 10),
          shares,
        },
      });

      await onSuccess?.();
    } catch (err) {
      showError(err);
    } finally {
      saveBtn.disabled = false;
    }
  };

  return form;
}

function renderAddExpensePage(event, participants) {
  const backBtn = el('button', { className: 'btn secondary', type: 'button' }, '← Назад');
  backBtn.onclick = () => loadEventById(event.id);

  const title = el('h1', { text: 'Добавить расход' });

  const form = buildAddExpenseForm(
      event,
      participants,
      () => loadEventById(event.id),
      async () => {
        // после успешного создания — вернуться в событие
        await loadEventById(event.id);
      }
  );

  setView(
      makeContainer(
          el('div', { className: 'topbar' }, el('div', { className: 'topbar__left' }, backBtn)),
          title,
          makeCard('Расход', null, pad(form))
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

  const title = el('h1', { text: event.title });

  // Invite link (copy only)
  const inviteLink = buildInviteLink(event.inviteCode);
  const inviteInput = el('input', { type: 'text', value: inviteLink, readonly: 'true' });
  inviteInput.addEventListener('focus', () => inviteInput.select());

  const copyBtn = el('button', { className: 'btn secondary', type: 'button' }, 'Копировать');
  copyBtn.onclick = async () => {
    const ok = await copyToClipboard(inviteLink);
    notify(ok ? 'Ссылка скопирована в буфер обмена' : 'Не удалось скопировать ссылку');
  };

  const inviteCard = makeCard(
      'Ссылка-приглашение',
      'Отправьте её друзьям, чтобы они присоединились к событию.',
      pad(inviteInput),
      pad(el('div', { className: 'actions actions--tight' }, copyBtn))
  );

  // Participants
  const pList =
      participants.length > 0
          ? el(
              'div',
              { className: 'stack' },
              ...participants.map((p) => {
                const left = el(
                    'div',
                    { className: 'row__title' },
                    p.name,
                    p.linked ? el('span', { className: 'badge' }, 'linked') : null
                );

                const del = el('button', { className: 'btn secondary danger', type: 'button' }, 'Удалить');
                del.onclick = async () => {
                  if (!confirm(`Удалить участника «${p.name}»?`)) return;
                  del.disabled = true;
                  try {
                    await apiFetch(`/api/events/${event.id}/participants/${p.id}`, { method: 'DELETE' });
                    await loadEvents();
                  } catch (err) {
                    showError(err);
                  } finally {
                    del.disabled = false;
                  }
                };

                return makeRow(el('div', null, left), el('div', { className: 'actions actions--tight' }, del));
              })
          )
          : pad(el('p', { className: 'text', text: 'Пока нет участников.' }));

  const addParticipantName = el('input', { type: 'text', placeholder: 'Имя участника', required: 'true', autocomplete: 'off' });
  const addParticipantBtn = el('button', { className: 'btn', type: 'submit' }, 'Добавить');

  const addParticipantForm = el(
      'form',
      { className: 'form form--compact' },
      el('div', { className: 'form__row' }, addParticipantName),
      el('div', { className: 'actions actions--tight' }, addParticipantBtn)
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
      pList,
      el('div', { className: 'divider' }),
      pad(el('div', { className: 'card__sectionTitle', text: 'Добавить участника' })),
      pad(addParticipantForm)
  );

  // Expenses (single card)
  const addExpenseBtn = el('button', { className: 'btn', type: 'button' }, 'Добавить расход');
  addExpenseBtn.onclick = () => renderAddExpensePage(event, participants);

  const expensesList =
      expenses.length > 0
          ? el(
              'div',
              { className: 'stack' },
              ...expenses.map((exp) => {
                const left = el(
                    'div',
                    null,
                    el('div', { className: 'row__title', text: exp.title }),
                    el('div', { className: 'row__meta', text: `${exp.amount} ${exp.currencyCode}` })

                );

                const detailsBtn = el('button', { className: 'btn secondary', type: 'button' }, 'Детали');
                const delBtn = el('button', { className: 'btn secondary danger', type: 'button' }, 'Удалить');

                const row = el(
                    'div',
                    { className: 'row row--card' },
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
          : pad(el('p', { className: 'text', text: 'Пока что нет расходов.' }));

  const expensesCard = makeCard(
      'Расходы',
      'Нажмите “Детали”, чтобы посмотреть распределение.',
      expensesList,
      pad(el('div', { className: 'actions actions--tight' }, addExpenseBtn))
  );

  // Debts
  const youOwe = balance.youOwe || [];
  const oweYou = balance.oweYou || [];

  function renderDebtList(items) {
    return el(
        'div',
        { className: 'stack' },
        ...items.map((e) =>
            el(
                'div',
                { className: 'row row--card' },
                el(
                    'div',
                    { className: 'row__left' },
                    el('div', { className: 'row__title', text: e.participantName }),
                    el('div', { className: 'row__meta', text: `${e.amount} ${e.currencyCode}` })
                )
            )
        )
    );
  }

  const debtsContent = el('div', null);

  if (youOwe.length > 0) {
    debtsContent.appendChild(pad(el('div', { className: 'card__sectionTitle', text: 'Вы должны' })));
    debtsContent.appendChild(renderDebtList(youOwe));
  }

  if (oweYou.length > 0) {
    debtsContent.appendChild(pad(el('div', { className: 'card__sectionTitle', text: 'Вам должны' })));
    debtsContent.appendChild(renderDebtList(oweYou));
  }

  if (youOwe.length === 0 && oweYou.length === 0) {
    debtsContent.appendChild(pad(el('p', { className: 'text', text: 'Нет долгов.' })));
  }

  const debtsCard = makeCard('Долги', null, debtsContent);

  setView(
      makeContainer(
          el('div', { className: 'topbar' }, el('div', { className: 'topbar__left' }, backBtn), el('div', { className: 'topbar__right' }, deleteEventBtn)),
          title,
          inviteCard,
          debtsCard,
          participantsCard,
          expensesCard
      )
  );
}

function renderExpenseDetails(parentNode, expenseDetails, participantNameById = new Map()) {
  const existing = parentNode.querySelector('.expense-details');
  if (existing) {
    existing.remove();
    return;
  }

  const meta = el(
      'div',
      { className: 'expense-details__meta' },
      el('div', { className: 'expense-details__metaRow', text: `Дата: ${expenseDetails.expenseDate}` }),
      el('div', { className: 'expense-details__metaRow', text: `Плательщик: ${expenseDetails.payer.name}` })
  );


  const list = el(
      'div',
      { className: 'expense-details__list' },
      ...expenseDetails.shares.map((share) => {
        const note = share.description ? ` (${share.description})` : '';
        const participantName = participantNameById.get(share.participantId) || `ID участника ${share.participantId}`;
        return el('div', { className: 'expense-details__item' }, `${participantName}: ${share.amount}${note}`);
      })
  );

  const details = el('div', { className: 'expense-details' }, meta, el('div', { className: 'expense-details__title', text: 'Доли' }), list);
  parentNode.appendChild(details);
}

/* ----------------------------- Start ----------------------------- */

document.addEventListener('DOMContentLoaded', () => {
  initApp();
});
