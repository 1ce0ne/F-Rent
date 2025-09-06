document.addEventListener('DOMContentLoaded', function () {
    // Кнопка открытия диалога
    const addProductBtn = document.getElementById('add-product-btn');
    const addProductDialog = document.getElementById('add-product-dialog-form');
    const addProductForm = document.getElementById('add-product-form');

    if (addProductBtn && addProductDialog) {
        addProductBtn.addEventListener('click', () => {
            addProductDialog.showModal();
        });
    }

    if (addProductForm) {
        addProductForm.addEventListener('submit', function (e) {
            e.preventDefault();
            // Исправлено: используем FormData от формы, а не вручную
            const formData = new FormData(addProductForm);

            fetch('/api/add-product', {
                method: 'POST',
                body: formData
            })
                .then(res => res.json())
                .then(data => {
                    if (data.success) {
                        addProductDialog.close();
                        window.location.reload();
                    } else {
                        alert('Ошибка при добавлении товара: ' + (data.error || 'Неизвестная ошибка'));
                    }
                })
                .catch(() => alert('Ошибка при добавлении товара'));
        });
    }

    // --- Логика редактирования товара ---
    document.querySelectorAll('.edit-product-btn').forEach(btn => {
        btn.addEventListener('click', function () {
            const productId = this.dataset.id;
            fetch(`/api/products/${productId}`)
                .then(res => res.json())
                .then(data => {
                    document.getElementById('edit-product-id').value = data.id;
                    document.getElementById('edit-product-name').value = data.name || '';
                    document.getElementById('edit-product-category').value = data.category || '';
                    document.getElementById('edit-product-description').value = data.description || '';
                    document.getElementById('edit-product-price-hour').value = data.price_per_hour || 0;
                    document.getElementById('edit-product-price-day').value = data.price_per_day || 0;
                    document.getElementById('edit-product-price-month').value = data.price_per_month || 0;
                    // Размер
                    document.getElementById('edit-product-size').value = data.size || '';
                    document.getElementById('edit-product-dialog-form').showModal();
                });
        });
    });

    // Обработка формы редактирования товара
    const editProductForm = document.getElementById('edit-product-form');
    if (editProductForm) {
        editProductForm.addEventListener('submit', function (e) {
            e.preventDefault();
            const productId = document.getElementById('edit-product-id').value;
            const payload = {
                name: document.getElementById('edit-product-name').value,
                category: document.getElementById('edit-product-category').value,
                description: document.getElementById('edit-product-description').value,
                price_per_hour: document.getElementById('edit-product-price-hour').value,
                price_per_day: document.getElementById('edit-product-price-day').value,
                price_per_month: document.getElementById('edit-product-price-month').value,
                size: document.getElementById('edit-product-size').value
            };
            fetch(`/api/update-product/${productId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            })
                .then(res => res.json())
                .then(data => {
                    if (data.success) {
                        document.getElementById('edit-product-dialog-form').close();
                        window.location.reload();
                    } else {
                        alert('Ошибка при изменении товара: ' + (data.error || 'Неизвестная ошибка'));
                    }
                })
                .catch(() => alert('Ошибка при изменении товара'));
        });
    }

    // --- Логика удаления товара ---
    document.querySelectorAll('.delete-product-btn').forEach(btn => {
        btn.addEventListener('click', function () {
            const productId = this.dataset.id;
            if (!productId) return;
            if (confirm('Вы уверены, что хотите удалить этот товар? Это действие необратимо.')) {
                fetch(`/api/delete-product/${productId}`, {
                    method: 'DELETE'
                })
                    .then(res => res.json())
                    .then(data => {
                        if (data.success) {
                            window.location.reload();
                        } else {
                            alert('Ошибка при удалении товара: ' + (data.error || 'Неизвестная ошибка'));
                        }
                    })
                    .catch(() => alert('Ошибка при удалении товара'));
            }
        });
    });

    // --- Поиск по товару ---
    const searchInput = document.getElementById('search');
    if (searchInput) {
        searchInput.addEventListener('input', function () {
            const searchTerm = this.value.toLowerCase();
            document.querySelectorAll('.table-scroll .scroll-item').forEach(item => {
                const name = item.querySelector('#head1-conf1').textContent.toLowerCase();
                if (name.includes(searchTerm)) {
                    item.style.display = '';
                } else {
                    item.style.display = 'none';
                }
            });
        });
    }

    // --- Диалог товара (универсальный) ---
    let currentProductId = null;
    window.showProductDialog = function(productId) {
        currentProductId = productId;
        const dialog = document.getElementById('product-dialog');
        if (!dialog) return;

        // Загружаем данные о товаре
        fetch(`/api/products/${productId}`)
            .then(res => res.json())
            .then(data => {
                document.getElementById('product-name').textContent = data.name || '';
                document.getElementById('product-description').textContent = data.description || 'Описание отсутствует';
                document.getElementById('product-hour').textContent = (data.price_per_hour || 0) + '₽';
                document.getElementById('product-day').textContent = (data.price_per_day || 0) + '₽';
                document.getElementById('product-month').textContent = (data.price_per_month || 0) + '₽';
                document.getElementById('product-image').style.backgroundImage =
                    `url('/get-product-image/${productId}')`;
            });

        // Кнопка "Изменить товар"
        const editBtn = document.getElementById('edit-product-btn');
        if (editBtn) {
            editBtn.onclick = function() {
                fetch(`/api/products/${currentProductId}`)
                    .then(res => res.json())
                    .then(data => {
                        document.getElementById('edit-product-id').value = data.id;
                        document.getElementById('edit-product-name').value = data.name || '';
                        document.getElementById('edit-product-category').value = data.category || '';
                        document.getElementById('edit-product-description').value = data.description || '';
                        document.getElementById('edit-product-price-hour').value = data.price_per_hour || 0;
                        document.getElementById('edit-product-price-day').value = data.price_per_day || 0;
                        document.getElementById('edit-product-price-month').value = data.price_per_month || 0;
                        document.getElementById('edit-product-size').value = data.size || '';
                        document.getElementById('edit-product-dialog-form').showModal();
                    });
            };
        }

        // Кнопка "Удалить товар"
        const deleteBtn = document.getElementById('delete-product-btn');
        if (deleteBtn) {
            deleteBtn.onclick = function() {
                if (!currentProductId) return;
                if (confirm('Вы уверены, что хотите удалить этот товар? Это действие необратимо.')) {
                    fetch(`/api/delete-product/${currentProductId}`, {
                        method: 'DELETE'
                    })
                        .then(res => res.json())
                        .then(data => {
                            if (data.success) {
                                dialog.close();
                                window.location.reload();
                            } else {
                                alert('Ошибка при удалении товара: ' + (data.error || 'Неизвестная ошибка'));
                            }
                        })
                        .catch(() => alert('Ошибка при удалении товара'));
                }
            };
        }

        // Кнопка "История заказов"
        const historyBtn = document.getElementById('history-btn');
        if (historyBtn) {
            historyBtn.onclick = function() {
                showProductHistoryDialog(currentProductId);
            };
        }

        // Обработчик закрытия
        const closeBtn = dialog.querySelector('.closedio');
        if (closeBtn) {
            closeBtn.onclick = () => dialog.close();
        }

        dialog.showModal();
    };

    // --- Диалог истории заказов (универсальный) ---
    window.showProductHistoryDialog = function(productId) {
        const dialog = document.getElementById('product-history-dialog');
        const content = document.getElementById('product-history-content');
        if (!dialog || !content) return;

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
