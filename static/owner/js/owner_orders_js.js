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
    console.log('=== НАЧИНАЕМ ЗАГРУЗКУ ЗАКАЗОВ ===');
    fetch('/api/get-active-orders')
        .then(response => {
            console.log('Статус ответа сервера:', response.status);
            console.log('Тип содержимого:', response.headers.get('content-type'));

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            return response.json();
        })
        .then(orders => {
            console.log('=== ДАННЫЕ ПОЛУЧЕНЫ ===');
            console.log('Тип данных:', typeof orders);
            console.log('Количество заказов:', Array.isArray(orders) ? orders.length : 'не массив');
            console.log('Полные данные заказов:', JSON.stringify(orders, null, 2));

            const container = document.getElementById('orders-container');
            if (!container) {
                console.error('Контейнер orders-container не найден!');
                return;
            }

            renderOrders(container, orders);
        })
        .catch(error => {
            console.error('=== ОШИБКА ЗАГРУЗКИ ===');
            console.error('Тип ошибки:', error.constructor.name);
            console.error('Сообщение ошибки:', error.message);
            console.error('Полная ошибка:', error);

            const container = document.getElementById('orders-container');
            if (container) {
                container.innerHTML = `
                    <div style="text-align: center; color: red; padding: 20px;">
                        <h3>Ошибка загрузки заказов</h3>
                        <p><strong>Тип:</strong> ${error.constructor.name}</p>
                        <p><strong>Сообщение:</strong> ${error.message}</p>
                        <button onclick="refreshOrders()" style="margin-top: 10px;">Попробовать снова</button>
                    </div>
                `;
            }
        });
}

function renderOrders(container, orders) {
    console.log('=== НАЧИНАЕМ РЕНДЕРИНГ ===');
    console.log('Контейнер:', container);
    console.log('Заказы для рендеринга:', orders);

    // Проверяем, что orders является массивом
    if (!Array.isArray(orders)) {
        console.error('orders не является массивом:', typeof orders, orders);
        container.innerHTML = '<p style="text-align: center; color: red;">Ошибка: получены некорректные данные заказов</p>';
        return;
    }

    if (orders.length === 0) {
        console.log('Массив заказов пустой');
        container.innerHTML = '<p style="text-align: center;">Нет активных заказов</p>';
        return;
    }

    let html = '';
    let renderedCount = 0;

    orders.forEach((order, index) => {
        console.log(`=== ОБРАБАТЫВАЕМ ЗАКАЗ ${index + 1} ===`);
        console.log('Данные заказа:', JSON.stringify(order, null, 2));

        // Проверяем, что order является объектом
        if (!order || typeof order !== 'object') {
            console.error(`Заказ ${index} не является объектом:`, order);
            return;
        }

        // Проверяем наличие order_id
        if (!order.order_id) {
            console.error(`У заказа ${index} отсутствует order_id:`, order);
            return;
        }

        // Исправленная логика определения кнопок на основе статусов
        let buttonsHtml = '';

        console.log(`Заказ ${order.order_id}: issued=${order.issued}, ready_for_return=${order.ready_for_return}, returned=${order.returned}, not_issued=${order.not_issued}`);

        if (order.issued == 1) {
            // Заказ выдан - проверяем готовность к возврату
            if (order.ready_for_return == 1) {
                buttonsHtml = `<button class="more confs accept-btn" onclick="acceptReturn(${order.order_id})">Принял товар</button>`;
                console.log(`Заказ ${order.order_id}: кнопка "Принял товар"`);
            } else {
                buttonsHtml = `<button class="more confs status-btn" onclick="showOrderDetails(${order.order_id})">Статус</button>`;
                console.log(`Заказ ${order.order_id}: кнопка "Статус"`);
            }
        } else {
            // Заказ не выдан - показываем кнопки выдать/отказ
            buttonsHtml = `
                <button class="more confs issue-btn" onclick="issueOrder(${order.order_id})">Выдать</button>
                <button class="more confs red decline-btn" onclick="declineOrder(${order.order_id})">Отказ</button>
            `;
            console.log(`Заказ ${order.order_id}: кнопки "Выдать" и "Отказ"`);
        }

        const orderHtml = `
    <div class="scroll-item" data-order-id="${order.order_id}" data-returned="${order.returned}" data-issued="${order.issued || 0}">
        <p id="head1-conf1">${order.client_name || 'N/A'}</p>
        <p id="head2-conf1">${order.phone_number || 'N/A'}</p>
        <p id="head3-conf1">${order.product_name || 'N/A'}</p>
        <p id="head4-conf1">${order.address_office || 'N/A'}</p>
        <div class="order-actions">
            ${buttonsHtml}
        </div>
    </div>`;

        html += orderHtml;
        renderedCount++;
        console.log(`Заказ ${order.order_id} добавлен в HTML`);
    });

    console.log(`=== РЕНДЕРИНГ ЗАВЕРШЕН ===`);
    console.log(`Обработано заказов: ${renderedCount} из ${orders.length}`);
    console.log('Финальный HTML длина:', html.length);

    container.innerHTML = html;
    console.log('HTML установлен в контейнер');
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
