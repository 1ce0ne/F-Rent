// Скрипт для мобильного меню навигации
window.addEventListener('DOMContentLoaded', function() {
    // Берём первую видимую кнопку .nav-toggle на странице
    const navToggle = document.querySelector('.nav-toggle');
    // Меню для пок��за/скрытия (div.nav)
    const navMenu = document.querySelector('nav');
    if (!navToggle || !navMenu) return;

    navToggle.addEventListener('click', function() {
        // Для мобильного: показываем/скрываем меню
        if (navMenu.style.display === 'flex') {
            console.log('Скрытие мобильного меню');
            navMenu.style.display = 'none';
            navMenu.style.flexDirection = '';
            navMenu.style.position = '';
            navMenu.style.top = '';
            navMenu.style.left = '';
            navMenu.style.width = '';
            navMenu.style.background = '';
            navMenu.style.zIndex = '';
            navMenu.style.height = '';
            navMenu.style.alignItems = '';
            navMenu.style.justifyContent = '';
            return;
        } else {
            console.log('Показ мобильного меню');
            navMenu.style.display = 'flex';
            navMenu.style.flexDirection = 'column';
            navMenu.style.position = 'fixed';
            navMenu.style.top = '60px';
            navMenu.style.left = '0';
            navMenu.style.width = '60%';
            navMenu.style.background = '#fff';
            navMenu.style.zIndex = '1000';
            navMenu.style.height = '91.7%';
            navMenu.style.alignItems = 'center';
            navMenu.style.justifyContent = 'space-between';
            return;
        }
    });

    // Скрывать меню при изменении размера окна
    window.addEventListener('resize', function() {
        if (window.innerWidth > 800) {
            navMenu.style.display = '';
            navMenu.style.position = '';
            navMenu.style.width = '';
            navMenu.style.top = '';
            navMenu.style.left = '';
            navMenu.style.background = '';
            navMenu.style.zIndex = '';
            navMenu.style.height = '';
            navMenu.style.alignItems = '';
            navMenu.style.justifyContent = '';
        } else {
            navMenu.style.display = 'none';
        }
    });
});
