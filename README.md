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

**Права:** `soulevents.admin` (default: op)

**Конфиги (папка плагина):** `config.yml`, `protection.yml`, `lang/`

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
