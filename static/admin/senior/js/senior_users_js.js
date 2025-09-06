document.addEventListener('DOMContentLoaded', function() {
    loadUsers();

    // Search functionality
    document.getElementById('search').addEventListener('input', function() {
        const searchTerm = this.value.toLowerCase();
        const users = document.querySelectorAll('.user-item');

        users.forEach(user => {
            const name = user.querySelector('.user-name').textContent.toLowerCase();
            const phone = user.querySelector('.user-phone').textContent.toLowerCase();

            if (name.includes(searchTerm) || phone.includes(searchTerm)) {
                user.style.display = 'grid';
            } else {
                user.style.display = 'none';
            }
        });
    });

    // Handle user dialog close
    document.querySelector('#user-dialog .closedio').addEventListener('click', function() {
        document.getElementById('user-dialog').close();
    });

    // Обработка закрытия окна редактирования пользователя
    const editUserDialog = document.getElementById('edit-user-dialog');
    if (editUserDialog) {
        editUserDialog.querySelector('.closedio').addEventListener('click', function() {
            editUserDialog.close();
        });
    }

    // Обработка формы изменения пользователя
    const editUserForm = document.getElementById('edit-user-form');
    if (editUserForm) {
        editUserForm.addEventListener('submit', function(e) {
            e.preventDefault();
            const userId = document.getElementById('edit-user-id').value;
            const name = document.getElementById('edit-user-name').value;
            const phone = document.getElementById('edit-user-phone').value;
            fetch(`/api/update-user/${userId}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    name: name,
                    phone_number: phone
                })
            })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    editUserDialog.close();
                    loadUsers();
                } else {
                    alert('Ошибка при изменении пользователя: ' + (data.error || 'Неизвестная ошибка'));
                }
            })
            .catch(() => alert('Ошибка при изменении пользователя'));
        });
    }
});

function loadUsers() {
    fetch('/api/get-users')
        .then(response => response.json())
        .then(users => {
            const container = document.getElementById('users-container');
            container.innerHTML = '';

            if (users.length === 0) {
                container.innerHTML = '<p style="text-align: center; grid-column: 1 / -1; padding: 20px;">Пользователи не найдены</p>';
                return;
            }

            users.forEach(user => {
                const userElement = document.createElement('div');
                userElement.className = 'scroll-item user-item';
                userElement.innerHTML = `
                    <p class="user-name">${user.name}</p>
                    <p class="user-phone">${user.phone_number}</p>
                    <p class="user-role">${user.role}</p>
                    <button class="more wide" onclick="showUserDetails(${user.id})">Подробнее</button>
                `;
                container.appendChild(userElement);
            });
        })
        .catch(error => {
            console.error('Error loading users:', error);
            document.getElementById('users-container').innerHTML =
                '<p style="text-align: center; grid-column: 1 / -1; padding: 20px; color: red;">Ошибка загрузки пользователей</p>';
        });
}

function showUserDetails(userId) {
    window.currentUserId = userId;
    fetch(`/api/get-user-details/${userId}`)
        .then(response => {
            if (!response.ok) throw new Error('Ошибка загрузки данных');
            return response.json();
        })
        .then(user => {
            document.getElementById('user-detail-name').textContent = user.name;
            document.getElementById('user-detail-phone').textContent = user.phone_number;
            document.getElementById('user-detail-role').textContent = user.role;
            document.getElementById('user-detail-ban').textContent = user.ban_info || '-';
            document.getElementById('edit-user-btn').onclick = () => editUser(user.id, user.name, user.phone_number);
            document.getElementById('delete-user-btn').onclick = () => deleteUser(user.id);
            // Снимаем старый обработчик, если был
            const banBtn = document.getElementById('ban-user-btn');
            const newBanBtn = banBtn.cloneNode(true);
            banBtn.parentNode.replaceChild(newBanBtn, banBtn);
            // Проверяем, действительно ли пользователь забанен
            if (user.ban_info && user.ban_info !== '-' && user.ban_info.trim() !== '') {
                newBanBtn.textContent = 'Разбанить';
                newBanBtn.classList.add('unban-btn');
                newBanBtn.classList.remove('ban-btn');
                newBanBtn.onclick = () => unbanUser(user.id);
            } else {
                newBanBtn.textContent = 'Забанить';
                newBanBtn.classList.add('ban-btn');
                newBanBtn.classList.remove('unban-btn');
                newBanBtn.onclick = () => showBanDialog(user.id);
            }
            document.getElementById('user-dialog').showModal();
        })
        .catch(error => {
            console.error('Error loading user details:', error);
            alert('Ошибка загрузки данных пользователя');
        });
}

function editUser(userId, userName, userPhone) {
    // Заполнить форму текущими данными
    document.getElementById('edit-user-id').value = userId;
    document.getElementById('edit-user-name').value = userName;
    document.getElementById('edit-user-phone').value = userPhone;
    document.getElementById('edit-user-dialog').showModal();
}

function deleteUser(userId) {
    if (confirm('Вы уверены, что хотите удалить этого пользователя?')) {
        fetch(`/api/delete-user/${userId}`, {
            method: 'DELETE'
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert('Пользователь успешно удален');
                loadUsers();
                document.getElementById('user-dialog').close();
            } else {
                alert('Ошибка при удалении пользователя: ' + (data.error || 'Неизвестная ошибка'));
            }
        })
        .catch(error => {
            console.error('Error deleting user:', error);
            alert('Ошибка при удалении пользователя');
        });
    }
}

function showBanDialog(userId) {
    const banDialog = document.getElementById('ban-dialog');
    const banReasonSelect = document.getElementById('ban-reason');
    const banPeriodLabel = document.getElementById('ban-period-label');
    let reasonsCache = [];
    banReasonSelect.innerHTML = '<option value="">Загрузка...</option>';
    banPeriodLabel.textContent = '-';
    fetch('/api/get-ban-reasons')
        .then(res => {
            if (!res.ok) throw new Error('Ошибка загрузки причин бана');
            return res.json();
        })
        .then(reasons => {
            reasonsCache = reasons;
            banReasonSelect.innerHTML = '';
            if (!Array.isArray(reasons) || reasons.length === 0) {
                banReasonSelect.innerHTML = '<option value="">Нет причин</option>';
                banPeriodLabel.textContent = '-';
                return;
            }
            reasons.forEach(r => {
                const opt = document.createElement('option');
                opt.value = r.reason_id;
                opt.textContent = r.name + (r.description ? ` — ${r.description}` : '');
                opt.dataset.banPeriod = r.ban_period;
                banReasonSelect.appendChild(opt);
            });
            banReasonSelect.selectedIndex = 0;
            const selected = reasonsCache.find(r => r.reason_id == banReasonSelect.value);
            banPeriodLabel.textContent = selected ? selected.ban_period + ' дней' : '-';
        })
        .catch(err => {
            banReasonSelect.innerHTML = '<option value="">Ошибка загрузки</option>';
            banPeriodLabel.textContent = '-';
        });
    banReasonSelect.onchange = function() {
        const selected = reasonsCache.find(r => r.reason_id == banReasonSelect.value);
        // Исправлено: поддержка строкового формата периода ("3 дня", "forever")
        if (selected) {
            banPeriodLabel.textContent = selected.ban_period;
        } else {
            banPeriodLabel.textContent = '-';
        }
    };
    // --- Исправление: обработчик submit навешивается только один раз ---
    const banForm = document.getElementById('ban-user-form');
    if (!banForm._banHandlerAttached) {
        banForm.addEventListener('submit', function(e) {
            e.preventDefault();
            const reasonId = banReasonSelect.value;
            // Исправлено: ищем причину только среди option, реально присутствующих в select
            let selected = null;
            for (const r of reasonsCache) {
                if (String(r.reason_id) === String(reasonId)) {
                    selected = r;
                    break;
                }
            }
            // Проверка на пустой reasonId (ничего не выбрано)
            if (!reasonId || !selected) {
                alert('Пожалуйста, выберите причину бана!');
                return;
            }
            // Отладка: выводим причину и её ban_period
            console.log('Выбрана причина:', selected);
            // Приводим ban_period и reason к строке всегда
            let banPeriod = String(selected.ban_period || '').trim().toLowerCase();
            let banReason = String(selected.name || '').trim();
            if (!banPeriod) {
                alert('Ошибка: у выбранной причины не задан период бана!');
                return;
            }
            if (banPeriod === 'forever' || banPeriod === 'навсегда') {
                fetch(`/api/ban-user/${userId}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ reason_id: reasonId, ban_end_time: "Никогда", ban_period: banPeriod, ban_reason: banReason })
                })
                .then(res => res.json())
                .then(data => {
                    if (data.success) {
                        banDialog.close();
                        showUserDetails(userId);
                    } else {
                        alert('Ошибка при бане: ' + (data.error || 'Неизвестная ошибка'));
                    }
                });
                return;
            }
            // Парсим число дней из строки, например "3 дня"
            const match = banPeriod.match(/(\d+)/);
            if (!match) {
                alert('Ошибка: не удалось определить период бана!');
                return;
            }
            const days = Number(match[1]);
            if (isNaN(days) || days <= 0) {
                alert('Ошибка: некорректное число дней!');
                return;
            }
            // Исправлено: определяем now перед использованием
            const now = new Date();
            // Формируем дату окончания в формате 05.08.2025 14:36
            const endDateObj = new Date(now.getTime() + days * 24 * 60 * 60 * 1000);
            if (isNaN(endDateObj.getTime())) {
                alert('Ошибка вычисления даты окончания бана!');
                return;
            }
            const pad = n => n.toString().padStart(2, '0');
            const endTime = `${pad(endDateObj.getDate())}.${pad(endDateObj.getMonth() + 1)}.${endDateObj.getFullYear()} ${pad(endDateObj.getHours())}:${pad(endDateObj.getMinutes())}`;
            fetch(`/api/ban-user/${userId}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ reason_id: reasonId, ban_end_time: endTime, ban_period: banPeriod, ban_reason: banReason })
            })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    banDialog.close();
                    showUserDetails(userId);
                } else {
                    alert('Ошибка при бане: ' + (data.error || 'Неизвестная ошибка'));
                }
            });
        });
        banForm._banHandlerAttached = true;
    }
    banDialog.querySelector('.closedio').onclick = () => banDialog.close();
    banDialog.showModal();
}

// Функция для разбана пользователя
function unbanUser(userId) {
    if (!confirm('Вы уверены, что хотите разбанить пользователя?')) return;
    fetch(`/api/unban-user/${userId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            showUserDetails(userId);
        } else {
            alert('Ошибка при разбане: ' + (data.error || 'Неизвестная ошибка'));
        }
    })
    .catch(() => alert('Ошибка при разбане пользователя!'));
}
