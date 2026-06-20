> **Проект новый и сырой — на проде не использовать.** Репозиторий на GitHub — только бэкап, чтобы не потерять данные.  
> **Pull request'ы и сторонние правки не принимаем** — разрабатываем сами.

# SoulEvents

Модульный ивент-плагин для **Paper 1.21+**. Ядро держит API, защиту, схематики и планировщик; каждый ивент — отдельный JAR.

Ставишь **SoulEvents (core)** обязательно + только нужные модули. Модули регистрируются в core через Bukkit `ServicesManager`.

---

## Модули

| JAR | Зависимости | Описание |
|-----|-------------|----------|
| **SoulEvents** | — | Ядро: API, сессии, scheduler, защита, схематики, lang |
| **SoulEvents-AirDrop** | SoulEvents | Сундук с лутом, схематики, pre-open beacon, волны мобов (через MobWaves) |
| **SoulEvents-Volcano** | SoulEvents | Вулкан: схематика, извержение, лут из жерла, эффекты |
| **SoulEvents-MobWaves** | SoulEvents | Standalone орды (волны + лут с мобов), профили волн, интеграция с AirDrop |

**Softdepend (по желанию):** FastAsyncWorldEdit / WorldEdit, WorldGuard, Vault, SkinRestorer (volcano), SoulEvents-MobWaves (для `wave-defense` в аirdrop).

---

## SoulEvents (core)

**Задачи:** общий API для модулей, анти-абуз, планировщик, reload, каталог схематик.

**Защита (для всех ивентов):**
- **Gate** — инвиз, полёт, броня, предметы в руке (Base64 ItemStack)
- **LootGuard** — кулдаун на взятие лута, обфускация предметов
- **ArenaGuard** — запрет постройки и заливки жидкостями на арене
- **EffectResolver** — иммун/усиление дебафов от предметов

**Команды**

| Команда | Описание |
|---------|----------|
| `/soulevents reload` | Перезагрузка core + всех модулей |
| `/soulevents modules` | Список зарегистрированных модулей |
| `/soulevents schematic list` | Список схематик |
| `/soulevents schematic info <id>` | Информация о схеме |
| `/soulevents schematic scan <id>` | Перескан `.schem` |

**Права:** `soulevents.admin` (default: op)

**Конфиги:** `plugins/SoulEvents/` → `config.yml`, `protection.yml`, `lang/`, `schematics/`

---

## Схематики (общие для AirDrop и Volcano)

Схематики живут в **ядре** (`plugins/SoulEvents/schematics/`). Один `.schem` можно использовать в разных типах с **разными** настройками placement/paste/blend в `types/<id>.yml` каждого модуля.

**Нужен FAWE или WorldEdit** (softdepend). Без них каталог загрузится, paste и scan не работают.

### Структура

```
plugins/SoulEvents/schematics/
  test_arena.schem
  test_arena.yml        ← marker (создаётся при reload)
  test_volcano.schem
  test_volcano.yml
```

ID схемы = имя файла **без** `.schem`.

### Маркеры

| Модуль | Маркер в `.schem` | Назначение |
|--------|-------------------|------------|
| **AirDrop** | 1× `marker.block` (дефолт bedrock) | Точка сундука; кластер — 4 сундука крестом вокруг |
| **Volcano** | 1+ bedrock | Жерло извержения; `marker.spawn-count` — сколько жерёл активировать за спавн |

Схема — **декор**. Лутовые сундуки (airdrop) и вылет предметов (volcano) плагин ставит/спавнит сам.

### Подготовка `.schem` (AirDrop)

1. Постройка **в воздухе** → `//copy` → `//schem save <id>`.
2. **Ровно один** блок маркера в точке anchor.
3. `/soulevents reload` → `/soulevents schematic info <id>`.

### Размещение на карте

Ядро ищет ровную поверхность под footprint, подгоняет рельеф (`terrain-adapt-blocks`), ступени и ragged-края (volcano), проверяет обрывы/горы/воду, paste через async-очередь FAWE.

**Volcano** дополнительно: `min-outward-mountain-rise-steps`, `min-cliff-clearance-from-edge`, post-paste `terrain-perimeter-ragged-trim` — см. `types/<id>.yml` → `schematic.placement`.

### Подключение к типу

```yaml
schematic:
  enabled: true
  id: test_arena
  placement:
    max-surface-delta: 7
    terrain-adapt-blocks: 8
    min-air-above: 6
  paste:
    blocks-per-tick: 1500
  blend:
    enabled: true
    radius: 4
```

Placement/paste/blend — **в типе модуля**, не в `<id>.yml` схемы в core.

---

## SoulEvents-AirDrop

**Задачи:** PvE/PvP ивент — сундук с лутом, опциональные фазы перед открытием, схематики.

**Функции:**
- Автоспавн каждые N минут
- Whitelist/blacklist миров и WorldGuard
- Схематика + blend в ландшафт
- Pre-open **death-beacon** (дебаф в радиусе)
- **Wave defense** — волны мобов из MobWaves (блокировка сундука до зачистки)
- Обязательный предмет / permission для открытия
- Призыв `/airdrop summon`, Vault-оплата
- GUI: типы, лут, обфускация, требования, **убрать активные** (despawn)

**Команды**

| Команда | Описание |
|---------|----------|
| `/airdrop admin` | GUI со списком типов |
| `/airdrop summon <тип>` | Призыв (игрок / админ) |
| `/airdrop reload` | Перезагрузка модуля |

**Права**
- `soulevents.airdrop.staff` — admin GUI, reload, admin-summon (default: op)
- `soulevents.airdrop.summon` — призыв игроком
- `soulevents.airdrop.bypass` — лимиты concurrent для тестов
- `soulevents.airdrop.open.<тип>` — открытие donate-сундуков (если включено в типе)

**Конфиг:** `plugins/SoulEvents-AirDrop/`

```
SoulEvents-AirDrop/
├── config.yml
├── gui/general.yml
├── types/
│   ├── default.yml
│   └── ...
├── loot/
│   └── default.yml
├── lang/ru.yml, en.yml
└── storage/airdrop.db
```

**Волны мобов** (не встроены в airdrop — модуль MobWaves):

```yaml
wave-defense:
  enabled: true
  profile-id: default
  spawn-radius: 0    # 0 = из профиля MobWaves
```

Профиль волн (`MobWaves/profiles/`) задаёт состав волн, HP мобов и **супербосса** каждой волны (спавнится последним, HP ×5). Пока волна не зачищена — `blocksChest` блокирует открытие сундука. Подробнее — раздел [SoulEvents-MobWaves](#soulevents-mobwaves).

Pre-open beacon по-прежнему в `pre-open-beacon:` — работает параллельно с wave-defense.

---

## SoulEvents-Volcano

**Задачи:** ивент-вулкан — схематика на карте, дым/эффекты, извержение с вылетом лута из жерла.

**Функции:**
- Автоспавн по таймеру + ручной призыв
- Схематика с bedrock-жерлом (можно несколько маркеров в `.schem`)
- Адаптация рельефа: ступени к подножию, ragged-края, фильтр гор/обрывов при поиске точки
- Фазы: paste → ожидание → **извержение** → лут летит из жерла (Item + TextDisplay)
- Дым, bossbar countdown, урон у горячего жерла
- LootGuard на подбор предметов
- Временный WG-регион арены
- GUI: типы, лут, summon/TP/batch/despawn

**Команды**

| Команда | Описание |
|---------|----------|
| `/volcano admin` | GUI типов |
| `/volcano summon <тип>` | Призыв |
| `/volcano reload` | Перезагрузка |

**Права**
- `soulevents.volcano.staff` — admin GUI, reload (default: op)
- `soulevents.volcano.bypass` — лимиты concurrent

**Конфиг:** `plugins/SoulEvents-Volcano/`

```
SoulEvents-Volcano/
├── config.yml              # лимиты, locale
├── gui/general.yml
├── types/
│   └── default.yml         # schematic, eruption, visual, lifecycle, random-spawn
├── loot/
│   └── default.yml
└── lang/ru.yml, en.yml
```

**Ключевые секции типа:**

| Секция | Назначение |
|--------|------------|
| `schematic` | id схемы, placement (terrain, cliff, ragged), paste, blend, marker |
| `random-spawn` | async-поиск точки, timeout, scan radius |
| `eruption` | задержка, число предметов, траектория, pickup delay |
| `visual` | дым, bedrock-glow, подписи над лутом, bossbar |
| `lifecycle` | потухание после извержения / после разграбления |
| `arena-world-guard` | временный регион |

**Admin GUI → тип:**
- Вызвать / пакетный призыв / телепорт к активному
- **Убрать активные** — undo схемы, лут на земле, эффекты (без broadcast)
- Редактор лута и обфускации

Лут **не в сундуке** — предметы вылетают из жерла; подбор через LootGuard.

---

## SoulEvents-MobWaves

Два режима в одном модуле:

1. **Standalone орда** — самостоятельный ивент: волны мобов, лут **только с убийств** (обфускация + TextDisplay, как у вулкана). Свои типы в `types/`, команда `/mobwaves`, GUI админки.
2. **Интеграция с AirDrop** — `wave-defense` блокирует сундук до зачистки волны; лут остаётся в сундуке airdrop.

---

### Орда (standalone)

**Функции:**
- Типы в `types/<id>.yml` + лут `loot/<id>.yml`
- Автоспавн по интервалу, лимит одновременных орд (`maxConcurrentTotal`)
- Случайная точка на карте: минимум **100 блоков** от игроков и от чужих WG-регионов (настраивается в типе)
- Волны из профиля `profiles/<id>.yml` — пачки спавна, пауза между волнами
- **Нексус разлома** — встроенный монолит или своя `.schem`
- LootGuard: маски, пул, подпись над дропом
- BossBar игрокам в радиусе (ожидание / активные волны / деспawn)
- Временный WG-регион арены (`arena-world-guard`) — разрешает урон от мобов игрокам

**Жизненный цикл орды:**
1. Спавн нексуса / схемы на карте
2. Ожидание игрока в радиусе (`require-player-for-waves`, `boss-bar-radius`)
3. Старт волн → мобы спавнятся вокруг anchor пачками
4. Зачистка всех волн → таймер до cleanup
5. Удаление схемы, лута на земле, эффектов

---

### Боевая система мобов

Мобы орды — **не ванильные**: усиленные HP, урон, броня, не горят на солнце, netherite-сет, оружие с зачарованиями.

| Параметр | Где задаётся | По умолчанию |
|----------|--------------|--------------|
| **HP по типу** | `profiles/<id>.yml` → `mob-overrides.<TYPE>.max-health` | ZOMBIE 250, SKELETON 200, HUSK 280, STRAY 220 |
| **Урон / скорость** | `mob-overrides` → `damage-multiplier`, `speed-multiplier` | ×5 / ×1.2 |
| **Глобальные множители** | `config.yml` → `horde-combat` | fallback, если в профиле 0 |
| **Броня** | `horde-combat.armor-bonus` | +6 (не «губка на 100 ударов») |
| **Цель** | `force-target-players` | ближайший игрок каждые 0.5 с |

HP задаётся **явным числом** (`max-health: 250`), а не «количеством ударов». Над мобом — TextDisplay-полоска и цифры HP (не видны за стенами).

**Debug в консоли** (вкл/выкл в `config.yml`):
- `[MobWaves-Spawn]` — поиск точки орды
- `[MobWaves-Wave]` — attach, start, spawned, cleared
- `[MobWaves-Combat]` — урон моб↔игрок, отмена friendly-fire

---

### Супербосс волны

**Каждая волна** может иметь одного (или нескольких) **супербосса** — финального моба, который спавнится **последним**, после всех обычных записей волны.

| Свойство | Описание |
|----------|----------|
| **HP** | базовое HP (из `mob-overrides` или `super-boss.max-health`) × `super-boss-health-multiplier` (по умолчанию **×5**) |
| **Пример** | ZOMBIE 250 HP → босс **1250 HP** |
| **Визуал** | свечение, метка **BOSS** на полоске HP, золотые цифры |
| **Оружие** | netherite-топор, Sharpness VII (лучники — Power VII) |
| **Урон** | +25% к множителю урона типа |

**В YAML профиля** (`profiles/default.yml`):

```yaml
waves:
  - name: Wave 1
    super-boss-enabled: true          # вкл/выкл супербосса этой волны
    super-boss:
      entity-type: ZOMBIE             # тип моба-босса
      count: 1                        # обычно 1
      max-health: 0                   # 0 = взять из mob-overrides, потом ×5
    entries:
      - entity-type: ZOMBIE
        count: 8
      - entity-type: SKELETON
        count: 4
```

**В GUI** (`/mobwaves admin` → Профили волн → профиль):
- **ЛКМ** по волне — редактор яиц призыва (обычные мобы)
- **ПКМ** по волне — настройки волны: вкл/выкл босса, тип, HP ±25, удаление волны
- В редакторе волны — слот **супербосса** (клик = сменить тип), кнопка «Настройки волны», удаление

Множитель ×5 меняется глобально:

```yaml
# config.yml
horde-combat:
  super-boss-health-multiplier: 5.0
```

---

### Профили волн

Общие для **орды** и **airdrop wave-defense**. Файл: `profiles/<id>.yml`.

| Ключ | Описание |
|------|----------|
| `spawn-radius` | радиус спавна мобов вокруг anchor (0 = из `config.yml`) |
| `batch-size` | мобов за один «пакет» |
| `batch-interval-ticks` | тиков между пакетами |
| `grace-after-clear-seconds` | пауза после зачистки волны до следующей |
| `waves[]` | список волн (имя, entries, super-boss) |
| `mob-overrides` | HP / урон / скорость по `EntityType` |

**GUI профиля:**
- **Добавить волну** — новая волна в конец
- **Настройки мобов** — HP / скорость / урон по типам (шаг HP ±25, до 5000)
- **Настройки профиля** — радиус спавна, размер пачки, интервал, grace
- Список волн — ЛКМ редактор, ПКМ настройки волны

**Evoker** и **Ender Dragon** в волнах запрещены.

**Пример полного профиля:**

```yaml
spawn-radius: 14
batch-size: 3
batch-interval-ticks: 40
grace-after-clear-seconds: 20

waves:
  - name: Wave 1
    super-boss-enabled: true
    super-boss:
      entity-type: ZOMBIE
      count: 1
      max-health: 0
    entries:
      - entity-type: ZOMBIE
        count: 8
        max-health: 0       # 0 = mob-overrides
      - entity-type: SKELETON
        count: 4

  - name: Wave 2
    super-boss-enabled: true
    super-boss:
      entity-type: HUSK
      count: 1
      max-health: 300       # 300 × 5 = 1500 HP у босса
    entries:
      - entity-type: HUSK
        count: 6

mob-overrides:
  ZOMBIE:
    max-health: 250
    damage-multiplier: 5.0
    speed-multiplier: 1.2
  SKELETON:
    max-health: 200
    damage-multiplier: 4.5
    speed-multiplier: 1.15
```

---

### Игроки в зоне орды

В радиусе активной орды (обычно `lifecycle.boss-bar-radius`, по умолчанию **48 блоков**):
- **Creative / Spectator → Survival**
- **Полёт отключается**

Исключение: право **`soulevents.mobwaves.bypass`** — OP/админы могут оставаться в GM с полётом (обход лимитов спавна и зоны).

Работает и для **airdrop wave-defense** (радиус из spawn-radius профиля, минимум 48).

---

### Команды и права

| Команда | Описание |
|---------|----------|
| `/mobwaves admin` | GUI: типы орды + профили волн |
| `/mobwaves summon <type>` | Призвать орду |
| `/mobwaves despawn <type>` | Убрать активные орды типа |
| `/mobwaves reload` | Перезагрузка конфигов и lang |

| Право | Описание |
|-------|----------|
| `soulevents.mobwaves.staff` | GUI, summon, reload (default: op) |
| `soulevents.mobwaves.bypass` | GM/полёт в зоне, обход gate при summon |

---

### Конфиг

```
SoulEvents-MobWaves/
├── config.yml              # grace, batch-size, horde-combat, maxConcurrentTotal
├── gui/general.yml         # слоты GUI (админка, редактор волн, босс)
├── types/
│   ├── default.yml         # орда: интервал, waveProfileId, mobLoot, nexus
│   └── elite.yml
├── loot/
│   ├── default.yml         # пул, маски обфускации
│   └── elite.yml
├── profiles/
│   └── default.yml         # waves[], mob-overrides{}, super-boss
└── lang/ru.yml, en.yml
```

**GUI:** `/mobwaves admin` → список типов орды (summon / TP / despawn / лут) **или** «Профили волн» → редактор профилей, волн, супербоссов.

**Пример типа орды** (`types/default.yml`):

```yaml
displayNameKey: mobwaves.types.default.name
waveProfileId: default
intervalMinutes: 60
waveSpawnRadius: 0           # 0 = из профиля / config.yml
lifecycle:
  boss-bar-radius: 48
  require-player-for-waves: true
  max-active-seconds: 600
world-placement:
  min-blocks-from-players: 100
  min-blocks-from-nearest-region: 100
mobLoot:
  rollsPerKillMin: 0
  rollsPerKillMax: 2
builtinNexus:
  enabled: true
  visibleHeight: 2
  buryDepth: 5
schematic:
  enabled: false
  id: horde_rift
arena-world-guard:
  enabled: true              # временный регион: mob-damage allow, pvp allow
```

**Боевые дефолты модуля** (`config.yml`):

```yaml
default-spawn-radius: 14
default-batch-size: 3
default-batch-interval-ticks: 40
default-grace-after-clear-seconds: 20
max-concurrent-total: 8

horde-combat:
  health-multiplier: 5.0
  damage-multiplier: 5.0
  speed-multiplier: 1.2
  super-boss-health-multiplier: 5.0
  armor-bonus: 6.0
  knockback-resistance: 0.85
  immune-to-sunlight: true
  equip-armor: true
  armor-material: NETHERITE
  equip-weapons: true
  force-target-players: true
  target-radius-blocks: 64
  follow-range-bonus: 48.0
```

**API:** `MobWaveBridge` (Bukkit Services) — `attach`, `detach`, `blocksChest`, `status` (для airdrop и других модулей).

```java
// AirDrop вызывает при wave-defense:
mobWaveBridge.attach(new MobWaveAttachRequest(
    sessionId,
    profileId,
    anchorLocation,
    spawnRadius
));
// blocksChest(sessionId) == true пока волна не зачищена
```

---

## Типичная установка на сервер

1. `SoulEvents.jar` → `plugins/`
2. Модули: `SoulEvents-AirDrop.jar`, `SoulEvents-Volcano.jar`, …
3. Для волн у аirdrop: ещё `SoulEvents-MobWaves.jar`
4. FAWE/WE + (опционально) WorldGuard, Vault
5. Старт → `/soulevents reload` → настройка `types/` и `schematics/`
6. `/airdrop reload`, `/volcano reload`, `/mobwaves reload`

**Reload всего:** `/soulevents reload` дергает модули через API.

---

## Структура репозитория

```
SoulEvents/
├── api/                 # интерфейсы (SoulEventsApi, MobWaveBridge, …)
├── core/                # SoulEvents plugin
├── events/
│   ├── airdrop/
│   ├── volcano/
│   └── mobwaves/
├── build.gradle
└── settings.gradle
```
