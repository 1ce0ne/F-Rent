let allReturns = [];

document.addEventListener('DOMContentLoaded', function () {
    fetchActiveReturns();
    setInterval(fetchActiveReturns, 100000);

    const searchInput = document.getElementById('search');
    if (searchInput) {
        searchInput.addEventListener('input', function () {
            filterReturns(this.value);
        });
    }

    // --- Диалог подтверждения возврата ---
    const acceptDialog = document.getElementById('accept-return-dialog');
    const acceptForm = document.getElementById('accept-return-form');
    const closeBtn = document.getElementById('close-accept-return-dialog');
    if (closeBtn) {
        closeBtn.onclick = () => acceptDialog.close();
    }
    if (acceptForm) {
        acceptForm.onsubmit = function (e) {
            e.preventDefault();
            const orderId = document.getElementById('accept-return-order-id').value;
            const haveProblem = document.getElementById('accept-return-have-problem').checked;
            const comment = document.getElementById('accept-return-comment').value;
            submitAcceptReturn(orderId, haveProblem, comment);
        };
    }
});

function fetchActiveReturns() {
    fetch('/api/get-active-office-returns')
        .then(response => {
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            return response.json();
        })
        .then(returns => {
            allReturns = returns;
            renderReturns(document.getElementById('returns-container'), returns);
        })
        .catch(error => {
            const container = document.getElementById('returns-container');
            container.innerHTML = `
                <p style="text-align: center; margin-top: 20px; color: red;">
                    Ошибка: ${error.message}
                </p>
                <p style="text-align: center; color: gray;">
                    Проверьте консоль для подробностей
                </p>
            `;
        });
}

function filterReturns(query) {
    query = (query || '').trim().toLowerCase();
    const filtered = !query
        ? allReturns
        : allReturns.filter(ret =>
            (ret.product_name || '').toLowerCase().includes(query)
        );
    renderReturns(document.getElementById('returns-container'), filtered);
}

function renderReturns(container, returns) {
    container.innerHTML = '';
    if (!returns || returns.length === 0) {
        container.innerHTML = `
            <p style="text-align: center; margin-top: 20px; width: 100%;">
                Активных возвратов нет
            </p>
        `;
        return;
    }
    returns.forEach(ret => {
        const returnElement = document.createElement('div');
        returnElement.className = 'scroll-item';
        returnElement.innerHTML = `
            <p id="head1-conf1">${ret.client_name || 'N/A'}</p>
            <p id="head2-conf1">${ret.phone_number || 'N/A'}</p>
            <p id="head3-conf1">${ret.product_name || 'N/A'}</p>
            <p id="head4-conf1">${ret.address_office || 'N/A'}</p>
            <div id="head6-conf1" class="actions">
                ${ret.accepted
                    ? '<span style="color:green;">Принят</span>'
                    : `<button class="more confs" onclick="openAcceptReturnDialog(${ret.order_id})">Принять</button>`
                }
            </div>
        `;
        container.appendChild(returnElement);
    });
}

// Открытие диалога подтверждения возврата
window.openAcceptReturnDialog = function (orderId) {
    document.getElementById('accept-return-order-id').value = orderId;
    document.getElementById('accept-return-have-problem').checked = false;
    document.getElementById('accept-return-comment').value = '';
    document.getElementById('accept-return-dialog').showModal();
};

// Отправка данных на API
function submitAcceptReturn(orderId, haveProblem, comment) {
    const formData = new FormData();
    formData.append('have_problem', haveProblem ? 'true' : '');
    formData.append('comment', comment);

    fetch(`/api/accept-office-return/${orderId}`, {
        method: 'POST',
        body: formData
    })
        .then(response => {
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            return response.json();
        })
        .then(data => {
            if (data.success) {
                alert('Успешно принято!');
                document.getElementById('accept-return-dialog').close();
                fetchActiveReturns();
            } else {
                alert(`Ошибка: ${data.error || 'Неизвестная ошибка'}`);
            }
        })
        .catch(error => {
            alert(`Ошибка: ${error.message}`);
        });
}
