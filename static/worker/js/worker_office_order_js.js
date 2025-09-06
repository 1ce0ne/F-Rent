document.addEventListener('DOMContentLoaded', function () {
    // Загружаем заказы сразу при открытии страницы
    refreshOrders();

    // Устанавливаем периодическое обновление
    setInterval(refreshOrders, 15000);

    // Закрытие модального окна при клике на "x"
    document.querySelector('.close').addEventListener('click', function() {
        document.getElementById('orderDetailsModal').style.display = 'none';
    });

    // Закрытие модального окна при клике вне его области
    window.addEventListener('click', function(event) {
        const modal = document.getElementById('orderDetailsModal');
        if (event.target === modal) {
            modal.style.display = 'none';
        }
    });
});

function refreshOrders() {
    fetch('/api/get-active-orders')
        .then(response => {
            if (!response.ok) throw new Error('Network error');
            return response.json();
        })
        .then(orders => {
            const container = document.getElementById('orders-container');
            renderOrders(container, orders);
        })
        .catch(error => {
            console.error('Ошибка загрузки заказов:', error);
            document.getElementById('orders-container').innerHTML =
                '<p style="text-align: center; color: red;">Ошибка загрузки заказов</p>';
        });
}

function renderOrders(container, orders) {
    if (!orders || orders.length === 0) {
        container.innerHTML = '<p style="text-align: center;">Нет активных заказов</p>';
        return;
    }

    let html = '';
    orders.forEach(order => {
        html += `
    <div class="scroll-item" data-order-id="${order.order_id}" data-returned="${order.returned}" data-issued="${order.issued || 0}">
        <p id="head1-conf1">${order.client_name || 'N/A'}</p>
        <p id="head2-conf1">${order.phone_number || 'N/A'}</p>
        <p id="head3-conf1">${order.product_name || 'N/A'}</p>
        <p id="head4-conf1">${order.address_office || 'N/A'}</p>
        <div class="order-actions">
            ${order.issued == 1 ? 
            `<button class="more confs status-btn" onclick="showOrderDetails(${order.order_id})">Статус</button>` : 
            order.returned == 1 ?
            `<button class="more confs accept-btn" onclick="acceptReturn(${order.order_id})">Принял товар</button>` :
            `<button class="more confs issue-btn" onclick="issueOrder(${order.order_id})">Выдать</button>
             <button class="more confs red decline-btn" onclick="declineOrder(${order.order_id})">Отказ</button>`
            }
        </div>
    </div>`;
    });
    container.innerHTML = html;
}

// Функция для отображения деталей заказа
function showOrderDetails(orderId) {
    fetch(`/api/get-order-details/${orderId}`)
        .then(response => {
            if (!response.ok) throw new Error('Network error');
            return response.json();
        })
        .then(data => {
            // Форматирование времени заказа
            let orderTime = data.start_time || data.end_time || data.rental_time;
            let formattedTime = 'Нет данных';
            if (orderTime) {
                try {
                    const d = new Date(orderTime);
                    formattedTime = `${d.toLocaleDateString()} ${d.toLocaleTimeString()}`;
                } catch (e) {
                    formattedTime = orderTime;
                }
            }
            // Телефон клиента
            let phone = data.client_phone || 'Нет данных';

            // Заполнение модального окна данными
            let detailsHtml = `
                <div class="modal-info">
                    <p><strong>Товар:</strong> ${data.product_name || 'Нет данных'}</p>
                    <p><strong>Время заказа:</strong> ${formattedTime}</p>
                    <p><strong>Клиент:</strong> ${data.client_name || 'Нет данных'}</p>
                    <p><strong>Телефон:</strong> ${phone}</p>
                    <p><strong>Адрес офиса:</strong> ${data.address_office || 'Нет данных'}</p>
                    <p><strong>Статус:</strong> ${data.issued == 1 ? 'Выдан' : 'В обработке'}</p>
                </div>
            `;

            document.getElementById('orderDetailsContent').innerHTML = detailsHtml;
            document.getElementById('orderDetailsModal').style.display = 'block';
        })
        .catch(error => {
            console.error('Ошибка загрузки деталей заказа:', error);
            alert('Не удалось загрузить детали заказа. Пожалуйста, попробуйте позже.');
        });
}

// Функции для обработки действий
function issueOrder(orderId) {
    if (confirm('Вы уверены, что хотите выдать этот заказ?')) {
        fetch(`/api/issue-order/${orderId}`, {method: 'POST'})
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    refreshOrders();
                } else {
                    alert('Ошибка: ' + (data.error || 'Неизвестная ошибка'));
                }
            })
            .catch(() => alert('Ошибка сети'));
    }
}

function declineOrder(orderId) {
    if (confirm('Вы уверены, что хотите отказать в этом заказе?')) {
        fetch(`/api/decline-order/${orderId}`, {method: 'POST'})
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    refreshOrders();
                } else {
                    alert('Ошибка: ' + (data.error || 'Неизвестная ошибка'));
                }
            })
            .catch(() => alert('Ошибка сети'));
    }
}

function acceptReturn(orderId) {
    if (confirm('Вы подтверждаете прием товара от клиента?')) {
        fetch(`/api/accept-return/${orderId}`, {method: 'POST'})
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    refreshOrders();
                } else {
                    alert('Ошибка: ' + (data.error || 'Неизвестная ошибка'));
                }
            })
            .catch(() => alert('Ошибка сети'));
    }
}
