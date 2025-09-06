document.addEventListener('DOMContentLoaded', function () {
    // --- ОБЩАЯ ЛОГИКА ---
    // Безопасное закрытие для всех диалоговых окон
    document.querySelectorAll('dialog').forEach(dialog => {
        // Закрытие по клику на фон
        dialog.addEventListener('click', (e) => {
            if (e.target === dialog) {
                dialog.close();
            }
        });

        // Закрытие по кнопке "X"
        const closeButton = dialog.querySelector('.closedio');
        if (closeButton) {
            closeButton.addEventListener('click', (e) => {
                e.stopPropagation(); // Предотвращаем всплытие события
                dialog.close();
            });
        }
    });

    // --- ЛОГИКА ДЛЯ СТРАНИЦЫ "ОФИСЫ" (/admin-junior/offices) ---
    const officesContainer = document.querySelector('.postamts');
    if (officesContainer) {
        // 1. Обработка кнопок "Подробнее" для каждого офиса
        document.querySelectorAll('.scroll-item .more').forEach(button => {
            button.addEventListener('click', function () {
                const officeId = this.id.split('-')[1];
                if (officeId) {
                    toggleOfficeDetails(officeId);
                }
            });
        });

        function toggleOfficeDetails(officeId) {
            const detailsElement = document.getElementById(`office-${officeId}`);
            const cellsContainer = document.getElementById(`cells-${officeId}`);
            if (!detailsElement || !cellsContainer) return;

            const isHidden = detailsElement.style.display === 'none' || detailsElement.style.display === '';

            if (isHidden) {
                // Загружаем ячейки
                fetch(`/api/get-office-cells/${officeId}`)
                    .then(response => response.json())
                    .then(data => {
                        cellsContainer.innerHTML = ''; // Очистка
                        // Создаем 30 ячеек
                        for (let i = 1; i <= 30; i++) {
                            const cellButton = document.createElement('button');
                            cellButton.className = 'cellb';
                            cellButton.textContent = i;
                            const cellData = data.cells.find(c => c.cell_id === i);
                            if (cellData && cellData.product_id) {
                                cellButton.classList.add('has-product');
                            }
                            cellButton.addEventListener('click', (e) => {
                                e.stopPropagation(); // Предотвращаем всплытие
                                showCellInfo(officeId, i);
                            });
                            cellsContainer.appendChild(cellButton);
                        }
                    })
                    .catch(error => console.error('Ошибка при загрузке ячеек:', error));

                detailsElement.style.display = 'flex';
            } else {
                detailsElement.style.display = 'none';
            }
        }

        // --- Состояние для диалогов ---
        let currentOfficeId = null;
        let currentCellId = null;

        // --- Диалог выбора товара ---
        function showAddProductDialog() {
            const productDialog = document.getElementById('add-product-dialog');
            if (!productDialog) return;

            // Загрузка товаров
            loadProducts('');

            // Поиск по названию
            document.getElementById('search-btn').onclick = () => {
                const searchTerm = document.getElementById('product-search').value;
                loadProducts(searchTerm);
            };
            document.getElementById('product-search').addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    const searchTerm = document.getElementById('product-search').value;
                    loadProducts(searchTerm);
                }
            });

            productDialog.showModal();
        }

        function loadProducts(searchTerm = '') {
            fetch('/api/products')
                .then(response => response.json())
                .then(products => {
                    const resultsContainer = document.getElementById('product-search-results');
                    resultsContainer.innerHTML = '';

                    const filtered = products.filter(product => {
                        const s = searchTerm.toLowerCase();
                        return product.name.toLowerCase().includes(s) ||
                            product.id.toString().includes(searchTerm) ||
                            product.category.toLowerCase().includes(s);
                    });

                    if (filtered.length === 0) {
                        resultsContainer.innerHTML = '<p class="no-results">Товары не найдены</p>';
                        return;
                    }

                    filtered.forEach(product => {
                        const row = document.createElement('div');
                        row.className = 'product-row';
                        row.innerHTML = `
                            <p>${product.id}</p>
                            <p>${product.name}</p>
                            <p>${product.category}</p>
                            <p>${product.price_per_hour}₽</p>
                            <button class="select-product" data-id="${product.id}">Выбрать</button>
                        `;
                        resultsContainer.appendChild(row);
                    });

                    // Навешиваем обработчики на кнопки "Выбрать"
                    resultsContainer.querySelectorAll('.select-product').forEach(btn => {
                        btn.addEventListener('click', function () {
                            const productId = this.getAttribute('data-id');
                            addProductToCell(productId);
                        });
                    });
                });
        }

        function addProductToCell(productId) {
            if (!currentOfficeId || !currentCellId) return;
            fetch(`/api/update-cell/${currentOfficeId}/${currentCellId}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ product_id: productId })
            })
                .then(response => response.json())
                .then(() => {
                    document.getElementById('add-product-dialog').close();
                    document.getElementById('cell-dialog').close();
                    toggleOfficeDetails(currentOfficeId);
                });
        }

        // --- Диалог ячейки ---
        function showCellInfo(officeId, cellId) {
            const cellDialog = document.getElementById('cell-dialog');
            if (!cellDialog) return;

            currentOfficeId = officeId;
            currentCellId = cellId;

            const productInfoEl = cellDialog.querySelector('#product-info');
            const emptyCellEl = cellDialog.querySelector('#empty-cell');
            const titleEl = cellDialog.querySelector('#cell-title');

            if (titleEl) titleEl.textContent = `Ячейка ${cellId} (Офис ${officeId})`;

            fetch(`/api/get-cell-info/${officeId}/${cellId}`)
                .then(response => response.json())
                .then(data => {
                    // Сброс обработчиков
                    const addBtn = cellDialog.querySelector('#add-product-btn');
                    const delBtn = cellDialog.querySelector('#remove-product-btn');

                    if (addBtn) addBtn.onclick = null;
                    if (delBtn) delBtn.onclick = null;

                    if (data.empty) {
                        if (productInfoEl) productInfoEl.style.display = 'none';
                        if (emptyCellEl) emptyCellEl.style.display = 'block';

                        // --- КНОПКА ДОБАВИТЬ ТОВАР ---
                        if (addBtn) {
                            addBtn.onclick = function () {
                                showAddProductDialog();
                            };
                        }
                    } else {
                        if (productInfoEl) {
                            productInfoEl.style.display = 'block';
                            const nameEl = productInfoEl.querySelector('#cell-product-name');
                            const descEl = productInfoEl.querySelector('#cell-product-description');
                            const priceHourEl = productInfoEl.querySelector('#cell-price-hour');
                            const priceDayEl = productInfoEl.querySelector('#cell-price-day');
                            const priceMonthEl = productInfoEl.querySelector('#cell-price-month');
                            const imgEl = productInfoEl.querySelector('#cell-product-image');

                            if (nameEl) nameEl.textContent = data.product.name;
                            if (descEl) descEl.textContent = data.product.description || 'Нет описания';
                            if (priceHourEl) priceHourEl.textContent = `${data.product.price_per_hour}₽`;
                            if (priceDayEl) priceDayEl.textContent = `${data.product.price_per_day}₽`;
                            if (priceMonthEl) priceMonthEl.textContent = `${data.product.price_per_month}₽`;
                            if (imgEl) imgEl.style.backgroundImage = `url('${data.product.image_url}')`;

                            // --- КНОПКИ ДЕЙСТВИЙ ---
                            // Изменить товар
                            const editBtn = productInfoEl.querySelector('#cell-edit-product-btn');
                            if (editBtn) {
                                editBtn.onclick = function () {
                                    fetch(`/api/products/${data.product.id}`)
                                        .then(res => res.json())
                                        .then(prod => {
                                            document.getElementById('edit-product-id').value = prod.id;
                                            document.getElementById('edit-product-name').value = prod.name || '';
                                            document.getElementById('edit-product-category').value = prod.category || '';
                                            document.getElementById('edit-product-description').value = prod.description || '';
                                            document.getElementById('edit-product-price-hour').value = prod.price_per_hour || 0;
                                            document.getElementById('edit-product-price-day').value = prod.price_per_day || 0;
                                            document.getElementById('edit-product-price-month').value = prod.price_per_month || 0;
                                            document.getElementById('edit-product-size').value = prod.size || '';
                                            document.getElementById('edit-product-dialog-form').showModal();
                                        });
                                };
                            }

                            // История заказов
                            const historyBtn = productInfoEl.querySelector('#cell-history-btn');
                            if (historyBtn) {
                                // Снимаем старый обработчик
                                historyBtn.onclick = null;
                                // Назначаем новый обработчик
                                historyBtn.onclick = function () {
                                    showProductHistoryDialog(data.product.id);
                                };
                            }

                            // Убрать товар (удаление)
                            const delBtn = productInfoEl.querySelector('#remove-product-btn');
                            if (delBtn) {
                                delBtn.onclick = function () {
                                    if (confirm('Удалить товар из ячейки?')) {
                                        fetch(`/api/update-cell/${officeId}/${cellId}`, {
                                            method: 'POST',
                                            headers: { 'Content-Type': 'application/json' },
                                            body: JSON.stringify({ product_id: null })
                                        })
                                            .then(res => res.json())
                                            .then(() => {
                                                cellDialog.close();
                                                toggleOfficeDetails(officeId);
                                            });
                                    }
                                };
                            }
                        }
                        if (emptyCellEl) emptyCellEl.style.display = 'none';
                    }
                    cellDialog.showModal();
                });
        }

        // 3. Логика поиска
        const searchInput = document.getElementById('search');
        if (searchInput) {
            searchInput.addEventListener('input', function () {
                const searchTerm = this.value.toLowerCase();
                document.querySelectorAll('.table-scroll .scroll-item').forEach(item => {
                    const address = item.querySelector('#head1').textContent.toLowerCase();
                    const officeId = item.querySelector('.more').id.split('-')[1];
                    const officeDetails = document.getElementById(`office-${officeId}`);

                    if (address.includes(searchTerm)) {
                        item.style.display = '';
                        if (officeDetails) officeDetails.style.display = 'none';
                    } else {
                        item.style.display = 'none';
                        if (officeDetails) officeDetails.style.display = 'none';
                    }
                });
            });
        }

        // 4. Логика добавления нового офиса
        const addOfficeBtn = document.getElementById('add-office-btn');
        const addOfficeDialog = document.getElementById('add-office-dialog');
        const addOfficeForm = document.getElementById('add-office-form');

        if (addOfficeBtn && addOfficeDialog) {
            addOfficeBtn.addEventListener('click', () => {
                addOfficeDialog.showModal();
            });

            if (addOfficeForm) {
                addOfficeForm.addEventListener('submit', function (e) {
                    e.preventDefault();
                    // Получаем значения из формы
                    const address = document.getElementById('new-office-address').value;
                    const latitude = document.getElementById('new-office-latitude').value;
                    const longitude = document.getElementById('new-office-longitude').value;

                    // Отправка данных на сервер
                    fetch('/api/add-office', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        body: JSON.stringify({
                            address: address,
                            latitude: latitude,
                            longitude: longitude
                        })
                    })
                        .then(response => response.json())
                        .then(data => {
                            if (data.success) {
                                // Обновляем страницу после успешного добавления
                                window.location.reload();
                            } else {
                                alert('Ошибка при добавлении офиса: ' + (data.message || 'Неизвестная ошибка'));
                            }
                        })
                        .catch(error => {
                            console.error('Ошибка:', error);
                            alert('Произошла ошибка при отправке данных');
                        });
                });
            }
        }

        // --- ДОБАВЛЕНО: Логика удаления офиса ---
        document.querySelectorAll('.delete-office-btn').forEach(btn => {
            btn.addEventListener('click', function () {
                const officeId = this.dataset.id;
                if (!officeId) return;
                if (confirm('Вы уверены, что хотите удалить этот офис? Это действие необратимо.')) {
                    fetch(`/api/delete-office/${officeId}`, {
                        method: 'DELETE'
                    })
                        .then(res => res.json())
                        .then(data => {
                            if (data.success) {
                                window.location.reload();
                            } else {
                                alert('Ошибка при удалении офиса: ' + (data.error || 'Неизвестная ошибка'));
                            }
                        })
                        .catch(() => alert('Ошибка при удалении офиса'));
                }
            });
        });

        // --- ДОБАВЛЕНО: Логика изменения офиса ---
        // Открытие диалога
        document.querySelectorAll('.edit-office-btn').forEach(btn => {
            btn.addEventListener('click', function () {
                const officeId = this.dataset.id;
                if (!officeId) return;
                // Получаем текущие значения
                const item = btn.closest('.office-details-layout').parentElement.previousElementSibling;
                const address = item.querySelector('#head1').textContent.trim();
                const latitude = item.querySelector('#head2').textContent.trim();
                const longitude = item.querySelector('#head3').textContent.trim();

                // Заполняем форму
                document.getElementById('edit-office-id').value = officeId;
                document.getElementById('edit-office-address').value = address;
                document.getElementById('edit-office-latitude').value = latitude;
                document.getElementById('edit-office-longitude').value = longitude;

                document.getElementById('edit-office-dialog').showModal();
            });
        });

        // Обработка формы изменения
        const editOfficeForm = document.getElementById('edit-office-form');
        if (editOfficeForm) {
            editOfficeForm.addEventListener('submit', function (e) {
                e.preventDefault();
                const officeId = document.getElementById('edit-office-id').value;
                const address = document.getElementById('edit-office-address').value;
                const latitude = document.getElementById('edit-office-latitude').value;
                const longitude = document.getElementById('edit-office-longitude').value;
                fetch(`/api/update-office/${officeId}`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        address: address,
                        latitude: latitude,
                        longitude: longitude
                    })
                })
                    .then(res => res.json())
                    .then(data => {
                        if (data.success) {
                            window.location.reload();
                        } else {
                            alert('Ошибка при изменении офиса: ' + (data.error || 'Неизвестная ошибка'));
                        }
                    })
                    .catch(() => alert('Ошибка при изменении офиса'));
            });
        }
    }

    // --- Диалог истории заказов (универсальный, как в товарах) ---
    window.showProductHistoryDialog = function(productId) {
        let dialog = document.getElementById('product-history-dialog');
        let content = document.getElementById('product-history-content');
        if (!dialog || !content) {
            // Если нет диалога, создаём его динамически (или добавьте в шаблон)
            return;
        }

        content.innerHTML = '<div class="loading-history" style="text-align:center; margin: 30px 0;">Загрузка...</div>';

        fetch(`/api/get-product-history/${productId}`)
            .then(res => res.json())
            .then(data => {
                if (data.error) {
                    content.innerHTML = `<div class="error-history">${data.error}</div>`;
                    return;
                }
                const orders = data.orders_history || [];
                if (orders.length === 0) {
                    content.innerHTML = '<div style="text-align:center; margin: 30px 0;">Нет заказов для этого товара</div>';
                    return;
                }
                let html = '';
                orders.forEach(order => {
                    html += `
                    <div class="order-history-block">
                        <div style="font-weight:bold; margin-bottom:6px;">
                            Заказ #${order.order_id} (${order.order_type_text})
                            <span style="color:#888; font-size:0.95em;">${order.status.text}</span>
                        </div>
                        <div style="margin-bottom:6px;">
                            <b>Клиент:</b> ${order.client_info ? (order.client_info.name || order.client_info.phone_number) : '—'}
                        </div>
                        <div style="margin-bottom:6px;">
                            <b>Период:</b> ${order.timeline.start_time || '-'} — ${order.timeline.end_time || '-'}
                        </div>
                        <div style="margin-bottom:6px;">
                            <b>Сумма аренды:</b> ${order.rental_details.rental_amount || 0}₽
                            <b>Время аренды:</b> ${order.rental_details.rental_time_hours || 0} ч
                        </div>
                        <div style="margin-bottom:6px;">
                            <b>Комментарий:</b> ${order.comments || '—'}
                        </div>
                        <div style="margin-bottom:6px;">
                            <b>Фото:</b>
                            <div class="photos-block">
                                ${order.photos && order.photos.length > 0 ? order.photos.map(photo => `
                                    <div>
                                        <img src="${photo.photo_url}" alt="Фото">
                                        <div>${photo.photo_type_text}</div>
                                    </div>
                                `).join('') : '<span style="color:#888;">Нет фото</span>'}
                            </div>
                        </div>
                    </div>
                    `;
                });
                content.innerHTML = html;
            })
            .catch(() => {
                content.innerHTML = '<div class="error-history">Ошибка загрузки истории заказов</div>';
            });

        // Обработчик закрытия
        const closeBtn = dialog.querySelector('.closedio');
        if (closeBtn) {
            closeBtn.onclick = () => dialog.close();
        }

        dialog.showModal();
    };
});
