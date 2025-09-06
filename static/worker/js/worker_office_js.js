document.addEventListener('DOMContentLoaded', function() {
    // Состояние открытых офисов
    const openOffices = JSON.parse(sessionStorage.getItem('openOffices')) || [];
    let currentOfficeId = null;
    let currentCellId = null;

    // Сохранение состояния в sessionStorage
    function saveOpenOffices() {
        sessionStorage.setItem('openOffices', JSON.stringify(openOffices));
    }

    // Общие функции для диалогов
    document.querySelectorAll('dialog').forEach(dialog => {
        dialog.addEventListener('click', (e) => {
            if (e.target === dialog) dialog.close();
        });

        const closeButton = dialog.querySelector('.closedio');
        if (closeButton) {
            closeButton.addEventListener('click', () => dialog.close());
        }
    });

    // Функция для обновления текста кнопки
    function updateButtonText(officeId, isOpen) {
        const button = document.getElementById(`more-${officeId}`);
        if (button) {
            button.textContent = isOpen ? 'Свернуть' : 'Подробнее';
        }
    }

    // Функция переключения состояния офиса
    function toggleOfficeDetails(officeId) {
        const detailsElement = document.getElementById(`office-${officeId}`);
        const cellsContainer = document.getElementById(`cells-${officeId}`);
        if (!detailsElement || !cellsContainer) return;

        const isOpen = !detailsElement.style.display || detailsElement.style.display === 'none';

        if (isOpen) {
            // Загрузка ячеек только при первом открытии
            if (cellsContainer.children.length === 0) {
                fetch(`/api/get-office-cells/${officeId}`)
                    .then(response => response.json())
                    .then(data => {
                        cellsContainer.innerHTML = '';
                        for (let i = 1; i <= 30; i++) {
                            const cellButton = document.createElement('button');
                            cellButton.className = 'cellb';
                            cellButton.textContent = i;
                            const cellData = data.cells.find(c => c.cell_id === i);
                            if (cellData && cellData.product_id) {
                                cellButton.classList.add('has-product');
                            }
                            cellButton.addEventListener('click', (e) => {
                                e.stopPropagation();
                                currentOfficeId = officeId;
                                currentCellId = i;
                                showCellInfo(officeId, i);
                            });
                            cellsContainer.appendChild(cellButton);
                        }
                    })
                    .catch(console.error);
            }
            detailsElement.style.display = 'flex';

            // Сохраняем состояние
            if (!openOffices.includes(officeId)) {
                openOffices.push(officeId);
                saveOpenOffices();
            }
        } else {
            detailsElement.style.display = 'none';

            // Удаляем из сохраненных состояний
            const index = openOffices.indexOf(officeId);
            if (index !== -1) {
                openOffices.splice(index, 1);
                saveOpenOffices();
            }
        }

        updateButtonText(officeId, isOpen);
    }

    // Восстановление открытых офисов при загрузке
    openOffices.forEach(officeId => {
        const detailsElement = document.getElementById(`office-${officeId}`);
        if (detailsElement) {
            detailsElement.style.display = 'flex';
            updateButtonText(officeId, true);
        }
    });

    // Обработчики для кнопок "Подробнее"
    document.querySelectorAll('.scroll-item .more').forEach(button => {
        button.addEventListener('click', function() {
            const officeId = this.id.split('-')[1];
            toggleOfficeDetails(officeId);
        });
    });

    // Функция показа информации о ячейке
    function showCellInfo(officeId, cellId) {
        const cellDialog = document.getElementById('cell-dialog');
        if (!cellDialog) return;

        const titleEl = cellDialog.querySelector('#cell-title');
        if (titleEl) titleEl.textContent = `Ячейка ${cellId} (Офис ${officeId})`;

        fetch(`/api/get-cell-info/${officeId}/${cellId}`)
            .then(response => response.json())
            .then(data => {
                const productInfo = document.getElementById('product-info');
                const emptyCell = document.getElementById('empty-cell');

                if (data.empty) {
                    productInfo.style.display = 'none';
                    emptyCell.style.display = 'flex';

                    // Обработчик добавления товара
                    document.getElementById('add-product-btn').onclick = () => {
                        currentOfficeId = officeId;
                        currentCellId = cellId;
                        showAddProductDialog();
                    };
                } else {
                    productInfo.style.display = 'flex';
                    emptyCell.style.display = 'none';

                    // Заполнение данных о товаре
                    document.getElementById('cell-product-name').textContent = data.product.name;
                    document.getElementById('cell-product-description').textContent =
                        data.product.description || 'Нет описания';
                    document.getElementById('cell-price-hour').textContent =
                        `${data.product.price_per_hour}₽`;
                    document.getElementById('cell-price-day').textContent =
                        `${data.product.price_per_day}₽`;
                    document.getElementById('cell-price-month').textContent =
                        `${data.product.price_per_month}₽`;

                    const imgEl = document.getElementById('cell-product-image');
                    if (imgEl) {
                        imgEl.style.backgroundImage = `url('${data.product.image_url}')`;
                    }

                    // Обработчик удаления товара
                    document.getElementById('remove-product-btn').onclick = () => {
                        fetch(`/api/update-cell/${officeId}/${cellId}`, {
                            method: 'POST',
                            headers: {'Content-Type': 'application/json'},
                            body: JSON.stringify({product_id: null})
                        })
                        .then(response => response.json())
                        .then(() => {
                            cellDialog.close();
                            toggleOfficeDetails(officeId);
                        });
                    };
                }
                cellDialog.showModal();
            })
            .catch(console.error);
    }

    // Функция показа диалога добавления товара
    function showAddProductDialog() {
        const productDialog = document.getElementById('add-product-dialog');
        if (!productDialog) return;

        // Загрузка всех товаров при открытии диалога
        loadProducts('');

        // Обработчик поиска
        document.getElementById('search-btn').onclick = () => {
            const searchTerm = document.getElementById('product-search').value;
            loadProducts(searchTerm);
        };

        // Обработчик поиска при нажатии Enter
        document.getElementById('product-search').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                const searchTerm = document.getElementById('product-search').value;
                loadProducts(searchTerm);
            }
        });

        productDialog.showModal();
    }

    // Функция загрузки товаров
    function loadProducts(searchTerm = '') {
        fetch('/api/products')
            .then(response => response.json())
            .then(products => {
                const resultsContainer = document.getElementById('product-search-results');
                resultsContainer.innerHTML = '';

                // Фильтрация товаров по поисковому запросу
                const filteredProducts = products.filter(product => {
                    const searchLower = searchTerm.toLowerCase();
                    return (
                        product.name.toLowerCase().includes(searchLower) ||
                        product.id.toString().includes(searchTerm) ||
                        product.category.toLowerCase().includes(searchLower)
                    );
                });

                if (filteredProducts.length === 0) {
                    resultsContainer.innerHTML = '<p class="no-results">Товары не найдены</p>';
                    return;
                }

                filteredProducts.forEach(product => {
                    const productRow = document.createElement('div');
                    productRow.className = 'product-row';
                    productRow.innerHTML = `
                        <p>${product.id}</p>
                        <p>${product.name}</p>
                        <p>${product.category}</p>
                        <p>${product.price_per_hour}₽</p>
                        <button class="select-product" data-id="${product.id}">Выбрать</button>
                    `;
                    resultsContainer.appendChild(productRow);
                });

                // Обработчики для кнопок выбора товара
                document.querySelectorAll('.select-product').forEach(button => {
                    button.addEventListener('click', function() {
                        const productId = this.getAttribute('data-id');
                        addProductToCell(productId);
                    });
                });
            })
            .catch(console.error);
    }

    // Функция добавления товара в ячейку
    function addProductToCell(productId) {
        if (!currentOfficeId || !currentCellId) return;

        fetch(`/api/update-cell/${currentOfficeId}/${currentCellId}`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({product_id: productId})
        })
        .then(response => response.json())
        .then(() => {
            document.getElementById('add-product-dialog').close();
            document.getElementById('cell-dialog').close();
            toggleOfficeDetails(currentOfficeId);
        })
        .catch(console.error);
    }

    // Поиск офисов
    const searchInput = document.getElementById('search');
    if (searchInput) {
        searchInput.addEventListener('input', function() {
            const term = this.value.toLowerCase();
            document.querySelectorAll('.scroll-item').forEach(item => {
                const address = item.querySelector('#head1').textContent.toLowerCase();
                const officeId = item.querySelector('.more').id.split('-')[1];
                const details = document.getElementById(`office-${officeId}`);

                if (address.includes(term)) {
                    item.style.display = '';
                    if (details) details.style.display = 'none';
                } else {
                    item.style.display = 'none';
                    if (details) details.style.display = 'none';
                }
            });
        });
    }
});