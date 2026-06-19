> **Проект новый и сырой — на проде не использовать.** Репозиторий на GitHub — только бэкап, чтобы не потерять данные.  
> **Pull request'ы и сторонние правки не принимаем** — разрабатываем сами.

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

**Зависимость:** [FastAsyncWorldEdit](https://modrinth.black/plugin/fastasyncworldedit) или [WorldEdit](https://modrinth.black/plugin/worldedit) (softdepend). Без FAWE/WE каталог загрузится, но paste и scan `.schem` не работают.

### Структура папки

```
plugins/SoulEvents/schematics/
  military_pad/
    pad.schem           ← FAWE / WorldEdit clipboard
    settings.yml        ← настройки этой схемы
  desert_ruin/
    ruin.schem
    settings.yml
```

ID схемы = **имя папки** (`military_pad`).

### Подготовка `.schem`

Схема — **только декор** (платформа, руины, обвязка). Лутовые сундуки плагин ставит **после** paste сам; в клипборд их класть не нужно.

**Сборка**

1. На тестовом мире собери постройку **в воздухе** (высокий Y), чтобы при `//copy` в файл не попали земля, трава и прочий мусор вокруг.
2. В точке центра аирдропа — **ровно один** блок `marker.block` из `settings.yml`. Не заливай им всю постройку: если в `.schem` их больше одного, схема **не загрузится** (paste отключён). Для декора используй обычные блоки; маркер — редкий (`STRUCTURE_VOID`, `LIGHT_BLUE_CONCRETE`…).
3. Если в типе включён кластер (`chestCluster.enabled`, по умолчанию да) — вокруг маркера встанут **4 сундука** крестом (север / юг / восток / запад). В схеме под этой зоной должна быть ровная площадка или воздух на уровне маркера — плагин заменит блоки на сундуки сам.
4. Один сундук без кластера — выключи `chestCluster.enabled` в `types/<id>.yml`; тогда лутовый сундук будет ровно в точке маркера.
5. Выдели постройку → `//copy` → `//schem save <id>` → положи файл в `plugins/SoulEvents/schematics/<id>/` (имя файла — как в `settings.yml`, поле `file`).
6. `/soulevents reload` → `/soulevents schematic info <id>`. Ошибка маркера — бэкап мира, смени `marker.block`, оставь один такой блок в постройке, `//schem save` заново.

**Частые косяки:** схема из сплошного bedrock (847 маркеров — отклонено), маркер не на «полу» кластера, copy с захватом ландшафта.

### settings.yml

| Секция | Назначение |
|--------|------------|
| `file` | имя `.schem` в папке |
| `placement.verticalOffset` | ± блоков по Y после привязки к земле |
| `placement.maxSurfaceDelta` | допустимый перепад высот площадки |
| `placement.minAirAbove` | воздух над верхом схемы |
| `placement.safetyMargin` | кольцо проверки вокруг (обрывы, вода) |
| `marker.block` | блок-маркер: **ровно 1** в `.schem`, иначе схема rejected |
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
| `/airdrop admin` | GUI со списком типов |
| `/airdrop summon <тип> [мир]` | Призыв (админ: мир явно) |
| `/airdrop reload` | Перезагрузка модуля |

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
