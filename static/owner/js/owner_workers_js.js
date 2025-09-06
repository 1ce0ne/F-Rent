document.addEventListener('DOMContentLoaded', function() {
    loadWorkers();

    // Поиск по имени
    document.getElementById('search').addEventListener('input', function() {
        const searchTerm = this.value.toLowerCase();
        const workers = document.querySelectorAll('.scroll-item');
        workers.forEach(worker => {
            const name = worker.querySelector('p').textContent.toLowerCase();
            if (name.includes(searchTerm)) {
                worker.style.display = 'flex';
            } else {
                worker.style.display = 'none';
            }
        });
    });

    // Закрытие диалога работника
    document.querySelector('#worker-dialog .closedio').addEventListener('click', function() {
        document.getElementById('worker-dialog').close();
    });

    // Открытие формы добавления работника
    document.getElementById('add-worker-btn').addEventListener('click', function() {
        document.getElementById('add-worker-name').value = '';
        document.getElementById('add-worker-password').value = '';
        document.getElementById('add-worker-role').value = 'office_worker';
        document.getElementById('worker-add-dialog').showModal();
    });

    // Закрытие формы добавления работника
    document.getElementById('close-add-worker').addEventListener('click', function() {
        document.getElementById('worker-add-dialog').close();
    });

    // Обработка отправки формы добавления работника
    document.getElementById('add-worker-form').onsubmit = function(e) {
        e.preventDefault();
        const name = document.getElementById('add-worker-name').value.trim();
        const password = document.getElementById('add-worker-password').value;
        const phone = document.getElementById('add-worker-phone').value.trim();
        const role = document.getElementById('add-worker-role').value;
        if (!name || !password || !role) {
            alert('Заполните все обязательные поля');
            return;
        }

        const requestBody = {
            username: name,
            password: password,
            worker_type: role
        };

        // Добавляем номер телефона если указан
        if (phone) {
            requestBody.phone_number = phone;
        }

        fetch('/api/add-worker', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(requestBody)
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                document.getElementById('worker-add-dialog').close();
                loadWorkers();
                alert('Работник успешно добавлен');
            } else {
                alert('Ошибка при добавлении работника: ' + (data.error || 'Неизвестная ошибка'));
            }
        })
        .catch(() => alert('Ошибка при добавлении работника'));
    };
});

function loadWorkers() {
    fetch('/api/get-workers')
        .then(response => response.json())
        .then(workers => {
            const container = document.getElementById('workers-container');
            container.innerHTML = '';
            if (workers.length === 0) {
                container.innerHTML = '<p style="text-align: center; grid-column: 1 / -1; padding: 20px;">Работники не найдены</p>';
                return;
            }
            workers.forEach(worker => {
                const workerElement = document.createElement('div');
                workerElement.className = 'scroll-item';
                // Передаем роль в showWorkerDetails
                workerElement.innerHTML = `
                    <p>${worker.name}</p>
                    <p>${worker.position}</p>
                    <p>${worker.office ? worker.office : 'Не назначен'}</p>
                    <p>${worker.id}</p>
                    <button class="more" onclick="showWorkerDetails(${worker.id}, '${worker.role ? worker.role : ''}')">Подробнее</button>
                `;
                container.appendChild(workerElement);
            });
        })
        .catch(error => {
            console.error('Error loading workers:', error);
            document.getElementById('workers-container').innerHTML =
                '<p style="text-align: center; grid-column: 1 / -1; padding: 20px; color: red;">Ошибка загрузки работников</p>';
        });
}

function showWorkerDetails(workerId, role) {
    if (!workerId) {
        alert('Некорректный ID работника');
        return;
    }
    fetch(`/api/get-worker-details/${workerId}?role=${encodeURIComponent(role || '')}`)
        .then(response => response.json())
        .then(worker => {
            document.getElementById('worker-name').textContent = worker.name;
            document.getElementById('worker-position').textContent = worker.position;
            document.getElementById('worker-phone').textContent = worker.phone_number || 'Не указан';
            document.getElementById('worker-telegram').textContent = worker.telegram_username ? '@' + worker.telegram_username : 'Не указан';
            document.getElementById('worker-office').textContent = worker.office || 'Не назначен';

            // Обновляем статус Telegram 2FA
            updateTelegram2FAStatus(worker);

            if (document.getElementById('worker-role')) {
                document.getElementById('worker-role').textContent = worker.role || '-';
            }
            document.getElementById('edit-worker-btn').onclick = () => editWorker(worker.id, worker.name, worker.role, worker.phone_number, worker.telegram_username);
            document.getElementById('delete-worker-btn').onclick = () => deleteWorker(worker.id);
            document.getElementById('worker-dialog').showModal();
        })
        .catch(error => {
            console.error('Error loading worker details:', error);
            alert('Ошибка загрузки данных работника');
        });
}

function updateTelegram2FAStatus(worker) {
    const statusElement = document.getElementById('telegram-2fa-status');
    const toggleBtn = document.getElementById('toggle-2fa-btn');

    if (worker.telegram_2fa_enabled) {
        statusElement.textContent = 'Включена';
        statusElement.style.color = '#28a745';
        toggleBtn.textContent = 'Выключить';
        toggleBtn.className = 'more confs red small';
    } else {
        statusElement.textContent = 'Выключена';
        statusElement.style.color = '#dc3545';
        toggleBtn.textContent = 'Включить';
        toggleBtn.className = 'more confs blue small';
    }

    // Обработчик для переключения 2FA
    toggleBtn.onclick = () => toggle2FA(worker.id, !worker.telegram_2fa_enabled, worker.phone_number);
}

function toggle2FA(workerId, enabled, phoneNumber) {
    // Проверяем наличие номера телефона при включении
    if (enabled && !phoneNumber) {
        alert('Для включения 2FA необходимо сначала указать номер телефона работника');
        return;
    }

    const action = enabled ? 'включить' : 'выключить';
    if (!confirm(`Вы уверены, что хотите ${action} двухфакторную аутентификацию для этого работника?`)) {
        return;
    }

    fetch(`/api/toggle-worker-2fa/${workerId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ enabled: enabled })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            alert(data.message);
            // Обновляем статус
            const worker = { telegram_2fa_enabled: data.telegram_2fa_enabled };
            updateTelegram2FAStatus(worker);
        } else {
            alert('Ошибка: ' + (data.error || 'Неизвестная ошибка'));
        }
    })
    .catch(error => {
        console.error('Error toggling 2FA:', error);
        alert('Ошибка при изменении настроек 2FA');
    });
}

function editWorker(workerId, currentName, role, currentPhone, currentTelegram) {
    // Показать форму редактирования
    document.getElementById('edit-worker-form').style.display = '';
    document.getElementById('edit-worker-name').value = currentName || '';
    document.getElementById('edit-worker-password').value = '';
    document.getElementById('edit-worker-phone').value = currentPhone || '';
    document.getElementById('edit-worker-telegram').value = currentTelegram || '';
    // Скрыть кнопки действий
    document.querySelector('#worker-dialog .worker-actions').style.display = 'none';

    // Обработка отмены
    document.getElementById('cancel-edit-worker').onclick = function() {
        document.getElementById('edit-worker-form').style.display = 'none';
        document.querySelector('#worker-dialog .worker-actions').style.display = '';
    };

    // Обработка отправки формы
    document.getElementById('edit-worker-form').onsubmit = function(e) {
        e.preventDefault();
        const newName = document.getElementById('edit-worker-name').value.trim();
        const newPassword = document.getElementById('edit-worker-password').value;
        const newPhone = document.getElementById('edit-worker-phone').value.trim();
        const newTelegram = document.getElementById('edit-worker-telegram').value.trim();

        if (!newName) {
            alert('Имя не может быть пустым');
            return;
        }

        const body = {
            username: newName,
            phone_number: newPhone,
            telegram_username: newTelegram
        };
        if (newPassword) body.password = newPassword;

        fetch(`/api/update-worker/${workerId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                document.getElementById('edit-worker-form').style.display = 'none';
                document.querySelector('#worker-dialog .worker-actions').style.display = '';
                document.getElementById('worker-dialog').close();
                loadWorkers();
                alert('Данные работника обновлены');
            } else {
                alert('Ошибка при обновлении работника: ' + (data.error || 'Неизвестная ошибка'));
            }
        })
        .catch(() => alert('Ошибка при обновлении работника'));
    };
}

function deleteWorker(workerId) {
    if (!confirm('Вы уверены, что хотите удалить работника?')) return;
    fetch(`/api/delete-worker/${workerId}`, { method: 'DELETE' })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                document.getElementById('worker-dialog').close();
                loadWorkers();
            } else {
                alert('Ошибка при удалении работника: ' + (data.error || 'Неизвестная ошибка'));
            }
        })
        .catch(() => alert('Ошибка при удалении работника'));
}
