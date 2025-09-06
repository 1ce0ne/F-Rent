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
                    : `<button class="more confs" id="accept-btn" onclick="acceptReturn(${ret.order_id})">Принять</button>`
                }
            </div>
        `;
        container.appendChild(returnElement);
    });
}

window.acceptReturn = function (returnId) {
    if (!confirm(`Принять возврат #${returnId}?`)) return;

    fetch(`/api/accept-office-return/${returnId}`, {
        method: 'POST'
    })
        .then(response => {
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            return response.json();
        })
        .then(data => {
            if (data.success) {
                alert('Успешно принято!');
                fetchActiveReturns();
            } else {
                alert(`Ошибка: ${data.error || 'Неизвестная ошибка'}`);
            }
        })
        .catch(error => {
            alert(`Ошибка: ${error.message}`);
        });
};
