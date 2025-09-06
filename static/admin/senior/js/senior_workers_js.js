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
        document.getElementById('add-worker-phone').value = '';
        document.getElementById('add-worker-role').value = 'office_worker';
        document.getElementById('worker-add-dialog').showModal();
    });

    // Закрытие формы добавления работника
    document.getElementById('close-add-worker').addEventListener('click', function() {
        document.getElementById('worker-add-dialog').close();
    });

    // Закрытие диалога привязки к офису
    document.getElementById('close-office-assign').addEventListener('click', function() {
        document.getElementById('office-assign-dialog').close();
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

        const requestData = {
            username: name,
            password: password,
            worker_type: role
        };

        if (phone) {
            requestData.phone_number = phone;
        }

        fetch('/api/add-worker', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(requestData)
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                document.getElementById('worker-add-dialog').close();
                loadWorkers();
            } else {
                alert('Ошибка при добавлении работника: ' + (data.error || 'Неизвестная ошибка'));
            }
        })
        .catch(() => alert('Ошибка при добавлении работника'));
    };
});

let currentWorkerId = null;

function loadWorkers() {
    fetch('/api/get-workers-for-senior')
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
                workerElement.innerHTML = `
                    <p>${worker.name}</p>
                    <p>${worker.position}</p>
                    <p id="worker-offices-${worker.id}">Загрузка...</p>
                    <p>${worker.id}</p>
                    <button class="more" onclick="showWorkerDetails(${worker.id})">Подробнее</button>
                `;
                container.appendChild(workerElement);

                // Загружаем офисы для каждого работника
                loadWorkerOffices(worker.id);
            });
        })
        .catch(error => {
            console.error('Error loading workers:', error);
            document.getElementById('workers-container').innerHTML = '<p style="text-align: center; grid-column: 1 / -1; padding: 20px; color: red;">Ошибка загрузки работников</p>';
        });
}

function loadWorkerOffices(workerId) {
    fetch(`/api/worker-office-assignments/${workerId}`)
        .then(response => response.json())
        .then(data => {
            const officesElement = document.getElementById(`worker-offices-${workerId}`);
            if (data.assignments && data.assignments.length > 0) {
                const officeNames = data.assignments.map(assignment => assignment.office_address).join(', ');
                officesElement.textContent = officeNames;
            } else {
                officesElement.textContent = 'Не привязан';
            }
        })
        .catch(error => {
            console.error('Error loading worker offices:', error);
            const officesElement = document.getElementById(`worker-offices-${workerId}`);
            if (officesElement) {
                officesElement.textContent = 'Ошибка загрузки';
            }
        });
}

function showWorkerDetails(workerId) {
    currentWorkerId = workerId;

    fetch(`/api/get-worker-details/${workerId}`)
        .then(response => response.json())
        .then(worker => {
            document.getElementById('worker-name').textContent = worker.name;
            document.getElementById('worker-position').textContent = worker.position;
            document.getElementById('worker-phone').textContent = worker.phone_number || 'Не указан';
            document.getElementById('worker-telegram').textContent = worker.telegram_username ? '@' + worker.telegram_username : 'Не указан';
            document.getElementById('worker-2fa-status').textContent = worker.telegram_2fa_enabled ? 'Включена' : 'Выключена';

            // Загружаем привязанные офисы
            loadWorkerOfficeAssignments(workerId);

            // Настраиваем кнопки
            setupWorkerButtons(workerId, worker);

            document.getElementById('worker-dialog').showModal();
        })
        .catch(error => {
            console.error('Error loading worker details:', error);
            alert('Ошибка загрузки информации о работнике');
        });
}

function loadWorkerOfficeAssignments(workerId) {
    fetch(`/api/worker-office-assignments/${workerId}`)
        .then(response => response.json())
        .then(data => {
            const officeList = document.getElementById('worker-office-list');
            officeList.innerHTML = '';

            if (data.assignments && data.assignments.length > 0) {
                data.assignments.forEach(assignment => {
                    const officeDiv = document.createElement('div');
                    officeDiv.className = 'office-assignment-item';
                    officeDiv.style.cssText = 'display: flex; justify-content: space-between; align-items: center; padding: 8px; border: 1px solid #ddd; margin: 4px 0; border-radius: 4px;';
                    officeDiv.innerHTML = `
                        <span>${assignment.office_address}</span>
                        <button class="more red" onclick="unassignWorkerFromOffice(${assignment.assignment_id})" 
                                style="padding: 4px 8px; font-size: 12px;">Отвязать</button>
                    `;
                    officeList.appendChild(officeDiv);
                });
            } else {
                officeList.innerHTML = '<p style="color: #666; font-style: italic;">Работник не привязан ни к одному офису</p>';
            }
        })
        .catch(error => {
            console.error('Error loading office assignments:', error);
            document.getElementById('worker-office-list').innerHTML = '<p style="color: red;">Ошибка загрузки офисов</p>';
        });
}

function setupWorkerButtons(workerId, worker) {
    // Кнопка привязки к офису
    document.getElementById('assign-office-btn').onclick = function() {
        showOfficeAssignDialog(workerId);
    };

    // Кнопка редактирования
    document.getElementById('edit-worker-btn').onclick = function() {
        document.getElementById('edit-worker-name').value = worker.name;
        document.getElementById('edit-worker-password').value = '';
        document.getElementById('edit-worker-phone').value = worker.phone_number || '';
        document.getElementById('edit-worker-telegram').value = worker.telegram_username || '';
        document.getElementById('edit-worker-role').value = worker.role;
        document.getElementById('edit-worker-form').style.display = 'block';
    };

    // Кнопка отмены редактирования
    document.getElementById('cancel-edit-worker').onclick = function() {
        document.getElementById('edit-worker-form').style.display = 'none';
    };

    // Обработка формы редактирования
    document.getElementById('edit-worker-form').onsubmit = function(e) {
        e.preventDefault();
        updateWorker(workerId);
    };

    // Кнопка переключения 2FA
    document.getElementById('toggle-2fa-btn').onclick = function() {
        toggle2FA(workerId, !worker.telegram_2fa_enabled);
    };

    // Кнопка удаления
    document.getElementById('delete-worker-btn').onclick = function() {
        if (confirm(`Вы уверены, что хотите удалить работника ${worker.name}?`)) {
            deleteWorker(workerId);
        }
    };
}

function showOfficeAssignDialog(workerId) {
    fetch(`/api/worker-available-offices/${workerId}`)
        .then(response => response.json())
        .then(data => {
            const officesList = document.getElementById('available-offices-list');
            officesList.innerHTML = '';

            if (data.offices && data.offices.length > 0) {
                data.offices.forEach(office => {
                    const officeDiv = document.createElement('div');
                    officeDiv.className = 'available-office-item';
                    officeDiv.style.cssText = 'display: flex; justify-content: space-between; align-items: center; padding: 12px; border: 1px solid #ddd; margin: 8px 0; border-radius: 4px; background: #f9f9f9;';
                    officeDiv.innerHTML = `
                        <div>
                            <strong>${office.address}</strong><br>
                            <small style="color: #666;">ID: ${office.id}</small>
                        </div>
                        <button class="more confs" onclick="assignWorkerToOffice(${workerId}, ${office.id})" 
                                style="padding: 6px 12px;">Привязать</button>
                    `;
                    officesList.appendChild(officeDiv);
                });
            } else {
                officesList.innerHTML = '<p style="text-align: center; color: #666; padding: 20px;">Нет доступных офисов для привязки</p>';
            }

            document.getElementById('office-assign-dialog').showModal();
        })
        .catch(error => {
            console.error('Error loading available offices:', error);
            alert('Ошибка загрузки доступных офисов');
        });
}

function assignWorkerToOffice(workerId, officeId) {
    fetch('/api/assign-worker-to-office', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            worker_id: workerId,
            office_id: officeId
        })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            document.getElementById('office-assign-dialog').close();
            loadWorkerOfficeAssignments(workerId);
            loadWorkerOffices(workerId); // Обновляем список в таблице
            alert(data.message);
        } else {
            alert('Ошибка: ' + (data.error || 'Неизвестная ошибка'));
        }
    })
    .catch(error => {
        console.error('Error assigning worker to office:', error);
        alert('Ошибка при привязке работника к офису');
    });
}

function unassignWorkerFromOffice(assignmentId) {
    if (!confirm('Вы уверены, что хотите отвязать работника от этого офиса?')) {
        return;
    }

    fetch(`/api/unassign-worker-from-office/${assignmentId}`, {
        method: 'DELETE'
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            loadWorkerOfficeAssignments(currentWorkerId);
            loadWorkerOffices(currentWorkerId); // Обновляем список в таблице
            alert(data.message);
        } else {
            alert('Ошибка: ' + (data.error || 'Неизвестная ошибка'));
        }
    })
    .catch(error => {
        console.error('Error unassigning worker from office:', error);
        alert('Ошибка при отвязке работника от офиса');
    });
}

function updateWorker(workerId) {
    const updateData = {
        username: document.getElementById('edit-worker-name').value.trim(),
        phone_number: document.getElementById('edit-worker-phone').value.trim(),
        telegram_username: document.getElementById('edit-worker-telegram').value.trim(),
        worker_type: document.getElementById('edit-worker-role').value
    };

    const password = document.getElementById('edit-worker-password').value;
    if (password) {
        updateData.password = password;
    }

    fetch(`/api/update-worker/${workerId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(updateData)
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            document.getElementById('edit-worker-form').style.display = 'none';
            document.getElementById('worker-dialog').close();
            loadWorkers();
            alert('Информация о работнике обновлена');
        } else {
            alert('Ошибка: ' + (data.error || 'Неизвестная ошибка'));
        }
    })
    .catch(error => {
        console.error('Error updating worker:', error);
        alert('Ошибка при обновлении информации о работнике');
    });
}

function toggle2FA(workerId, enabled) {
    fetch(`/api/toggle-worker-2fa/${workerId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ enabled: enabled })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            document.getElementById('worker-2fa-status').textContent = enabled ? 'Включена' : 'Выключена';
            alert(data.message);
        } else {
            alert('Ошибка: ' + (data.error || 'Неизвестная ошибка'));
        }
    })
    .catch(error => {
        console.error('Error toggling 2FA:', error);
        alert('Ошибка при изменении статуса 2FA');
    });
}

function deleteWorker(workerId) {
    fetch(`/api/delete-worker/${workerId}`, {
        method: 'DELETE'
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            document.getElementById('worker-dialog').close();
            loadWorkers();
            alert('Работник удален');
        } else {
            alert('Ошибка: ' + (data.error || 'Неизвестная ошибка'));
        }
    })
    .catch(error => {
        console.error('Error deleting worker:', error);
        alert('Ошибка при удалении работника');
    });
}
