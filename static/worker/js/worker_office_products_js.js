// junior_admin_products.js
document.addEventListener('DOMContentLoaded', function() {
    // Получаем URL плейсхолдера из скрытого элемента
    const placeholderImageUrl = document.getElementById('placeholder-image-url')?.textContent || '';

    // Элементы интерфейса
    const searchInput = document.getElementById('search');
    const reloadButton = document.querySelector('.reload');
    const productItems = document.querySelectorAll('.scroll-item');

    // Обработчик поиска
    if (searchInput) {
        searchInput.addEventListener('input', function() {
            const searchTerm = this.value.toLowerCase().trim();

            productItems.forEach(item => {
                const productName = item.querySelector('p[id^="head1-conf"]').textContent.toLowerCase();
                const productId = item.querySelector('p[id^="head3-conf"]').textContent.toLowerCase();

                // Проверка совпадения по названию или ID
                if (productName.includes(searchTerm) || productId.includes(searchTerm)) {
                    item.style.display = 'flex';
                } else {
                    item.style.display = 'none';
                }
            });
        });
    }

    // Кнопка перезагрузки
    if (reloadButton) {
        reloadButton.addEventListener('click', function() {
            searchInput.value = '';
            productItems.forEach(item => {
                item.style.display = 'flex';
            });
        });
    }

    // Функция загрузки изображения товара
    async function loadProductImage(productId) {
        try {
            const response = await fetch(`/api/get-product-image-url/${productId}`);
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            const data = await response.json();

            const imgElement = document.getElementById(`product-image-${productId}`);
            if (imgElement) {
                // Создаем временное изображение для проверки загрузки
                const tempImg = new Image();
                tempImg.onload = function() {
                    imgElement.style.backgroundImage = `url('${data.image_url}')`;
                };
                tempImg.onerror = function() {
                    console.error('Failed to load image:', data.image_url);
                    // Оставляем плейсхолдер в случае ошибки
                    imgElement.style.backgroundImage = `url('${placeholderImageUrl}')`;
                };
                tempImg.src = data.image_url;
            }
        } catch (error) {
            console.error('Error fetching product image:', error);
        }
    }

    // Обработчик для кнопок "Подробнее"
    document.querySelectorAll('.confs').forEach(button => {
        button.addEventListener('click', function(event) {
            event.preventDefault();

            // Получаем ID продукта из атрибута onclick
            const onclickContent = this.getAttribute('onclick');
            const productIdMatch = onclickContent.match(/showProductDialog\((\d+)\)/);

            if (productIdMatch && productIdMatch[1]) {
                const productId = productIdMatch[1];
                showProductDialog(productId);
            }
        });
    });

    // Функция показа диалога товара
    window.showProductDialog = function(productId) {
        const dialog = document.getElementById(`dialog-${productId}`);
        if (dialog) {
            dialog.showModal();
            // Загружаем изображение при открытии диалога
            loadProductImage(productId);
        }
    };

    // Закрытие диалога при клике на кнопку закрытия
    document.querySelectorAll('.closedio').forEach(button => {
        button.addEventListener('click', function() {
            const dialog = this.closest('dialog');
            if (dialog) {
                const productId = dialog.id.split('-')[1];
                const imgElement = document.getElementById(`product-image-${productId}`);
                if (imgElement && placeholderImageUrl) {
                    imgElement.style.backgroundImage = `url('${placeholderImageUrl}')`;
                }
                dialog.close();
            }
        });
    });

    // Закрытие диалога при клике вне области контента
    document.querySelectorAll('dialog').forEach(dialog => {
        dialog.addEventListener('click', function(event) {
            if (event.target === dialog) {
                const productId = dialog.id.split('-')[1];
                const imgElement = document.getElementById(`product-image-${productId}`);
                if (imgElement && placeholderImageUrl) {
                    imgElement.style.backgroundImage = `url('${placeholderImageUrl}')`;
                }
                dialog.close();
            }
        });
    });
});