package com.example.akkubattrent.AllCategories;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.Classes.ProductWithCoordinates;
import com.example.akkubattrent.R;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class AllCategoriesActivity extends AppCompatActivity {

    // Элементы интерфейса для поиска
    private FrameLayout searchFrame; // Контейнер поисковой строки
    private ImageView searchButtonImg; // Кнопка поиска/закрытия
    private EditText searchEditText; // Поле ввода для поиска
    private LinearLayout categoriesContainer; // Контейнер с категориями
    private LinearLayout searchResultsContainer; // Контейнер для результатов поиска
    private ProgressBar searchProgressBar; // Индикатор загрузки
    private TextView searchLoadingText; // Текст "Идет поиск..."
    private TextView noSearchResultsText; // Текст "Товары не найдены"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        // Инициализация элементов интерфейса
        searchFrame = findViewById(R.id.searchFrame);
        searchButtonImg = findViewById(R.id.search_button_img);

        // Получаем контейнер с категориями из layout
        FrameLayout mainFrame = findViewById(R.id.frameLayout3);
        ScrollView scrollView = (ScrollView) mainFrame.getChildAt(0);
        categoriesContainer = (LinearLayout) scrollView.getChildAt(0);

        // Создаем контейнер для результатов поиска
        searchResultsContainer = new LinearLayout(this);
        searchResultsContainer.setOrientation(LinearLayout.VERTICAL);
        searchResultsContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        // Добавляем контейнер для результатов в основной layout
        mainFrame.addView(searchResultsContainer);
        searchResultsContainer.setVisibility(View.GONE); // Сначала скрываем

        // Настраиваем индикатор загрузки
        searchProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleLarge);
        searchLoadingText = new TextView(this);
        searchLoadingText.setText("Поиск товаров...");
        searchLoadingText.setTextSize(16);
        searchLoadingText.setGravity(View.TEXT_ALIGNMENT_CENTER);

        // Настраиваем текст для пустых результатов
        noSearchResultsText = new TextView(this);
        noSearchResultsText.setText("Товары не найдены");
        noSearchResultsText.setTextSize(16);
        noSearchResultsText.setGravity(View.TEXT_ALIGNMENT_CENTER);

        // Создаем и настраиваем поле ввода для поиска
        searchEditText = new EditText(this);
        searchEditText.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
        searchEditText.setPadding(35, 50, 60, 40); // 15dp слева, остальные отступы для удобства
        searchEditText.setHint("Поиск товаров...");
        searchEditText.setTextSize(16);
        searchEditText.setTextColor(getResources().getColor(android.R.color.black));
        searchEditText.setHintTextColor(getResources().getColor(android.R.color.darker_gray));
        searchEditText.setBackgroundColor(Color.TRANSPARENT); // Прозрачный фон
// Добавляем нижнюю границу
        searchEditText.getBackground().mutate().setColorFilter(
                getResources().getColor(android.R.color.darker_gray),
                PorterDuff.Mode.SRC_ATOP);
        searchFrame.addView(searchEditText, 0);

        // Обработчик клика по кнопке поиска/закрытия
        searchButtonImg.setOnClickListener(v -> {
            if (searchEditText.getText().toString().isEmpty()) {
                // Если поиск пустой - ничего не делаем
                return;
            }

            if (searchResultsContainer.getVisibility() == View.VISIBLE) {
                // Если результаты видны - очищаем поиск
                searchEditText.setText("");
                searchResultsContainer.setVisibility(View.GONE);
                categoriesContainer.setVisibility(View.VISIBLE);
                searchButtonImg.setImageResource(R.drawable.search_button);
            } else {
                // Иначе выполняем поиск
                performSearch(searchEditText.getText().toString());
            }
        });

        // Устанавливаем размер кнопки закрытия (25dp)
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                (int) (20 * getResources().getDisplayMetrics().density), // конвертируем dp в px
                (int) (20 * getResources().getDisplayMetrics().density));
        params.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        params.rightMargin = (int) (15 * getResources().getDisplayMetrics().density);
        searchButtonImg.setLayoutParams(params);

        // Обработчик изменения текста для поиска при вводе
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    // Если есть текст - меняем иконку на "закрыть" и выполняем поиск
                    searchButtonImg.setImageResource(R.drawable.close_button_icon);
                    performSearch(s.toString());
                } else {
                    // Если текст пустой - возвращаем иконку поиска и скрываем результаты
                    searchButtonImg.setImageResource(R.drawable.search_button);
                    searchResultsContainer.setVisibility(View.GONE);
                    categoriesContainer.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Кнопка закрытия категорий (возврат в главное меню)
        // В AllCategoriesActivity:
        Button closeButton = findViewById(R.id.closeCategoryButton);
        closeButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, R.anim.slide_out_left);
        });

        // Инициализация обработчиков для кнопок категорий
        initializeCategoryButtons();
    }

    /**
     * Инициализация обработчиков нажатий для кнопок категорий
     */
    private void initializeCategoryButtons() {
        // Кнопка "Проекторы"
        FrameLayout projectorsButton = findViewById(R.id.projectorsButton);
        projectorsButton.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategoriesActivity.this, AllProjectorsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });
        ImageView projectorImage = findViewById(R.id.projector_image);
        projectorImage.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategoriesActivity.this, AllProjectorsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });

        // Кнопка "Игровые приставки"
        FrameLayout consolesButton = findViewById(R.id.consolesButton);
        consolesButton.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategoriesActivity.this, AllConsolesWindow.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });
        ImageView consoleImage = findViewById(R.id.game_console_image);
        consoleImage.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategoriesActivity.this, AllConsolesWindow.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });

        // Кнопка "Пылесосы"
        FrameLayout vacuumCleanerButton = findViewById(R.id.vacuumCleanerButton);
        vacuumCleanerButton.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategoriesActivity.this, AllVacuumCleanerActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });
        ImageView vacuumCleanerImage = findViewById(R.id.vacuum_cleaner_image);
        vacuumCleanerImage.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategoriesActivity.this, AllVacuumCleanerActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });

        // Кнопка "Мойка высокого давления"
        FrameLayout pressureWasherButton = findViewById(R.id.pressureWasherButton);
        pressureWasherButton.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategoriesActivity.this, PressureWashersActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });
        ImageView pressureWasherImage = findViewById(R.id.pressure_washer_image);
        pressureWasherImage.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategoriesActivity.this, PressureWashersActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });

        // Кнопка "Лобзики"
        FrameLayout lobzikiButton = findViewById(R.id.lobzikiButton);
        lobzikiButton.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategoriesActivity.this, JigsawsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });
        ImageView jigsawsImage = findViewById(R.id.jigsaws_image);
        jigsawsImage.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategoriesActivity.this, JigsawsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });

        // Кнопка "Перфораторы"
        FrameLayout perforatorsButton = findViewById(R.id.perforatorsButton);
        perforatorsButton.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategoriesActivity.this, PerforatorsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });
        ImageView punchersImage = findViewById(R.id.punchers_image);
        punchersImage.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategoriesActivity.this, PerforatorsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });

        // Кнопка "Строительные пылесосы"
        FrameLayout stroitVacuumCleanerButton = findViewById(R.id.stroitVacuumCleanerButton);
        stroitVacuumCleanerButton.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategoriesActivity.this, ConstructionVacuumCleanersActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });
        ImageView constructionVacuumCleanerImage = findViewById(R.id.constrution_vacuum_cleaner_image);
        constructionVacuumCleanerImage.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategoriesActivity.this, ConstructionVacuumCleanersActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });

        // Кнопка "Шуруповерты"
        FrameLayout screwDriverButton = findViewById(R.id.screwDriverButton);
        screwDriverButton.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategoriesActivity.this, ScrewdriversActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });
        ImageView screwdriverImage = findViewById(R.id.screwdriver_image);
        screwdriverImage.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategoriesActivity.this, ScrewdriversActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });

        // Кнопка "Фрезеры"
        FrameLayout millingMachineButton = findViewById(R.id.millingMachineButton);
        millingMachineButton.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategoriesActivity.this, MillingMachinesActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });
        ImageView millingMachineImage = findViewById(R.id.milling_machine_image);
        millingMachineImage.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategoriesActivity.this, MillingMachinesActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });

        // Кнопка "Винтоверт"
        FrameLayout vintovertButton = findViewById(R.id.vintovertButton);
        vintovertButton.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategoriesActivity.this, VintovertActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });
        ImageView vintovertImage = findViewById(R.id.vintovert_image);
        vintovertImage.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategoriesActivity.this, VintovertActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });

        // Кнопка "Цепные пилы"
        FrameLayout chainSawButton = findViewById(R.id.chainSawButton);
        chainSawButton.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategoriesActivity.this, ChainSawsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });
        ImageView chainSawImage = findViewById(R.id.chain_saw_image);
        chainSawImage.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategoriesActivity.this, ChainSawsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });

        // Кнопка "Воздуходувы"
        FrameLayout airCompressorButton = findViewById(R.id.airCompressorButton);
        airCompressorButton.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategoriesActivity.this, AirCompressorsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });
        ImageView airCompressorImage = findViewById(R.id.air_compressor_image);
        airCompressorImage.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategoriesActivity.this, AirCompressorsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });

        // Кнопка "Триммер"
        FrameLayout grassTrimmerButton = findViewById(R.id.grassTrimmerButton);
        grassTrimmerButton.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategoriesActivity.this, GrassTrimmersActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });
        ImageView grassTrimmerImage = findViewById(R.id.grass_trimmer_image);
        grassTrimmerImage.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategoriesActivity.this, GrassTrimmersActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });
    }

    /**
     * Выполняет поиск товаров по запросу
     * @param query Поисковый запрос
     */
    private void performSearch(String query) {
        if (query.isEmpty()) {
            // Если запрос пустой - скрываем результаты и показываем категории
            searchResultsContainer.setVisibility(View.GONE);
            categoriesContainer.setVisibility(View.VISIBLE);
            return;
        }

        // Показываем индикатор загрузки
        searchResultsContainer.removeAllViews();
        searchResultsContainer.addView(searchProgressBar);
        searchResultsContainer.addView(searchLoadingText);
        searchResultsContainer.setVisibility(View.VISIBLE);
        categoriesContainer.setVisibility(View.GONE);

        // Запускаем поиск в отдельном потоке
        new Thread(() -> {
            try {
                // Подключаемся к базе данных
                Class.forName("com.mysql.jdbc.Driver");
                Connection connection = DriverManager.getConnection(
                        "jdbc:mysql://89.23.113.93:3306/AkkuBattRent",
                        "akku_batt_admin",
                        "8rssCz31UiUr512e"
                );

                // SQL запрос для поиска товаров
                String searchQuery = "SELECT id, name, description, price_per_hour, price_per_day, " +
                        "price_per_month, image, address_of_postamat " +
                        "FROM products " +
                        "WHERE name LIKE ? AND address_of_postamat IS NOT NULL";

                PreparedStatement stmt = connection.prepareStatement(searchQuery);
                stmt.setString(1, "%" + query + "%"); // Подставляем поисковый запрос
                ResultSet rs = stmt.executeQuery();

                // Собираем найденные товары в список
                ArrayList<ProductWithCoordinates> products = new ArrayList<>();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    String description = rs.getString("description");
                    double pricePerHour = rs.getDouble("price_per_hour");
                    double pricePerDay = rs.getDouble("price_per_day");
                    double pricePerMonth = rs.getDouble("price_per_month");
                    byte[] image = rs.getBytes("image");
                    String address = rs.getString("address_of_postamat");

                    // Получаем координаты постамата
                    double firstCoordinate = 0.0;
                    double secondCoordinate = 0.0;

                    if (address != null && !address.isEmpty()) {
                        String coordQuery = "SELECT first_coordinate, second_coordinate " +
                                "FROM office WHERE address = ?";
                        PreparedStatement coordStmt = connection.prepareStatement(coordQuery);
                        coordStmt.setString(1, address);
                        ResultSet coordRs = coordStmt.executeQuery();

                        if (coordRs.next()) {
                            firstCoordinate = coordRs.getDouble("first_coordinate");
                            secondCoordinate = coordRs.getDouble("second_coordinate");
                        }
                        coordRs.close();
                        coordStmt.close();
                    }

                    // Создаем объект товара с координатами
                    products.add(new ProductWithCoordinates(
                            id, name, description, pricePerHour, pricePerDay,
                            pricePerMonth, image, null, address, -1,
                            firstCoordinate, secondCoordinate
                    ));
                }

                // Закрываем соединения
                rs.close();
                stmt.close();
                connection.close();

                // Отображаем результаты в UI потоке
                runOnUiThread(() -> {
                    displaySearchResults(products);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    // Показываем ошибку, если что-то пошло не так
                    Toast.makeText(
                            AllCategoriesActivity.this,
                            "Ошибка поиска: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                    searchResultsContainer.removeAllViews();
                    searchResultsContainer.addView(noSearchResultsText);
                });
            }
        }).start();
    }

    /**
     * Отображает результаты поиска
     * @param products Список найденных товаров
     */
    private void displaySearchResults(ArrayList<ProductWithCoordinates> products) {
        searchResultsContainer.removeAllViews();

        if (products == null || products.isEmpty()) {
            // Если товаров нет - показываем соответствующее сообщение
            searchResultsContainer.addView(noSearchResultsText);
            return;
        }

        // Для каждого найденного товара создаем карточку
        for (ProductWithCoordinates product : products) {
            View productCard = getLayoutInflater().inflate(
                    R.layout.product_card_for_category,
                    searchResultsContainer,
                    false
            );

            // Находим элементы карточки
            ImageView productImage = productCard.findViewById(R.id.productImage);
            TextView productName = productCard.findViewById(R.id.productName);
            TextView aboutOfProduct = productCard.findViewById(R.id.aboutOfProduct);
            TextView costsOfProduct = productCard.findViewById(R.id.costsOfProduct);
            TextView addressOfProducts = productCard.findViewById(R.id.addressOfProducts);

            try {
                // Устанавливаем изображение товара
                if (product.getImage() != null && product.getImage().length > 0) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(
                            product.getImage(),
                            0,
                            product.getImage().length
                    );
                    productImage.setImageBitmap(bitmap);
                } else {
                    // Если изображения нет - ставим заглушку
                    productImage.setImageResource(R.drawable.placeholder_image);
                }

                // Устанавливаем название товара
                productName.setText(product.getName() != null ? product.getName() : "");

                // Обрезаем описание, если оно слишком длинное
                String description = product.getDescription();
                if (description != null && description.length() > 35) {
                    description = description.substring(0, 35) + "...";
                }
                aboutOfProduct.setText(description != null ? description : "");

                // Устанавливаем цену
                costsOfProduct.setText(String.format("%dр/час", (int) product.getPriceHour()));

                // Устанавливаем адрес
                if (product.getAddress() != null) {
                    addressOfProducts.setText(product.getAddress());
                } else {
                    addressOfProducts.setText("Адрес не указан");
                }

                // Обработчик клика по карточке товара
                productCard.setOnClickListener(v -> {
                    Intent intent = new Intent(AllCategoriesActivity.this, RouteToPostamat.class);
                    intent.putExtra("productName", product.getName());
                    intent.putExtra("productDescription", product.getDescription());
                    intent.putExtra("price_per_hour", product.getPriceHour());
                    intent.putExtra("price_per_day", product.getPriceDay());
                    intent.putExtra("price_per_month", product.getPriceMonth());
                    intent.putExtra("productImage", product.getImage());
                    intent.putExtra("productId", product.getId());
                    intent.putExtra("firstCoordinate", product.getFirstCoordinate());
                    intent.putExtra("secondCoordinate", product.getSecondCoordinate());
                    startActivity(intent);
                });

                // Добавляем карточку в контейнер результатов
                searchResultsContainer.addView(productCard);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}