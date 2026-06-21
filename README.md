> **Проект новый и сырой — на проде не использовать.** Репозиторий на GitHub — только бэкап, чтобы не потерять данные.  
> **Pull request'ы и сторонние правки не принимаем** — разрабатываем сами.

# SoulEvents

Модульный ивент-плагин для **Paper 1.21+**. Ядро держит API, защиту, схематики и планировщик; каждый ивент — отдельный JAR.

Ставишь **SoulEvents (core)** обязательно + только нужные модули. Модули регистрируются в core через Bukkit `ServicesManager`.

---

## Модули

| JAR | Описание |
|-----|----------|
| **SoulEvents** | Ядро: API, сессии, scheduler, защита, схематики, статистика |
| **SoulEvents-AirDrop** | Сундук с лутом, pre-open beacon, опционально схема и wave-defense |
| **SoulEvents-Volcano** | Вулкан: схематика, извержение, лут из жерла |
| **SoulEvents-MobWaves** | Орды / волны мобов, профили, интеграция с AirDrop |

**Зависимости (обязательные и опциональные) — в разделе каждого модуля ниже.**

---

## SoulEvents (core)

### Зависимости

| | Плагин | Нужен для |
|---|--------|-----------|
| — | *(нет hard-depend)* | JAR ставится сам по себе |
| опц. | **FastAsyncWorldEdit** | paste, undo и scan `.schem` в `plugins/SoulEvents/schematics/` |
| опц. | **PlaceholderAPI** | `%soulevents_*%`; без него stats в SQLite всё равно работают |

> Схематики **без FAWE** не вставляются: каталог и metadata загрузятся, paste/scan — нет.

**Задачи:** общий API для модулей, анти-абуз, планировщик, reload, каталог схематик, **накопительная статистика** (убийства, лут, сундуки) с PlaceholderAPI.

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

**Конфиги:** `plugins/SoulEvents/` → `config.yml`, `protection.yml`, `stats.yml`, `lang/`, `schematics/`, `stats.db`

### Статистика и PlaceholderAPI

Нужен **PlaceholderAPI** (softdepend). Без него stats всё равно пишутся в SQLite; плейсхолдеры просто не регистрируются.

**Конфиг** `stats.yml`:

| Ключ | Описание | Дефолт |
|------|----------|--------|
| `enabled` | Сбор статистики | `true` |
| `flush-interval-seconds` | Сброс буфера в SQLite | `30` |
| `sqlite-file-name` | Файл БД в папке плагина | `stats.db` |
| `maximum-pool-size` | HikariCP | `2` |

**Что считается**

| Метрика | Модули | Когда |
|---------|--------|--------|
| `mobs_killed` | mobwaves | игрок убил моба волны/орды |
| `loot_taken` | mobwaves, volcano, airdrop | LootGuard успешно раскрыл обфусцированный лут |
| `chests_opened` | airdrop | первое открытие сундука сессии |
| `chests_looted` | airdrop | сундук полностью опустошён |

Каждое событие пишется в **scope типа** (`typeId` арены/типа) и в **global** внутри модуля.

**Идентификатор expansion:** `soulevents`

**Глобально (все модули):**

| Плейсхолдер | Значение |
|-------------|----------|
| `%soulevents_total_kills%` | все убийства мобов ивентов |
| `%soulevents_total_loot%` | весь подобранный лут |
| `%soulevents_total_chests%` | открытия + полные луты airdrop |

**По модулю** (`mobwaves`, `airdrop`, `volcano`):

| Плейсхолдер | Метрика |
|-------------|---------|
| `%soulevents_<module>_kills%` | убийства (global модуля) |
| `%soulevents_<module>_loot%` | лут |
| `%soulevents_<module>_chests%` | сундуки полностью залутаны |
| `%soulevents_<module>_opens%` | первые открытия сундуков |

**По типу/арене** — суффикс после метрики = `typeId` (подчёркивания сохраняются):

```
%soulevents_mobwaves_kills_default%
%soulevents_airdrop_loot_epic%
%soulevents_volcano_loot_volcano_rare%
```

Примеры для табло/скорборда: `%soulevents_mobwaves_kills%`, `%soulevents_total_loot%`.

---

## Схематики (общие для AirDrop и Volcano)

Схематики живут в **ядре** (`plugins/SoulEvents/schematics/`). Один `.schem` можно использовать в разных типах с **разными** настройками placement/paste/blend в `types/<id>.yml` каждого модуля.

**Нужен FastAsyncWorldEdit (FAWE).** Без него каталог загрузится, paste и scan не работают.

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

1. Постройка **в воздухе** → сохрани схему через **FAWE** (`//copy`, `//schem save <id>`).
2. Положи `<id>.schem` в `plugins/SoulEvents/schematics/`.
3. **Ровно один** блок маркера в точке anchor.
4. `/soulevents reload` → `/soulevents schematic info <id>`.

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

### Зависимости

| | Плагин | Нужен для |
|---|--------|-----------|
| **обяз.** | **SoulEvents** (core) | API, защита, scheduler, схематики через core |
| опц. | **FastAsyncWorldEdit** | только если в типе `schematic.enabled: true` (paste через core) |
| опц. | **WorldGuard** | gate «не спавнить в чужих регионах», временный регион арены (`arena-world-guard`) |
| опц. | **Vault** + economy-плагин | платный призыв `/airdrop summon` (`summon.cost > 0`) |
| опц. | **SoulEvents-MobWaves** | `wave-defense` в типе — волны мобов до открытия сундука |

Без WorldGuard gate по регионам и temp-арена **не работают** (NoOp — ивент всё равно спавнится). Без MobWaves wave-defense выключен. Без FAWE — только типы **без** схемы (сундук на земле).

**Задачи:**

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

**Wave-defense** — секция уже есть в `types/*.yml` (fresh install и после `/airdrop reload`, если секции не было). По умолчанию `enabled: false`. Нужен **SoulEvents-MobWaves.jar** — без него `enabled` игнорируется.

```yaml
# types/default.yml — поставь enabled: true после установки SoulEvents-MobWaves
wave-defense:
  enabled: false
  profile-id: default
  spawn-radius: 0    # 0 = из профиля MobWaves
```

Профиль волн (`MobWaves/profiles/`) задаёт состав волн, HP мобов и **супербосса** каждой волны. Пока волна не пройдена (убит босс, если включён) — сундук заблокирован. Подробнее — [SoulEvents-MobWaves](#soulevents-mobwaves).

Pre-open beacon по-прежнему в `pre-open-beacon:` — работает параллельно с wave-defense.

---

## SoulEvents-Volcano

### Зависимости

| | Плагин | Нужен для |
|---|--------|-----------|
| **обяз.** | **SoulEvents** (core) | API, защита, scheduler |
| **обяз.** | **FastAsyncWorldEdit** | все штатные типы используют `schematic.enabled: true` — без FAWE вулкан не вставится |
| опц. | **WorldGuard** | временный регион арены (`arena-world-guard`), gate по регионам при поиске точки |

Без WorldGuard temp-регион и проверки соседних регионов отключены (NoOp).

**Задачи:**

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

### Зависимости

| | Плагин | Нужен для |
|---|--------|-----------|
| **обяз.** | **SoulEvents** (core) | API, защита, LootGuard, stats |
| опц. | **FastAsyncWorldEdit** | только если в типе `schematic.enabled: true` (своя `.schem` вместо встроенного нексуса) |
| опц. | **WorldGuard** | отступ от чужих регионов при спавне, временный регион арены (`arena-world-guard`) |

По умолчанию (`builtinNexus.enabled: true`, `schematic.enabled: false`) **FAWE не нужен** — разлом строится блоками в мире.

Два режима в одном модуле:

1. **Standalone орда** — самостоятельный ивент: волны мобов, лут **только с убийств** (обфускация + TextDisplay, как у вулкана). Свои типы в `types/`, команда `/mobwaves`, GUI админки.
2. **Интеграция с AirDrop** — `wave-defense` блокирует сундук до прохождения волны (убийство босса); лут остаётся в сундуке airdrop.

---

### Орда (standalone)

Это не случайные нашествия. Раз за разом мир **испытывает** живых — вырывает в коре **разлом**, будто спрашивает: *«Вы ещё способны держать удар?»* Орды возвращаются снова и снова, пока кто-то не докажет обратное — или пока некому доказывать.

Где-то под травой и камнем рвётся кора. Из трещины поднимается нексус: обсидиан, портальные искры, низкий гул — мир на секунду замирает. По землям летит **призыв**: в чат каждому гремит *«☠ РАЗЛОМ ☠»*, координаты, как зов к оружию. Но испытание не начинается в пустоте: пока рядом нет **ни одного** смельчака, разлом лишь дремлет — тихое знамение ожидания. Если никто так и не пришёл на бой, трещина **схлопывается сама** — словно мир понял, что сегодня некого проверять, и убрал лишний гнев. Стоит первому ступить в круг — **волны** хлынут из земли и тумана: усиленные твари, в конце каждой — **супербосс**. Свергнешь босса — небо ответит громом и молнией, короткая передышка, и снова накат: новая волна, а старые мобы **не рассеиваются**, кольцо сжимается. С тел падает добыча — замаскированная, с призрачной надписью; над полем боя тикает **срок судьбы** — каждый поверженный враг дарует время, но между волнами песочница **не обнуляется**. Пройдёшь все волны — разлом вспыхнет финалом, по миру прокатится **«★ ПОБЕДА ★»**, полминуты на трофеи, и трещина затянется. Не выстоишь — разлом схлопнется без триумфа: испытание провалено, но мир **вернётся** — снова спросит, когда придёт время.

Это полноценный PvE-ивент без сундука: типы в `types/`, профили волн в `profiles/`, команда `/mobwaves`, лут только с убийств.

**Функции:**
- Типы в `types/<id>.yml` + лут `loot/<id>.yml`
- Автоспавн по интервалу, лимит одновременных орд (`maxConcurrentTotal`)
- Случайная точка на карте: минимум **100 блоков** от игроков и от чужих WG-регионов (настраивается в типе)
- **4 волны по умолчанию** в `profiles/default.yml` (fresh install): волна 1 — **3 зомби + 2 скелета + босс**, дальше нарастающие миньоны и HP боссов
- Переход к **следующей волне при убийстве супербосса** — **живые мобы прошлых волн остаются** и копятся (нельзя просто rush босса)
- Над обычным мобом: **«Волна N»** + HP-бар; у босса — **«БОСС · волна N»** (без отдельной метки волны сверху)
- Пачки спавна, пауза между волнами, **таймер волны** (BossBar + бонус секунд за килл; **не сбрасывается** между волнами — только на первой)
- **Leash к разлому:** мобов **тянет** pathfinder’ом; если не вернулись — **фейковая молния** «призыва» и телепорт (см. `horde-combat`)
- **Нексус разлома** — встроенный монолит или своя `.schem`; ambient-портал + **финал при победе** (частицы, гром, вспышка)
- LootGuard: маски, пул, TextDisplay над дропом, **action bar при подборе** (`lootVisual.pickup-action-bar-keys`)
- **Action bar киллеру:** «ты убил моба / босса волны N» + бонус секунд (`broadcast.mob-kill-action-bar-*`, `boss-kill-action-bar-*`)
- **BossBar** в радиусе: ожидание → таймер волны → пауза после босса → **ПОБЕДА** (фиолетовый) → cleanup
- Broadcast **«★ ПОБЕДА ★»** — игроки разгромили орду (`<kills>`, `<boss_killer>`); при провале — «разлом не выдержал»
- **30 сек** после полной зачистки до cleanup (`lifecycle.max-active-seconds-after-cleared`)
- Временный WG-регион арены (`arena-world-guard`)
- В сообщениях игрокам **нет названия мира** — только координаты и структура

**Жизненный цикл орды:**
1. Спавн нексуса / схемы на карте (+ broadcast «РАЗЛОМ»)
2. Ожидание игрока в радиусе (`require-player-for-waves`, `boss-bar-radius`)
3. Волны → **убей босса волны** (гром + молния) → пауза grace → следующая
4. Все волны пройдены → **эпичный финал разлома** + broadcast победы + **30 с** на трофеи
5. Cleanup: схема, дроп, эффекты (без «орда исчезла» после победы)

---

### Боевая система мобов

Мобы орды — **не ванильные**: усиленные HP, урон, броня, не горят на солнце, netherite-сет, оружие с зачарованиями.

| Параметр | Где задаётся | По умолчанию (fresh install) |
|----------|--------------|------------------------------|
| **HP по типу** | `profiles/<id>.yml` → `mob-overrides.<TYPE>.max-health` | ZOMBIE 80, SKELETON 65, HUSK 95, STRAY 70 |
| **Урон / скорость** | `mob-overrides` → `damage-multiplier`, `speed-multiplier` | ×1.8–2.2 / ×1.05–1.08 |
| **Глобальные множители** | `config.yml` → `horde-combat` | fallback, если в профиле 0 |
| **Броня** | `horde-combat.armor-bonus` | +6 |
| **Цель / leash** | `force-target-players`, `keep-near-anchor` | таргет игроков, притягивание к нексусу |
| **Притягивание** | `mob-pull-speed`, `mob-recall-attempts-before-lightning` | pathfinder → молния → TP |
| **Таймер волны** | `horde-combat.wave-timer-*` | 180 с на **1-й** волне, +180 с за килл; **между волнами не обнуляется** |

HP задаётся **явным числом** в `mob-overrides` или `entries[].max-health`. Над мобом — полоска HP, белая подсветка (красная у босса).

**Debug в консоли** (вкл/выкл в `config.yml`):
- `[MobWaves-Spawn]` — поиск точки орды
- `[MobWaves-Wave]` — attach, start, spawned, cleared
- `[MobWaves-Combat]` — урон моб↔игрок, отмена friendly-fire

---

### Супербосс волны

**Каждая волна** может иметь **супербосса** — спавнится **последним** в очереди волны.

**Прохождение волны:** если `super-boss-enabled: true`, следующая волна начинается **после смерти босса** (остальные мобы **не удаляются**). Если босс выключен — нужна полная зачистка всех мобов.

| Свойство | Описание |
|----------|----------|
| **HP** | `super-boss.max-health` или из `mob-overrides`, затем × `super-boss-health-multiplier` (дефолт **×5**, clamp ≤1024) |
| **Дефолтный профиль** | волна 1 — **3 зомби + 2 скелета + босс**; боссы 1→4: **80 → 120 → 180 → 260** HP (до множителя) |
| **Визуал** | красная подсветка, метка **БОСС · волна N**, крупные частицы |
| **Оружие** | netherite, зачарования (см. `horde-combat`) |
| **Убийство босса** | фейковая молния + **раскатистый гром**; action bar **только киллеру** |

**Action bar при убийстве** (только игрок, нанёсший последний удар):
- моб: `mobwaves.actionbar.you-killed-mob.*` — `<wave>`, `<bonus>`
- босс: `mobwaves.actionbar.you-killed-boss.*` — `<wave>`, `<kills>`, `<alive>`, `<bonus>`

Ключи в типе: `broadcast.mob-kill-action-bar-enabled`, `broadcast.boss-kill-action-bar-enabled`.

**В YAML профиля** (`profiles/default.yml`, fresh install — 4 волны):

```yaml
waves:
  - name: Разведка
    super-boss-enabled: true
    super-boss:
      entity-type: ZOMBIE
      count: 1
      max-health: 80
    entries:
      - entity-type: ZOMBIE
        count: 3
      - entity-type: SKELETON
        count: 2

  - name: Натиск
    super-boss-enabled: true
    super-boss:
      entity-type: HUSK
      count: 1
      max-health: 120
    entries:
      - entity-type: ZOMBIE
        count: 5
      - entity-type: SKELETON
        count: 3
      - entity-type: HUSK
        count: 2
  # … Эскалация, Финал — см. дефолт на диске после первого запуска
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

### BossBar и сообщения

| Фаза | Lang-ключ | Смысл |
|------|-----------|--------|
| Ожидание игрока | `mobwaves.bossbar.waiting` | нексус на месте, волны не стартовали |
| Волна активна | `mobwaves.bossbar.wave` | `<wave>/<waves>`, таймер, `+<bonus>` за килл |
| Пауза после босса | `mobwaves.bossbar.wave-grace` | босс повержен, жди следующую волну; **таймер идёт дальше** |
| **Победа** | `mobwaves.bossbar.victory` | орда побеждена, разлом схлопывается, осталось `<timer>` на лут |
| Провал / таймаут | `mobwaves.bossbar.despawn` | разлом рассыпается (не победа) |

| Broadcast | Когда | Lang |
|-----------|--------|------|
| Спавн | орда появилась | `mobwaves.broadcast.spawn` (coords, **без мира**) |
| **Победа** | все волны пройдены | `mobwaves.broadcast.cleared` — «игроки разгромили орду» |
| Провал | таймер волны / despawn без победы | `mobwaves.broadcast.removed` — «орда не была побеждена» |

После победы cleanup **не** шлёт `removed`.

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
- Список волн — слоты **9–44** (до **36** на страницу), при большем числе — **страницы** ← / →; нижний ряд — кнопки (назад, пагинация)
- ЛКМ по волне — редактор, ПКМ — настройки волны

**Evoker** и **Ender Dragon** в волнах запрещены.

**Пример полного профиля** (укорочен; на fresh install генерируется 4 волны):

```yaml
spawn-radius: 14
batch-size: 3
batch-interval-ticks: 40
grace-after-clear-seconds: 20

waves:
  - name: Разведка
    super-boss-enabled: true
    super-boss:
      entity-type: ZOMBIE
      count: 1
      max-health: 80
    entries:
      - entity-type: ZOMBIE
        count: 3
      - entity-type: SKELETON
        count: 2

  - name: Финал
    super-boss-enabled: true
    super-boss:
      entity-type: ZOMBIE
      count: 1
      max-health: 260
    entries:
      - entity-type: HUSK
        count: 5
      - entity-type: STRAY
        count: 4

mob-overrides:
  ZOMBIE:
    max-health: 80
    damage-multiplier: 2.0
    speed-multiplier: 1.08
  SKELETON:
    max-health: 65
    damage-multiplier: 1.8
    speed-multiplier: 1.05
```

> Старый `profiles/default.yml` с одной волной **не перезаписывается** — удали файл и `/mobwaves reload`, либо настрой в GUI.

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
  boss-bar-victory-key: mobwaves.bossbar.victory
  require-player-for-waves: true
  max-active-seconds: 600
  max-active-seconds-after-cleared: 30   # после победы до cleanup
broadcast:
  cleared-enabled: true                  # mobwaves.broadcast.cleared — ПОБЕДА
  mob-kill-action-bar-enabled: true
  boss-kill-action-bar-enabled: true
lootVisual:
  pickup-action-bar-enabled: true
  pickup-action-bar-keys:
    - mobwaves.pickup.action.1
    - mobwaves.pickup.action.2
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
  wave-timer-enabled: true
  initial-wave-timer-seconds: 180
  seconds-added-per-kill: 180
  clear-vanilla-drops: true
  keep-near-anchor: true
  mob-pull-speed: 1.35
  mob-recall-attempts-before-lightning: 5   # ~2.5 с тянуть, потом молния + TP
  mob-pull-particles-enabled: true
  health-multiplier: 5.0
  damage-multiplier: 5.0
  speed-multiplier: 1.2
  super-boss-health-multiplier: 5.0
  armor-bonus: 6.0
  force-target-players: true
```

Статистика убийств/лута орды попадает в **core stats** → `%soulevents_mobwaves_kills%`, `%soulevents_mobwaves_loot%` (см. [Статистика и PlaceholderAPI](#статистика-и-placeholderapi)).

**API:** `MobWaveBridge` (Bukkit Services) — `attach`, `detach`, `blocksChest`, `status` (для airdrop и других модулей).

```java
// AirDrop вызывает при wave-defense:
mobWaveBridge.attach(new MobWaveAttachRequest(
    sessionId,
    profileId,
    anchorLocation,
    spawnRadius
));
// blocksChest(sessionId) == true пока текущая волна не пройдена (убит босс)
```

---

## Типичная установка на сервер

1. `SoulEvents.jar` → `plugins/`
2. Модули: `SoulEvents-AirDrop.jar`, `SoulEvents-Volcano.jar`, …
3. Для волн у аirdrop: ещё `SoulEvents-MobWaves.jar`
4. **FastAsyncWorldEdit** — схематики; **WorldGuard** — регионы; **Vault** — платный airdrop; **PlaceholderAPI** — плейсхолдеры
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
