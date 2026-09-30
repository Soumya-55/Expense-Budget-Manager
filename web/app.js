const CUR = '₹';
const $ = s => document.querySelector(s);
const fmt = n => CUR + Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const todayStr = () => new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10);
let editId = null;

async function api(path, method = 'GET', data) {
  const opt = { method };
  if (data) {
    opt.headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
    opt.body = new URLSearchParams(data).toString();
  }
  const r = await fetch('/api/' + path, opt);
  const j = await r.json();
  if (!r.ok) throw new Error(j.error || 'Something went wrong');
  return j;
}

function toast(msg, isErr) {
  const t = $('#toast');
  t.textContent = msg;
  t.className = 'show' + (isErr ? ' err' : '');
  setTimeout(() => (t.className = ''), 2500);
}

async function refresh() {
  const m = $('#month').value;
  try {
    const [sum, txs, buds] = await Promise.all([
      api('summary?month=' + m),
      api('transactions?month=' + m + '&type=' + $('#fType').value),
      api('budgets?month=' + m),
    ]);
    renderSummary(sum); renderTx(txs); renderBudgets(buds);
  } catch (e) { toast(e.message, true); }
}

function renderSummary(s) {
  $('#sIncome').textContent = fmt(s.income);
  $('#sExpense').textContent = fmt(s.expense);
  $('#sBalance').textContent = fmt(s.balance);
  const max = Math.max(1, ...s.categories.map(c => c.total));
  $('#catBars').innerHTML = s.categories.length
    ? s.categories.map(c => `<div class="brow"><span>${esc(c.category)}</span><span>${fmt(c.total)}</span></div>
        <div class="bar"><i style="width:${(c.total / max) * 100}%"></i></div>`).join('')
    : '<div class="empty">No expenses this month.</div>';
}

function renderTx(list) {
  $('#txBody').innerHTML = list.length
    ? list.map(t => `<tr>
        <td>${esc(t.tx_date)}</td><td>${esc(t.category)}</td><td>${esc(t.description)}</td>
        <td class="${t.type === 'INCOME' ? 'inc' : 'exp'}">${t.type === 'INCOME' ? '+' : '-'}${fmt(t.amount)}</td>
        <td><button class="edit" data-edit='${JSON.stringify(t).replace(/'/g, '&#39;')}'>Edit</button>
            <button class="del" data-del="${t.id}">Delete</button></td></tr>`).join('')
    : '<tr><td colspan="5" class="empty">No transactions yet.</td></tr>';
}

function renderBudgets(list) {
  $('#budgets').innerHTML = list.length
    ? list.map(b => {
        const pct = (b.spent / b.budget) * 100;
        const cls = pct > 100 ? 'over' : pct > 80 ? 'warn' : '';
        return `<div class="brow"><span><b>${esc(b.category)}</b> <small>${fmt(b.spent)} / ${fmt(b.budget)}</small></span>
          <span>${Math.round(pct)}% <button class="del" data-delb="${b.id}">✕</button></span></div>
          <div class="bar ${cls}"><i style="width:${Math.min(pct, 100)}%"></i></div>`;
      }).join('')
    : '<div class="empty">No budgets set for this month.</div>';
}

function resetForm() {
  editId = null;
  $('#amount').value = ''; $('#category').value = ''; $('#description').value = '';
  $('#date').value = todayStr();
  document.querySelector('input[name=type][value=EXPENSE]').checked = true;
  $('#formTitle').textContent = 'Add Transaction';
  $('#cancelEdit').hidden = true;
}

$('#saveTx').onclick = async () => {
  const data = {
    type: document.querySelector('input[name=type]:checked').value,
    amount: $('#amount').value, category: $('#category').value,
    description: $('#description').value, date: $('#date').value,
  };
  try {
    if (editId) await api('transactions?id=' + editId, 'PUT', data);
    else await api('transactions', 'POST', data);
    toast(editId ? 'Transaction updated' : 'Transaction added');
    resetForm(); refresh();
  } catch (e) { toast(e.message, true); }
};
$('#cancelEdit').onclick = resetForm;

$('#saveBudget').onclick = async () => {
  try {
    await api('budgets', 'POST', { category: $('#bCategory').value, amount: $('#bAmount').value, month: $('#month').value });
    $('#bCategory').value = ''; $('#bAmount').value = '';
    toast('Budget saved'); refresh();
  } catch (e) { toast(e.message, true); }
};

document.addEventListener('click', async e => {
  const t = e.target;
  try {
    if (t.dataset.del) {
      if (!confirm('Delete this transaction?')) return;
      await api('transactions?id=' + t.dataset.del, 'DELETE'); toast('Deleted'); refresh();
    } else if (t.dataset.delb) {
      await api('budgets?id=' + t.dataset.delb, 'DELETE'); toast('Budget removed'); refresh();
    } else if (t.dataset.edit) {
      const x = JSON.parse(t.dataset.edit);
      editId = x.id;
      document.querySelector(`input[name=type][value=${x.type}]`).checked = true;
      $('#amount').value = x.amount; $('#category').value = x.category;
      $('#description').value = x.description || ''; $('#date').value = x.tx_date;
      $('#formTitle').textContent = 'Edit Transaction'; $('#cancelEdit').hidden = false;
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  } catch (err) { toast(err.message, true); }
});

$('#month').onchange = refresh;
$('#fType').onchange = refresh;
$('#month').value = todayStr().slice(0, 7);
resetForm();
refresh();
