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
});

function showProductDialog(productId) {
    const dialog = document.getElementById(`dialog-${productId}`);
    if (!dialog) return;

    // Загружаем данные о товаре
    fetch(`/api/products/${productId}`)
        .then(res => res.json())
        .then(data => {
            document.getElementById(`product-name-${productId}`).textContent = data.name || '';
            document.getElementById(`product-description-${productId}`).textContent = data.description || 'Описание отсутствует';
            document.getElementById(`product-hour-${productId}`).textContent = (data.price_per_hour || 0) + '₽';
            document.getElementById(`product-day-${productId}`).textContent = (data.price_per_day || 0) + '₽';
            document.getElementById(`product-month-${productId}`).textContent = (data.price_per_month || 0) + '₽';
            document.getElementById(`product-image-${productId}`).style.backgroundImage =
                `url('/get-product-image/${productId}')`;
        });

    dialog.showModal();

    // Закрытие диалога
    dialog.querySelector('.closedio').onclick = () => dialog.close();
}
