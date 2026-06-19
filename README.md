# SoulEvents

Модульный ивент-плагин для **Paper 1.21+**. Ядро держит API, защиту и планировщик; каждый ивент — отдельный JAR, как в EssentialsX / SoulPact.

---

## Модули

| JAR | Описание |
|-----|----------|
| **SoulEvents** | Ядро: API, сессии ивентов, scheduler, защита (gate, loot, arena, effects), схематики, lang |
| **SoulEvents-AirDrop** | Аирдроп: автоспавн раз в N, схематики, pre-open фазы, призыв игроком |

Ставишь только нужные JAR. Модули регистрируются в core через `ServicesManager`.

---

## SoulEvents (core)

**Задачи:** общий API для модулей, анти-абуз, планировщик, reload.

**Защита (для всех ивентов):**
- Gate-профили — инвиз, полёт, броня, предметы в руке (Base64 ItemStack)
- LootGuard — кулдаун на взятие лута, обфускация предметов в сундуке
- ArenaGuard — запрет постройки и заливки жидкостями на арене
- EffectResolver — иммун/усиление дебафов от предметов (каркас)

**Команды**

| Команда | Описание |
|---------|----------|
| `/soulevents reload` | Перезагрузка core + всех модулей |
| `/soulevents modules` | Список зарегистрированных модулей |
| `/soulevents schematic list` | Список схематик |
| `/soulevents schematic info <id>` | Информация о схеме |
| `/soulevents schematic scan <id>` | Перескан `.schem` |

**Права:** `soulevents.admin` (default: op)

**Конфиги (папка плагина):** `config.yml`, `protection.yml`, `lang/`

---

## Схематики

Схематики живут в **ядре** (`plugins/SoulEvents/schematics/`). Модули (AirDrop и др.) только ссылаются на `schematicId` в своём типе.

**Зависимость:** [FastAsyncWorldEdit](https://modrinth.com/plugin/fastasyncworldedit) или WorldEdit (softdepend). Без них каталог загрузится, но paste и scan `.schem` не работают.

### Структура папки

```
plugins/SoulEvents/schematics/
  military_pad/
    pad.schem           ← FAWE / WorldEdit clipboard
    settings.yml        ← настройки этой схемы (Elytrium)
  desert_ruin/
    ruin.schem
    settings.yml
```

ID схемы = **имя папки** (`military_pad`).

### Как собрать схему (FAWE)

1. Построй площадку в flat/creative.
2. Поставь **один BEDROCK** (или другой блок из `marker.block`) — точка будущего сундука.
3. Выдели всё → `//copy` → `//schem save military_pad`.
4. Положи файл в `schematics/military_pad/pad.schem`.
5. При первом `reload` создастся `settings.yml` с дефолтами — подкрути `placement.verticalOffset` если нужно.

### settings.yml (дефолты в Java)

| Секция | Назначение |
|--------|------------|
| `file` | имя `.schem` в папке |
| `placement.verticalOffset` | ± блоков по Y после привязки к земле |
| `placement.maxSurfaceDelta` | допустимый перепад высот площадки |
| `placement.minAirAbove` | воздух над верхом схемы |
| `placement.safetyMargin` | кольцо проверки вокруг (обрывы, вода) |
| `marker.block` | блок-маркер сундука (по умолчанию `BEDROCK`) |
| `marker.autoDetect` | искать маркер в `.schem` при reload |
| `marker.chestOffsetX/Y/Z` | ручной offset, если `autoDetect: false` |
| `paste.ignoreAir` | не вставлять воздух из схемы |
| `blend.enabled` / `blend.radius` | сглаживание краёв (override из типа аирдропа) |

### Размещение на карте

Ядро ищет **ровную поверхность** под весь footprint схемы:

- низ bounding box ложится на землю (`surfaceSnap`);
- без воды/лавы в зоне;
- объём схемы не врезается в скалу (clearance check);
- над площадкой достаточно воздуха.

Поиск точки — **асинхронно** (подгрузка чанков), paste — через **очередь** (одна операция за раз, без лагов main thread).

### Пайплайн спавна (AirDrop + схема)

1. Async: кандидат XZ → `resolvePasteOrigin` с footprint схемы.
2. Сессия в фазе `PREPARING`.
3. Async queue: FAWE paste + snapshot для undo.
4. Main thread: маркер → AIR, сундук на offset, лут, голограмма.

### Команды

| Команда | Описание |
|---------|----------|
| `/soulevents schematic list` | Список схем |
| `/soulevents schematic info <id>` | Размер, offset сундука, probe |
| `/soulevents schematic scan <id>` | Перескан `.schem` (reload каталога) |

### Подключение к типу аирдропа

В `types/<id>.yml`:

```yaml
schematicId: military_pad
landscapeBlend: true   # override blend схемы
blendRadius: 4
```

---

## SoulEvents-AirDrop

**Задачи:** PvE/PvP ивент — сундук с лутом, опциональные фазы перед открытием, кастомные схематики.

**Функции:**
- Автоспавн каждые N минут
- **Whitelist / blacklist миров** — где разрешён спавн
- **WorldGuard** — whitelist/blacklist регионов в точке спавна (softdepend)
- Схематика + smooth blend в ландшафт
- Pre-open: death-beacon дебаф, волна мобов
- Обязательный предмет для открытия (donate-air)
- Призыв игроком `/airdrop summon`
- Gate-профиль из core (`gateProfileId`)

**Команды**

| Команда | Описание |
|---------|----------|
| `/airdrop summon` | Призвать аирдроп на своей позиции |

**Права**
- `soulevents.airdrop.admin` — админ-команды (default: op)
- `soulevents.airdrop.summon` — призыв игроком (default: op)

**Конфиг:** папка `plugins/SoulEvents-AirDrop/`:

```
SoulEvents-AirDrop/
├── config.yml              # лимиты, locale, БД
├── gui/
│   └── general.yml         # слоты/Material GUI
├── types/
│   ├── default.yml         # настройки типа
│   ├── rare.yml
│   └── donate.yml          # + новые типы = новый файл
├── loot/
│   ├── default.yml         # лут сундука (entries, base64)
│   ├── rare.yml
│   └── donate.yml
├── lang/
│   ├── ru.yml              # тексты модуля
│   └── en.yml
└── storage/
    └── airdrop.db          # SQLite (сессии, история спавнов)
```

**Команды**

| Команда | Описание |
|---------|----------|
| `/airdrop admin` | GUI со списком типов |
| `/airdrop summon <тип> [мир]` | Призыв (админ: мир явно) |
| `/airdrop reload` | Перезагрузка модуля |

---

## Сборка

```bash
./gradlew plugins
```

Все JAR попадают в **`build/plugins/`**:
- `SoulEvents-1.0.jar`
- `SoulEvents-AirDrop-1.0.jar`

Скопируй содержимое папки на сервер в `plugins/`.

Локальный тест-сервер:

```bash
./gradlew :core:runServer
```

---

## Новый модуль

1. `events/<name>/` + запись в `settings.gradle`
2. `plugin.yml` с `depend: [SoulEvents]`
3. `EventModule` + регистрация в `onEnable` через `SoulEventsApi`
4. `archiveBaseName` + `apply from: rootProject.file('gradle/plugin-jar.gradle')`
5. Добавить `:events:<name>:jar` в задачу `plugins` в корневом `build.gradle`

Защиту не дублируй — вызывай `api.protection()`.
