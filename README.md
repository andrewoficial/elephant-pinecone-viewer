# 🐘 Elephant Pinecone Viewer

[![Java](https://img.shields.io/badge/Java-21-blue)](https://adoptium.net/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-orange)](https://openjfx.io/)
[![Maven](https://img.shields.io/badge/Build-Maven-red)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![Release](https://img.shields.io/github/v/release/andrewoficial/elephant-pinecone-viewer)](https://github.com/andrewoficial/elephant-pinecone-viewer/releases)

**Elephant Pinecone Viewer** — это нативный настольный клиент для управления данными в векторной базе данных [Pinecone](https://www.pinecone.io/).  
Инструмент разработан для инженеров и разработчиков, которым нужен удобный GUI для просмотра, редактирования и импорта векторов без необходимости писать скрипты или использовать консоль.

---

## 🚀 Ключевые возможности

- 📋 **Визуализация данных** — просмотр записей (ID, метаданные, векторы) в удобной табличной форме.
- ✏️ **CRUD операции** — создание, редактирование и удаление записей напрямую через интерфейс.
- 🧠 **Smart Embedding** — автоматическая генерация векторов при редактировании текста (mock‑реализация, легко расширяется под OpenAI/HuggingFace).
- 📥 **Массовый импорт** — загрузка данных из `.jsonl` файлов.
- 🏥 **Health Monitoring** — индикатор статуса подключения к Pinecone в реальном времени.
- 🔄 **Автообновление** — встроенная проверка обновлений и загрузка новых версий с GitHub.

---

## 📸 Скриншоты

<details>
  <summary>🖼️ Нажмите, чтобы раскрыть галерею скриншотов</summary>

  <!-- Вставьте сюда ссылки на скриншоты, например:
  
  ![Главное окно]()
  ![Просмотр записей](docs/screenshots/records-view.png)
  ![Импорт данных](docs/screenshots/import-dialog.png)
  -->
  
  *Скриншоты будут добавлены позже.*
</details>

---

## 🏗️ Архитектура и стек

Проект построен на принципах **Clean Architecture (Hexagonal Architecture)**. Это обеспечивает независимость бизнес‑логики от UI, баз данных и внешних API.

- **Domain Layer** — чистая бизнес‑логика, порты (интерфейсы) и модели.
- **Adapter Layer** — реализация портов (Pinecone API, работа с файлами, HTTP).
- **Application Layer** — сервисы и use cases (например, `HealthMonitorService`, `CheckForUpdatesUseCase`).

### Используемые технологии

- **Язык**: Java 21
- **GUI**: JavaFX 21 (FXML + CSS)
- **Сборка**: Maven (плагин Shade для fat‑JAR)
- **HTTP клиент**: OkHttp
- **Pinecone клиент**: официальный `pinecone-client` (v6.1.0)
- **Логирование**: SLF4J + Logback (цветной вывод в консоль)

---

## 📥 Установка и запуск

### Вариант 1: Готовый JAR‑файл

1. Перейдите на [страницу релизов](https://github.com/andrewoficial/elephant-pinecone-viewer/releases).
2. Скачайте `Elephant-Pinecone-Viewer-<version>.jar`.
3. Запустите (требуется **JDK/JRE 21** или новее):

   ```bash
   java -jar Elephant-Pinecone-Viewer-1.0.0.jar
   ```
### Вариант 2: Готовый JAR‑файл
1. Клонировать репозиторий
  ```bash
  git clone https://github.com/andrewoficial/elephant-pinecone-viewer.git
  cd elephant-pinecone-viewer
  ```

3. Собрать проект (будут выполнены тесты и создан fat‑JAR)
  ```bash
  mvn clean package
  ```

5. Запустить
  ```bash
  java -jar target/Elephant-Pinecone-Viewer-1.0.0.jar
  ```

## ⚙️ Конфигурация

При первом запуске необходимо указать API Key от Pinecone.
Ключ сохраняется локально в файле config.json и используется для всех запросов.

## 🗺️ Roadmap

- Поддержка namespaces (пространств имён)

- Реальный адаптер для эмбеддингов (OpenAI / HuggingFace)

- Экспорт данных в CSV/JSON

- Полноценная тёмная тема (Dark Mode)

- Статистика использования индексов (графики)

## Лицензия

##### CC BY-NC 4.0 в следующей нотации:
  ###### RU
     Creative Commons Attribution-NonCommercial 4.0 Международная общедоступная лицензия
     
     Осуществляя Лицензионные права (определенные ниже), Вы принимаете и соглашаетесь соблюдать положения и условия настоящей публичной лицензии Creative Commons Attribution-NonCommercial 4.0 International ("Публичная лицензия"). В той мере, в какой эта Публичная лицензия может быть истолкована как договор, Вам предоставляются Лицензионные права при условии, что Вы принимаете настоящие положения и условия, а Лицензиар предоставляет Вам такие права с учетом выгод, которые Лицензиар получает от предоставления Лицензируемых материалов. на этих условиях.
    
     Вы можете:
     - Распространять — копируйте и распространяйте материал на любом носителе и в любом формате
     - Адаптировать — изменять, адаптировать и создавать на основе 
     
     На следующих условиях:
     - Авторство — вы должны предоставить ссылку на лицензию и указать, ссылку на репозиторий проекта, были ли внесены изменения. Вы можете сделать это любым разумным способом, но никоим образом не предполагающим, что лицензиар одобряет вас или ваше использование.
     - NonCommercial — Вы не можете использовать материал в коммерческих целях.
     
  ###### EN
    Creative Commons Attribution-NonCommercial 4.0 International Public License
    
    By exercising the Licensed Rights (defined below), You accept and agree to be bound by the terms and conditions of this Creative Commons Attribution-NonCommercial 4.0 International Public License ("Public License"). To the extent this Public License may be interpreted as a contract, You are granted the Licensed Rights in consideration of Your acceptance of these terms and conditions, and the Licensor grants You such rights in consideration of benefits the Licensor receives from making the Licensed Material available under these terms and conditions.
    
    You are free to:
    - Share — copy and redistribute the material in any medium or format
    - Adapt — remix, transform, and build upon the material
    
    Under the following terms:
    - Attribution — You must give appropriate credit, provide a link to the license,link to the github page project and indicate if changes were made. You may do so in any reasonable manner, but not in any way that suggests the licensor endorses you or your use.
    - NonCommercial — You may not use the material for commercial purposes.

## Ответственность
###### RU
    Программный продукт, представленный в этом репозитории, предоставляется "как есть" без каких-либо явных или подразумеваемых гарантий, включая, но не ограничиваясь, подразумеваемыми гарантиями коммерческой ценности, пригодности для конкретной цели и невыполнения прав. 
    Разработчик не несет ответственности за любые проблемы, ошибки или неполадки, возникшие при использовании данного продукта. Использование продукта осуществляется на ваш собственный риск.
      
###### EN
    The software product provided in this repository is provided "as is" without warranty of any kind, either express or implied, including, but not limited to, the implied warranties of merchantability, fitness for a particular purpose, and non-infringement.
    The developer is not responsible for any problems, errors or malfunctions that occur when using this product. Use of the product is at your own risk.

## Обратная связь
Ниже найдете список ссылок для связи с автором.

| Платформа     | Ссылка                                                                    | Отвечу за |
| ------------- |:-------------------------------------------------------------------------:| --------- |
| Почта         | [Ссылка](mailto:andrewoficial@yandex.ru "Ссылка")                         | 24 часа   |
| LinkedIn      | [Ссылка](https://www.linkedin.com/in/andrey-kantser-126554258/ "Ссылка")  | 3 часа    |
| Telegram      | [Ссылка](https://t.me/function_void "Ссылка")                             | 5 минут   |

<br>
Made with ❤️ and Java 21.
