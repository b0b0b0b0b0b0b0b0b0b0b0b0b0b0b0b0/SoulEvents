> **Проект новый и сырой — на проде не использовать.** Репозиторий на GitHub — только бэкап, чтобы не потерять данные.  
> **Pull request'ы и сторонние правки не принимаем** — разрабатываем сами.

## SoulEvents (core)

### Назначение

Ядро для всех ивент-модулей: общий API, сессии, планировщик, reload, каталог схематик, защита от абуза, **накопительная статистика** в SQLite.

### Функции

- **Gate** — инвиз, полёт, броня, предметы в руке (Base64 ItemStack)
- **LootGuard** — кулдаун на лут, обфускация предметов
- **ArenaGuard** — запрет постройки и заливки жидкостями на арене
- **EffectResolver** — иммун/усиление дебафов от предметов
- **SchematicService** — paste / undo / scan `.schem` (через FAWE)
- **Scheduler** — автоспавн типов модулей по интервалу
- **Stats** — убийства, лут, сундуки → PlaceholderAPI

### Команды

| Команда | Описание |
|---------|----------|
| `/soulevents reload` | Перезагрузка core + всех модулей |
| `/soulevents modules` | Список зарегистрированных модулей |
| `/soulevents schematic list` | Список схематик |
| `/soulevents schematic info <id>` | Информация о схеме |
| `/soulevents schematic scan <id>` | Перескан `.schem` |

### Права

| Право | Описание |
|-------|----------|
| `soulevents.admin` | Команды core (default: op) |

### Плейсхолдеры

Expansion **`soulevents`** (нужен PlaceholderAPI). Без него stats пишутся в SQLite, плейсхолдеры не регистрируются.

**Глобально (все модули):**

| Плейсхолдер | Значение |
|-------------|----------|
| `%soulevents_total_kills%` | все убийства мобов ивентов |
| `%soulevents_total_loot%` | весь подобранный лут |
| `%soulevents_total_chests%` | открытия + полные луты airdrop |

**По модулю** — см. разделы AirDrop / Volcano / MobWaves. По типу: `%soulevents_<module>_<метрика>_<typeId>%`.

### Зависимости

| | Плагин | Нужен для |
|---|--------|-----------|
| — | *(нет hard-depend)* | JAR ставится сам по себе |
| опц. | **FastAsyncWorldEdit** | paste, undo и scan `.schem` |
| опц. | **PlaceholderAPI** | `%soulevents_*%` |

> Схематики **без FAWE** не вставляются.

### Прочее

**Папка:** `plugins/SoulEvents/` → `config.yml`, `protection.yml`, `stats.yml`, `lang/`, `schematics/`, `stats.db`

**`stats.yml`:**

| Ключ | Дефолт |
|------|--------|
| `enabled` | `true` |
| `flush-interval-seconds` | `30` |
| `sqlite-file-name` | `stats.db` |
| `maximum-pool-size` | `2` |

| Метрика | Модули |
|---------|--------|
| `mobs_killed` | mobwaves |
| `loot_taken` | mobwaves, volcano, airdrop |
| `chests_opened` | airdrop |
| `chests_looted` | airdrop |

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
| **AirDrop** | 1+ `marker.block` (дефолт bedrock) | Точки сундуков; `schematic.marker.spawn-count` — сколько активировать за спавн (случайно). При нескольких точках `chestCluster` не используется |
| **Volcano** | 1+ bedrock | Жерла извержения; `marker.spawn-count` — сколько жерёл активировать |
| **MobWaves** | 1+ bedrock (если своя `.schem`) | Точки anchor орды; `marker.spawn-count` |

Схема — **декор**. Лутовые сундуки (airdrop) и вылет предметов (volcano) плагин ставит/спавнит сам.

### Подготовка `.schem`

1. Постройка **в воздухе** → сохрани схему через **FAWE** (`//copy`, `//schem save <id>`).
2. Положи `<id>.schem` в `plugins/SoulEvents/schematics/`.
3. Размести **один или несколько** блоков маркера (по умолчанию Бедрок) там, где нужны точки лута / жерла / anchor. Минимум **один** маркер в схеме. Сколько из них активировать за один спавн — `schematic.marker.spawn-count` в типе модуля (случайный выбор без повторов, не больше числа маркеров в `.schem`).
4. `/soulevents reload` → `/soulevents schematic info <id>` — в выводе смотри число маркеров (`markers`).

**Volcano — магма в схеме:** в `.schem` можно расставить **магму** (`MAGMA_BLOCK`) как декор кратера и склонов. После вставки плагин находит эти блоки в объёме схемы и поднимает над ними **столбы дыма** (серые клубы + тёмная струя) — вулкан «дымится» без ручных частиц. Настройки в типе: `visual.magma-smoke-*` (`magma-smoke-enabled`, интервал, высота столба, частицы). Bedrock-маркеры жерла дополнительно могут светиться огнём (`visual.bedrock-glow-*`).

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

### Описание эвента

С неба на мир падает **аирдроп** — сундук с добычей, вокруг которого сгущается напряжение. По чату летит координатный зов; кто первый доберётся, тот и в деле. Сундук не отдаёт лут сразу: может стоять **beacon смерти** с дебаффами в радиусе, а если включён **wave-defense** — сначала нужно пройти волны MobWaves и свергнуть босса. Только потом — открытие, обфускация в слотах, гонка за содержимым. Схематика опциональна: сундук на земле или арена из `.schem`.

### Функции эвента

- Автоспавн каждые N минут, лимит concurrent по типу
- Whitelist/blacklist миров и WorldGuard gate
- Схематика + blend в ландшафт (опционально)
- Pre-open **death-beacon** (дебаф в радиусе)
- **Wave-defense** — волны MobWaves, блокировка сундука до зачистки
- Обязательный предмет / permission для открытия
- Призыв `/airdrop summon`, оплата через Vault
- GUI: типы, лут, обфускация, требования, despawn активных

### Команды

| Команда | Описание |
|---------|----------|
| `/airdrop admin` | GUI со списком типов |
| `/airdrop summon <тип>` | Призыв (игрок / админ) |
| `/airdrop reload` | Перезагрузка модуля |

### Права

| Право | Описание |
|-------|----------|
| `soulevents.airdrop.staff` | admin GUI, reload, admin-summon (default: op) |
| `soulevents.airdrop.summon` | призыв игроком |
| `soulevents.airdrop.bypass` | обход лимита concurrent |
| `soulevents.airdrop.open.<тип>` | donate-сундуки (если включено в типе) |

### Плейсхолдеры

Expansion `soulevents` — [глобальные](#плейсхолдеры) в core.

| Плейсхолдер | Значение |
|-------------|----------|
| `%soulevents_airdrop_loot%` | подобранный лут (global модуля) |
| `%soulevents_airdrop_chests%` | сундуки полностью залутаны |
| `%soulevents_airdrop_opens%` | первые открытия сундуков |
| `%soulevents_airdrop_loot_<typeId>%` | лут по типу |
| `%soulevents_airdrop_chests_<typeId>%` | залутаны по типу |
| `%soulevents_airdrop_opens_<typeId>%` | открытия по типу |

### Зависимости

| | Плагин | Нужен для |
|---|--------|-----------|
| **обяз.** | **SoulEvents** | API, защита, scheduler, схематики |
| опц. | **FastAsyncWorldEdit** | `schematic.enabled: true` в типе |
| опц. | **WorldGuard** | gate по регионам, temp-арена |
| опц. | **Vault** + economy | платный summon (`summon.cost > 0`) |
| опц. | **SoulEvents-MobWaves** | `wave-defense` в типе |

Без WorldGuard gate и temp-арена отключены (NoOp). Без MobWaves `wave-defense` игнорируется. Без FAWE — типы без схемы.

### Прочее

**Папка:** `plugins/SoulEvents-AirDrop/`

```
SoulEvents-AirDrop/
├── config.yml
├── gui/general.yml
├── types/
├── loot/
├── lang/
└── storage/airdrop.db
```

**Wave-defense** в `types/*.yml` (дефолт `enabled: false`). Нужен **SoulEvents-MobWaves.jar**:

```yaml
wave-defense:
  enabled: false          # true после установки MobWaves
  profile-id: default
  spawn-radius: 0         # 0 = из профиля MobWaves
```

Профили волн — `MobWaves/profiles/`. Подробнее — [MobWaves → Профили волн](#профили-волн). Pre-open beacon: `pre-open-beacon:` в типе.

---

## SoulEvents-Volcano

### Описание эвента

В мире **просыпаются вулканы** — не декорация, а живые раны земли. Где-то на горизонте встаёт конус, из жерла тянется дым; земля под ногами дрожит, будто что-то внизу переворачивается. По чату летит **весть**: *«🌋 ВУЛКАН 🌋»* — координаты, как маяк для охотников за добычей. Сначала **тишина перед бурей**: над кратером обратный отсчёт, голограмма «извержение через…». Потом — **извержение**: из bedrock-жерла в небо вырываются сокровища, искры и жар; лут **не в сундуке** — его **ловят на лету**, пока предметы ещё горят в воздухе. Над снопом — призрачная надпись «подбери!»; чужой забрал — не твоё. Когда последний трофей ушёл — вулкан **гаснет**, по миру *«☠ ПОТУХ»*, остаётся пепел и таймер до исчезновения. Не успел никто — конус стихнет и сотрётся, словно извержения не было.

PvE-гонка за лут: схематика на карте, типы в `types/`, `/volcano`, GUI админки.

### Функции эвента

- Автоспавн по таймеру + ручной призыв
- Схематика с bedrock-жерлом (можно несколько маркеров в `.schem`)
- Адаптация рельефа: ступени к подножию, ragged-края, фильтр гор/обрывов при поиске точки
- Фазы: paste → ожидание → **извержение** → лут летит из жерла (Item + TextDisplay)
- Дым, bossbar countdown, урон у горячего жерла
- LootGuard на подбор предметов
- Временный WG-регион арены
- GUI: типы, лут, summon / TP / batch / despawn

### Команды

| Команда | Описание |
|---------|----------|
| `/volcano admin` | GUI типов |
| `/volcano summon <тип>` | Призыв |
| `/volcano reload` | Перезагрузка |

### Права

| Право | Описание |
|-------|----------|
| `soulevents.volcano.staff` | admin GUI, reload (default: op) |
| `soulevents.volcano.bypass` | обход лимита concurrent |

### Плейсхолдеры

Expansion `soulevents` — [глобальные](#плейсхолдеры) в core.

| Плейсхолдер | Значение |
|-------------|----------|
| `%soulevents_volcano_loot%` | подобранный лут (global модуля) |
| `%soulevents_volcano_loot_<typeId>%` | лут по типу |

### Зависимости

| | Плагин | Нужен для |
|---|--------|-----------|
| **обяз.** | **SoulEvents** | API, защита, scheduler |
| **обяз.** | **FastAsyncWorldEdit** | штатные типы на `schematic.enabled: true` |
| опц. | **WorldGuard** | temp-арена, gate при поиске точки |

Без WorldGuard temp-регион и проверки регионов отключены (NoOp).

### Прочее

**Папка:** `plugins/SoulEvents-Volcano/`

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
| `visual` | дым из жерла, **дым с магмы/лавы в схеме**, bedrock-glow, подписи над лутом, bossbar |
| `lifecycle` | потухание после извержения / после разграбления |
| `arena-world-guard` | временный регион |

**Admin GUI → тип:** summon / batch / TP / despawn активных / редактор лута.

---

## SoulEvents-MobWaves

### Описание эвента

Это не случайные нашествия. Раз за разом мир **испытывает** живых — вырывает в коре **разлом**, будто спрашивает: *«Вы ещё способны держать удар?»* Орды возвращаются снова и снова, пока кто-то не докажет обратное — или пока некому доказывать.

Где-то под травой и камнем рвётся кора. Из трещины поднимается нексус: обсидиан, портальные искры, низкий гул — мир на секунду замирает. По землям летит **призыв**: в чат каждому гремит *«☠ РАЗЛОМ ☠»*, координаты, как зов к оружию. Но испытание не начинается в пустоте: пока рядом нет **ни одного** смельчака, разлом лишь дремлет — тихое знамение ожидания. Если никто так и не пришёл на бой, трещина **схлопывается сама** — словно мир понял, что сегодня некого проверять, и убрал лишний гнев. Стоит первому ступить в круг — **волны** хлынут из земли и тумана: усиленные твари, в конце каждой — **супербосс**. Свергнешь босса — небо ответит громом и молнией, короткая передышка, и снова накат: новая волна, а старые мобы **не рассеиваются**, кольцо сжимается. С тел падает добыча — замаскированная, с призрачной надписью; над полем боя тикает **срок судьбы** — каждый поверженный враг дарует время, но между волнами песочница **не обнуляется**. Пройдёшь все волны — разлом вспыхнет финалом, по миру прокатится **«★ ПОБЕДА ★»**, полминуты на трофеи, и трещина затянется. Не выстоишь — разлом схлопнется без триумфа: испытание провалено, но мир **вернётся** — снова спросит, когда придёт время.

PvE без сундука: типы в `types/`, профили в `profiles/`, `/mobwaves`, лут только с убийств.

### Функции эвента

- Типы в `types/<id>.yml` + лут `loot/<id>.yml`
- Автоспавн по интервалу, лимит одновременных орд (`maxConcurrentTotal`)
- Случайная точка на карте: минимум **100 блоков** от игроков и от чужих WG-регионов (настраивается в типе)
- **4 волны по умолчанию** в `profiles/default.yml` (fresh install): волна 1 — **3 зомби + 2 скелета + босс**, дальше нарастающие миньоны и HP боссов
- Переход к **следующей волне при убийстве супербосса** — **живые мобы прошлых волн остаются** и копятся
- Над мобом: **«Волна N»** + HP-бар; у босса — **«БОСС · волна N»**
- Пачки спавна, пауза между волнами, **таймер волны** (BossBar + бонус секунд за килл; **не сбрасывается** между волнами)
- **Leash к разлому:** pathfinder → фейковая молния → телепорт (см. `horde-combat`)
- **Нексус разлома** — встроенный монолит или своя `.schem`; ambient-портал + **финал при победе**
- LootGuard: маски, пул, TextDisplay над дропом, **action bar при подборе**
- **Action bar киллеру** при убийстве моба / босса
- **BossBar** в радиусе: ожидание → таймер → пауза → **ПОБЕДА** → cleanup
- Broadcast **«★ ПОБЕДА ★»** / провал «разлом не выдержал»
- **30 сек** после полной зачистки до cleanup
- Временный WG-регион арены; в сообщениях **нет названия мира** — только координаты
- **Интеграция с AirDrop** — `wave-defense` в типе airdrop (волны до сундука, лут в сундуке)

### Команды

| Команда | Описание |
|---------|----------|
| `/mobwaves admin` | GUI: типы орды + профили волн |
| `/mobwaves summon <type>` | Призвать орду |
| `/mobwaves despawn <type>` | Убрать активные орды типа |
| `/mobwaves reload` | Перезагрузка конфигов и lang |

### Права

| Право | Описание |
|-------|----------|
| `soulevents.mobwaves.staff` | GUI, summon, reload (default: op) |
| `soulevents.mobwaves.bypass` | GM/полёт в зоне, обход gate при summon |

### Плейсхолдеры

Expansion `soulevents` — [глобальные](#плейсхолдеры) в core.

| Плейсхолдер | Значение |
|-------------|----------|
| `%soulevents_mobwaves_kills%` | убийства мобов орды (global модуля) |
| `%soulevents_mobwaves_loot%` | подобранный лут (global модуля) |
| `%soulevents_mobwaves_kills_<typeId>%` | убийства по типу |
| `%soulevents_mobwaves_loot_<typeId>%` | лут по типу |

### Зависимости

| | Плагин | Нужен для |
|---|--------|-----------|
| **обяз.** | **SoulEvents** | API, защита, LootGuard, stats |
| опц. | **FastAsyncWorldEdit** | только если в типе `schematic.enabled: true` |
| опц. | **WorldGuard** | отступ от регионов при спавне, temp-арена |

По умолчанию (`builtinNexus.enabled: true`, `schematic.enabled: false`) **FAWE не нужен** — разлом строится блоками в мире.

### Прочее

#### AirDrop wave-defense

Волны до открытия сундука airdrop. В типе airdrop: `wave-defense.enabled: true`, `profile-id`, `spawn-radius`. Профили — те же `profiles/`; API — `MobWaveBridge` (см. ниже).

#### Жизненный цикл орды

1. Спавн нексуса / схемы (+ broadcast «РАЗЛОМ»)
2. Ожидание игрока в радиусе (`require-player-for-waves`, `boss-bar-radius`)
3. Волны → **убей босса волны** (гром + молния) → пауза grace → следующая
4. Все волны пройдены → **финал разлома** + broadcast победы + **30 с** на трофеи
5. Cleanup: схема, дроп, эффекты (без «орда исчезла» после победы)

#### Боевая система мобов

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

#### Супербосс волны

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

#### BossBar и сообщения

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

#### Профили волн

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

#### Игроки в зоне орды

В радиусе активной орды (обычно `lifecycle.boss-bar-radius`, по умолчанию **48 блоков**):
- **Creative / Spectator → Survival**
- **Полёт отключается**

Исключение: право **`soulevents.mobwaves.bypass`** — OP/админы могут оставаться в GM с полётом (обход лимитов спавна и зоны).

Работает и для **airdrop wave-defense** (радиус из spawn-radius профиля, минимум 48).

#### Конфиг

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

#### API MobWaveBridge

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
