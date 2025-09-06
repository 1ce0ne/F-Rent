const periodSelect = document.getElementById('period');
const incomeEl = document.getElementById('income').querySelector('.income-value');
const ordersEl = document.getElementById('orders').querySelector('.value');
const clientsEl = document.getElementById('clients').querySelector('.value');

let chart;

function updateStats(data) {
    incomeEl.textContent = data.income + ' ₽';
    ordersEl.textContent = data.orders;
    clientsEl.textContent = data.clients;

    chart.data.labels = data.labels;
    chart.data.datasets[0].data = data.chart;
    chart.update();
}

async function fetchStats(period) {
    try {
        const response = await fetch(`/api/owner/statistics?period=${period}`);
        if (!response.ok) throw new Error('Ошибка загрузки статистики');
        const data = await response.json();
        updateStats(data);
    } catch (e) {
        console.error(e);
        incomeEl.textContent = 'Ошибка';
        ordersEl.textContent = '-';
        clientsEl.textContent = '-';
        chart.data.labels = [];
        chart.data.datasets[0].data = [];
        chart.update();
    }
}

function updateCategoriesStats(data) {
    // Группируем товары по категориям
    const categories = {};
    data.products.forEach(prod => {
        const cat = prod.product_category || 'Без категории';
        if (!categories[cat]) {
            categories[cat] = {
                category: cat,
                income: 0,
                orders: 0,
                products: 0
            };
        }
        categories[cat].income += prod.total_income;
        categories[cat].orders += prod.total_orders_count;
        categories[cat].products += 1;
    });

    // Заполняем таблицу
    const tbody = document.getElementById('categories-stat-tbody');
    tbody.innerHTML = '';
    Object.values(categories).forEach(cat => {
        tbody.innerHTML += `
            <tr>
                <td>${cat.category}</td>
                <td>${cat.income} ₽</td>
                <td>${cat.orders}</td>
                <td>${cat.products}</td>
                <td>${cat.products > 0 ? Math.round(cat.income / cat.products) + ' ₽' : '-'}</td>
            </tr>
        `;
    });
}

function updateProductsStats(data) {
    // summary
    document.getElementById('total-products').textContent = data.summary.total_products;
    document.getElementById('total-income-all-products').textContent = data.summary.total_income_all_products + ' ₽';
    document.getElementById('total-orders-all-products').textContent = data.summary.total_orders_all_products;
    document.getElementById('most-profitable-product').textContent = data.summary.most_profitable_product || '-';
    document.getElementById('most-popular-product').textContent = data.summary.most_popular_product || '-';

    // table
    const tbody = document.getElementById('products-stat-tbody');
    tbody.innerHTML = '';
    data.products.forEach(prod => {
        tbody.innerHTML += `
            <tr>
                <td>${prod.product_name}</td>
                <td>${prod.product_category}</td>
                <td>${prod.total_income} ₽</td>
                <td>${prod.total_orders_count}</td>
                <td>${prod.office_orders_count} (${prod.office_orders_income} ₽)</td>
                <td>${prod.postamat_orders_count} (${prod.postamat_orders_income} ₽)</td>
                <td>${prod.returned_orders}</td>
                <td>${prod.active_orders}</td>
                <td>${prod.average_rental_time.toFixed(1)}</td>
                <td>${prod.revenue_per_hour.toFixed(2)} ₽</td>
                <td>${prod.popularity_rank}</td>
            </tr>
        `;
    });
}

// --- Исправлено: объявление функции до window.onload ---
async function fetchProductsStats() {
    try {
        const response = await fetch('/api/get_owner_products_statistics');
        if (!response.ok) throw new Error('Ошибка загрузки статистики товаров');
        const data = await response.json();
        updateProductsStats(data);
        updateCategoriesStats(data);
    } catch (e) {
        console.error(e);
        document.getElementById('products-stat-tbody').innerHTML =
            `<tr><td colspan="11" style="text-align:center;color:red;">Ошибка загрузки статистики товаров</td></tr>`;
        document.getElementById('categories-stat-tbody').innerHTML =
            `<tr><td colspan="5" style="text-align:center;color:red;">Ошибка загрузки статистики категорий</td></tr>`;
    }
}
window.onload = function () {
    chart = new Chart(document.getElementById('statChart').getContext('2d'), {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: 'Доход',
                data: [],
                backgroundColor: 'rgba(52, 152, 219, 0.2)',
                borderColor: 'rgba(44, 62, 80, 1)',
                borderWidth: 3,
                pointBackgroundColor: 'rgba(44, 62, 80, 1)',
                tension: 0.3
            }]
        },
        options: {
            responsive: true,
            plugins: {
                legend: {display: false}
            },
            scales: {
                y: {beginAtZero: true}
            }
        }
    });

    fetchStats(periodSelect.value);
    fetchProductsStats();
};

periodSelect.addEventListener('change', function () {
    fetchStats(this.value);
});

// --- Переключение вкладок ---
document.addEventListener('DOMContentLoaded', function () {
    const tabBtns = document.querySelectorAll('.stat-tab-btn');
    const tabContents = document.querySelectorAll('.stat-tab-content');
    tabBtns.forEach(btn => {
        btn.addEventListener('click', function () {
            tabBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            tabContents.forEach(cont => cont.style.display = 'none');
            document.getElementById('tab-' + btn.dataset.tab).style.display = '';
        });
    });
});
