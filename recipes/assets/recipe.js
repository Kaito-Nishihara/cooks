// レシピページ共通の振る舞い。各ページの分量の換算と手順の進み具合、一覧の検索と絞り込み、今日の献立を持つ

/** 分量を読みやすい表記へ整える */
class QuantityFormatter {
  /** 大さじ、個などは分数で表す。分母はこの中から近いものを選ぶ */
  static DENOMINATORS = [2, 3, 4];

  /**
   * @param {number} value
   * @param {"int" | "frac" | "round5"} style
   * @returns {string}
   */
  format(value, style) {
    if (style === "round5") return String(Math.max(5, Math.round(value / 5) * 5));
    if (style === "int") return String(Math.max(1, Math.round(value)));
    return this.toFraction(value);
  }

  /**
   * @param {number} value
   * @returns {string}
   */
  toFraction(value) {
    const whole = Math.floor(value + 1e-9);
    const rest = value - whole;
    if (rest < 0.08) return String(whole || 1);
    if (rest > 0.92) return String(whole + 1);

    let best = { num: 1, den: 2, diff: Infinity };
    for (const den of QuantityFormatter.DENOMINATORS) {
      const num = Math.round(rest * den);
      const diff = Math.abs(rest - num / den);
      if (num > 0 && num < den && diff < best.diff) best = { num, den, diff };
    }
    const frac = `${best.num}/${best.den}`;
    return whole === 0 ? frac : `${whole}と${frac}`;
  }
}

/** 人数の増減に合わせて材料の分量を書き換える */
class ServingsScaler {
  /**
   * @param {HTMLElement} root 材料カードの要素
   * @param {QuantityFormatter} formatter
   */
  constructor(root, formatter) {
    this.formatter = formatter;
    this.base = Number(root.dataset.baseServings);
    this.min = Number(root.dataset.minServings ?? 1);
    this.max = Number(root.dataset.maxServings ?? 8);
    this.servings = this.base;
    this.countEl = root.querySelector("[data-servings-count]");
    this.decBtn = root.querySelector("[data-servings-dec]");
    this.incBtn = root.querySelector("[data-servings-inc]");
    this.items = [...root.querySelectorAll("[data-amount]")];

    this.decBtn.addEventListener("click", () => this.set(this.servings - 1));
    this.incBtn.addEventListener("click", () => this.set(this.servings + 1));
    this.render();
  }

  /** @param {number} next */
  set(next) {
    this.servings = Math.min(this.max, Math.max(this.min, next));
    this.render();
  }

  render() {
    const ratio = this.servings / this.base;
    this.countEl.textContent = String(this.servings);
    this.decBtn.disabled = this.servings <= this.min;
    this.incBtn.disabled = this.servings >= this.max;

    for (const el of this.items) {
      const amount = Number(el.dataset.amount) * ratio;
      const text = this.formatter.format(amount, el.dataset.style ?? "int");
      const { prefix = "", unit = "" } = el.dataset;
      el.textContent = `${prefix}${text}${unit}`;
    }
  }
}

/** 手順ごとの「済」ボタンで、終わった手順を薄く表示する */
class StepTracker {
  /** @param {HTMLElement} list 手順の ol 要素 */
  constructor(list) {
    for (const step of list.querySelectorAll(".step")) {
      const btn = document.createElement("button");
      btn.type = "button";
      btn.className = "step__done";
      btn.textContent = "済";
      btn.setAttribute("aria-pressed", "false");
      btn.addEventListener("click", () => {
        const done = step.classList.toggle("is-done");
        btn.setAttribute("aria-pressed", String(done));
      });
      step.appendChild(btn);
    }
  }
}

/** 検索のために文字列の表記ゆれをそろえる。全角と半角、大文字と小文字、カタカナとひらがなを同一視する */
class TextNormalizer {
  /**
   * @param {string} text
   * @returns {string}
   */
  normalize(text) {
    return text
      .normalize("NFKC")
      .toLowerCase()
      .replace(/[ァ-ヶ]/g, (ch) => String.fromCharCode(ch.charCodeAt(0) - 0x60));
  }
}

/** 献立の合計に使う栄養素。目録の nutrition のキー、単位、表示の桁をまとめて持つ */
class Nutrient {
  /**
   * @param {keyof Nutrition} key
   * @param {string} label
   * @param {string} unit
   * @param {number} digits 小数点以下の桁数
   */
  constructor(key, label, unit, digits) {
    this.key = key;
    this.label = label;
    this.unit = unit;
    this.digits = digits;
  }

  /**
   * @param {number} value
   * @returns {string}
   */
  format(value) {
    const text = value.toLocaleString("ja-JP", { minimumFractionDigits: this.digits, maximumFractionDigits: this.digits });
    return `${text}${this.unit}`;
  }

  /**
   * @param {Nutrition[]} list
   * @returns {Nutrition}
   */
  static sum(list) {
    const total = { kcal: 0, protein: 0, fat: 0, carb: 0, salt: 0 };
    for (const n of list) for (const nutrient of Nutrient.ALL) total[nutrient.key] += n[nutrient.key];
    return total;
  }

  static ALL = [
    new Nutrient("kcal", "エネルギー", "kcal", 0),
    new Nutrient("protein", "たんぱく質", "g", 0),
    new Nutrient("fat", "脂質", "g", 0),
    new Nutrient("carb", "炭水化物", "g", 0),
    new Nutrient("salt", "塩分", "g", 1),
  ];
}

/** 暦の上の一日を "YYYY-MM-DD" の文字列で扱う。時刻を持たせないので時間帯で日付がずれない */
class CalendarDay {
  /**
   * @param {Date} [now]
   * @returns {string}
   */
  static today(now = new Date()) {
    const pad = (n) => String(n).padStart(2, "0");
    return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
  }

  /**
   * @param {string} day "YYYY-MM-DD"
   * @returns {string} 「9月30日(水)」の形
   */
  static label(day) {
    const [y, m, d] = day.split("-").map(Number);
    const week = "日月火水木金土"[new Date(y, m - 1, d).getDay()];
    return `${m}月${d}日(${week})`;
  }
}

/** 食事の区分。献立の品はどれか一つに属する */
class Meal {
  /**
   * @param {string} key
   * @param {string} label
   * @param {number} untilHour この時刻より前に追加した品は、この食事に入れる
   */
  constructor(key, label, untilHour) {
    this.key = key;
    this.label = label;
    this.untilHour = untilHour;
  }

  static ALL = [
    new Meal("breakfast", "朝食", 10),
    new Meal("lunch", "昼食", 15),
    new Meal("dinner", "夕食", 24),
  ];

  /**
   * @param {Date} [now]
   * @returns {Meal} 今の時刻に合う食事
   */
  static current(now = new Date()) {
    return Meal.ALL.find((m) => now.getHours() < m.untilHour) ?? Meal.ALL[Meal.ALL.length - 1];
  }

  /**
   * @param {string} key
   * @returns {Meal}
   */
  static of(key) {
    return Meal.ALL.find((m) => m.key === key) ?? Meal.ALL[Meal.ALL.length - 1];
  }
}

/**
 * @typedef {{ data: RecipeData, haystack: string }} RecipeEntry
 */

/** レシピの目録を引く。検索用の文字列はここで一度だけ作る */
class RecipeCatalog {
  /**
   * @param {RecipeData[]} data
   * @param {TextNormalizer} normalizer
   */
  constructor(data, normalizer) {
    /** @type {RecipeEntry[]} */
    this.entries = data.map((d) => ({
      data: d,
      haystack: normalizer.normalize([d.title ?? d.name, d.reading, ...d.ingredients, ...d.genres].join(" ")),
    }));
  }

  /**
   * @param {string} slug
   * @returns {RecipeData | undefined}
   */
  find(slug) {
    return this.entries.find((e) => e.data.slug === slug)?.data;
  }
}

/**
 * 一覧のカードを描き、検索やジャンルなど複数の条件を名前付きで持って、すべてを満たすカードだけを表示する。
 * 条件を足すときは setCriterion に名前と判定関数を渡すだけでよい。
 */
class CardList {
  /**
   * @param {HTMLElement} container
   * @param {RecipeCatalog} catalog
   */
  constructor(container, catalog) {
    this.catalog = catalog;
    /** @type {Map<string, (entry: RecipeEntry) => boolean>} */
    this.criteria = new Map();
    /** @type {Set<(visible: number, total: number) => void>} */
    this.listeners = new Set();
    /** @type {Map<string, HTMLElement>} */
    this.cards = new Map();
    for (const entry of catalog.entries) {
      const card = this.renderCard(entry.data);
      this.cards.set(entry.data.slug, card);
      container.append(card);
    }
  }

  /**
   * @param {RecipeData} d
   * @returns {HTMLElement}
   */
  renderCard(d) {
    const card = Dom.el("article", "card");
    const link = Dom.el("a", "card__link");
    link.href = `${d.slug}.html`;
    const img = Dom.el("img");
    img.src = `images/${d.slug}/hero.jpg`;
    img.alt = d.name;
    img.loading = "lazy";
    const title = Dom.el("h2", "card__title", d.name);
    if (d.arrange) title.append(Dom.el("span", "card__badge", "アレンジ"));
    const body = Dom.el("div", "card__body");
    body.append(title, Dom.el("span", "card__meta", `${d.kicker} / ${d.time}分 / 2人分`));
    link.append(img, body);
    card.append(link);
    return card;
  }

  /**
   * @param {string} slug
   * @returns {HTMLElement | undefined}
   */
  cardOf(slug) {
    return this.cards.get(slug);
  }

  /**
   * 条件を登録する。null を渡すとその条件を外す
   * @param {string} name
   * @param {((entry: RecipeEntry) => boolean) | null} predicate
   */
  setCriterion(name, predicate) {
    if (predicate) this.criteria.set(name, predicate);
    else this.criteria.delete(name);
    this.apply();
  }

  /** @param {(visible: number, total: number) => void} listener */
  onChange(listener) {
    this.listeners.add(listener);
  }

  apply() {
    let visible = 0;
    for (const entry of this.catalog.entries) {
      const show = [...this.criteria.values()].every((test) => test(entry));
      const card = this.cards.get(entry.data.slug);
      if (card) card.hidden = !show;
      if (show) visible++;
    }
    for (const listener of this.listeners) listener(visible, this.catalog.entries.length);
  }
}

/**
 * ジャンルで絞り込むボタンを並べる。
 * ボタンは目録の genres から出現順に作るので、ジャンルを足すときは目録に書くだけでよい。
 * 選んだジャンルは URL のハッシュに残し、レシピから戻ったときも同じ絞り込みで表示する。
 */
class GenreFilter {
  static ALL = "すべて";

  /**
   * @param {HTMLElement} nav 絞り込みボタンを入れる要素
   * @param {CardList} list
   */
  constructor(nav, list) {
    this.list = list;
    this.genres = [GenreFilter.ALL, ...new Set(list.catalog.entries.flatMap((e) => e.data.genres))];
    this.buttons = this.genres.map((genre) => this.createButton(genre));
    nav.append(...this.buttons);

    window.addEventListener("hashchange", () => this.apply(this.fromHash()));
    this.apply(this.fromHash());
  }

  /**
   * @param {string} genre
   * @returns {HTMLButtonElement}
   */
  createButton(genre) {
    const btn = Dom.button("genre-filter__btn", genre, () => {
      // ハッシュが変わらないときは hashchange が発火しないので、ここでも反映する
      location.hash = genre === GenreFilter.ALL ? "" : encodeURIComponent(genre);
      this.apply(genre);
    });
    btn.dataset.genre = genre;
    return btn;
  }

  /** @returns {string} ハッシュが指すジャンル。知らないジャンルなら「すべて」 */
  fromHash() {
    let genre = "";
    try {
      genre = decodeURIComponent(location.hash.slice(1));
    } catch {
      // 壊れたハッシュは無視して「すべて」を表示する
    }
    return this.genres.includes(genre) ? genre : GenreFilter.ALL;
  }

  /** @param {string} genre */
  apply(genre) {
    this.list.setCriterion("genre",
      genre === GenreFilter.ALL ? null : (entry) => entry.data.genres.includes(genre));
    for (const btn of this.buttons) {
      btn.setAttribute("aria-pressed", String(btn.dataset.genre === genre));
    }
  }
}

/** 検索欄の入力で絞り込む。空白で区切った語をすべて含むカードを残す */
class RecipeSearch {
  /**
   * @param {HTMLInputElement} input
   * @param {CardList} list
   * @param {TextNormalizer} normalizer
   */
  constructor(input, list, normalizer) {
    this.list = list;
    this.normalizer = normalizer;
    input.addEventListener("input", () => this.apply(input.value));
    this.apply(input.value);
  }

  /** @param {string} query */
  apply(query) {
    const terms = this.normalizer.normalize(query).split(/\s+/).filter(Boolean);
    this.list.setCriterion("search",
      terms.length === 0 ? null : (entry) => terms.every((t) => entry.haystack.includes(t)));
  }
}

/** 表示中の件数を出し、該当がないときはそう知らせる */
class CatalogStatus {
  /**
   * @param {HTMLElement} el
   * @param {CardList} list
   */
  constructor(el, list) {
    list.onChange((visible, total) => {
      if (visible === 0) el.textContent = "条件に合うレシピがありません。";
      else if (visible === total) el.textContent = `${total}品`;
      else el.textContent = `${total}品中 ${visible}品`;
    });
    list.apply();
  }
}

/**
 * @typedef {{ id: string, meal: string }} MenuItem
 * @typedef {{ day: string, items: MenuItem[] }} MenuState
 */

/**
 * 献立をブラウザに覚える。保存するのは料理の識別子と食事の区分だけで、名前や栄養の値はその都度目録から引く。
 * ブラウザの保存領域が使えないときも、ページを閉じるまでの間は操作を続けられる。
 */
class MenuStore {
  static KEY = "recipes.menu.v2";
  /** 識別子だけを並べていた前の形式。読み込めたら夕食として引き継ぐ */
  static LEGACY_KEY = "recipes.menu.v1";

  /**
   * @param {string} today
   * @returns {MenuState}
   */
  load(today) {
    try {
      const raw = localStorage.getItem(MenuStore.KEY);
      if (raw) {
        const parsed = JSON.parse(raw);
        const items = Array.isArray(parsed?.items)
          ? parsed.items.filter((x) => typeof x?.id === "string" && typeof x?.meal === "string")
          : [];
        const day = typeof parsed?.day === "string" ? parsed.day : today;
        return { day, items };
      }
      const legacy = JSON.parse(localStorage.getItem(MenuStore.LEGACY_KEY) ?? "[]");
      if (Array.isArray(legacy)) {
        return { day: today, items: legacy.filter((x) => typeof x === "string").map((id) => ({ id, meal: "dinner" })) };
      }
    } catch {
      // 壊れた値は捨てて空の献立から始める
    }
    return { day: today, items: [] };
  }

  /** @param {MenuState} state */
  save(state) {
    try {
      localStorage.setItem(MenuStore.KEY, JSON.stringify(state));
      localStorage.removeItem(MenuStore.LEGACY_KEY);
    } catch {
      // 保存できない環境では、ページを閉じるまでの間だけ覚えておく
    }
  }
}

/**
 * @typedef {{ index: number, id: string, meal: Meal, name: string, href: string | null,
 *   image: string | null, nutrition: Nutrition }} ResolvedItem
 */

/**
 * 今日の献立の状態と操作。一覧の献立の枠と、今日の献立のページが同じものを使う。
 * ご飯はレシピではないが献立に欠かせないので、固定の品として何杯でも足せるようにする。
 */
class MenuModel {
  static RICE = {
    id: "rice",
    name: "ご飯(茶碗1杯 150g)",
    // 日本食品標準成分表の「こめ 精白米 うるち米 めし」150g 分
    nutrition: { kcal: 234, protein: 3.8, fat: 0.5, carb: 55.7, salt: 0 },
  };

  /**
   * @param {RecipeCatalog} catalog
   * @param {MenuStore} store
   */
  constructor(catalog, store) {
    this.catalog = catalog;
    this.store = store;
    const state = store.load(CalendarDay.today());
    this.day = state.day;
    // 目録から消えた料理は読み捨てる
    this.state = state.items.filter((x) => x.id === MenuModel.RICE.id || catalog.find(x.id));
    /** @type {Set<() => void>} */
    this.listeners = new Set();
  }

  /** @param {() => void} listener */
  onChange(listener) {
    this.listeners.add(listener);
  }

  /**
   * @param {string} id
   * @returns {boolean}
   */
  has(id) {
    return this.state.some((x) => x.id === id);
  }

  /**
   * 料理を献立に入れる。すでに入っていれば外す
   * @param {string} id
   * @param {Meal} meal
   */
  toggle(id, meal) {
    if (this.has(id)) this.update(this.state.filter((x) => x.id !== id));
    else this.update([...this.state, { id, meal: meal.key }]);
  }

  /** @param {Meal} meal */
  addRice(meal) {
    this.update([...this.state, { id: MenuModel.RICE.id, meal: meal.key }]);
  }

  /** @param {number} index */
  removeAt(index) {
    this.update(this.state.filter((_, i) => i !== index));
  }

  /**
   * @param {number} index
   * @param {Meal} meal
   */
  moveTo(index, meal) {
    this.update(this.state.map((x, i) => (i === index ? { ...x, meal: meal.key } : x)));
  }

  clear() {
    this.update([]);
  }

  /** @param {MenuItem[]} items */
  update(items) {
    this.state = items;
    // 献立を触った日を、その献立の日付とする
    this.day = CalendarDay.today();
    this.store.save({ day: this.day, items });
    for (const listener of this.listeners) listener();
  }

  /** @returns {boolean} 前の日に作った献立のままか */
  isStale() {
    return this.state.length > 0 && this.day !== CalendarDay.today();
  }

  /** @returns {ResolvedItem[]} */
  items() {
    return this.state.map((x, index) => {
      const meal = Meal.of(x.meal);
      if (x.id === MenuModel.RICE.id) {
        return { index, id: x.id, meal, name: MenuModel.RICE.name, href: null, image: null, nutrition: MenuModel.RICE.nutrition };
      }
      const d = /** @type {RecipeData} */ (this.catalog.find(x.id));
      return {
        index, id: x.id, meal, name: d.title ?? d.name,
        href: `${d.slug}.html`, image: `images/${d.slug}/hero.jpg`, nutrition: d.nutrition,
      };
    });
  }

  /**
   * @param {Meal} [meal] 省略すると 1 日分
   * @returns {Nutrition}
   */
  totals(meal) {
    return Nutrient.sum(this.items().filter((x) => !meal || x.meal === meal).map((x) => x.nutrition));
  }
}

/**
 * 一覧ページの献立の枠。各カードに「献立に追加」を付け、画面下に品数とカロリーの合計を出す。
 * 内訳は今日の献立のページで見る。
 */
class MenuBar {
  /**
   * @param {HTMLElement} root
   * @param {MenuModel} model
   * @param {CardList} list
   */
  constructor(root, model, list) {
    this.root = root;
    this.model = model;
    /** @type {Map<string, HTMLButtonElement>} */
    this.buttons = new Map();
    for (const entry of list.catalog.entries) {
      const slug = entry.data.slug;
      const btn = Dom.button("card__add", "", () => model.toggle(slug, Meal.current()));
      list.cardOf(slug)?.append(btn);
      this.buttons.set(slug, btn);
    }
    model.onChange(() => this.render());
    this.render();
  }

  render() {
    for (const [slug, btn] of this.buttons) {
      const added = this.model.has(slug);
      btn.textContent = added ? "献立から外す" : `＋ ${Meal.current().label}に追加`;
      btn.setAttribute("aria-pressed", String(added));
    }

    const items = this.model.items();
    this.root.hidden = items.length === 0;
    this.root.replaceChildren();
    if (items.length === 0) return;

    const link = Dom.el("a", "menu__bar");
    link.href = "menu.html";
    link.append(
      Dom.el("span", "menu__bar-title", `今日の献立 ${items.length}品`),
      Dom.el("span", "menu__bar-total", Nutrient.ALL[0].format(this.model.totals().kcal)),
      Dom.el("span", "menu__bar-toggle", "献立を見る →"));
    this.root.append(link);
  }
}

/**
 * 1 日の摂取量の目安。日本人の食事摂取基準(2020年版)の 30〜49 歳、身体活動レベルふつうの値。
 * 二人で使うアプリなので、男女の値を並べて示す。
 */
class DailyReference {
  static ENERGY = { male: 2700, female: 2050 };
  static SALT = { male: 7.5, female: 6.5 };
  /** エネルギーに占める割合の目標量(%)。たんぱく質、脂質、炭水化物の順 */
  static PFC = [
    { key: "protein", label: "たんぱく質", factor: 4, min: 13, max: 20 },
    { key: "fat", label: "脂質", factor: 9, min: 20, max: 30 },
    { key: "carb", label: "炭水化物", factor: 4, min: 50, max: 65 },
  ];
}

/** 今日の献立のページ。食事ごとに品を並べ、小計、1 日の合計、PFC バランスを出す */
class MenuPage {
  /**
   * @param {HTMLElement} root
   * @param {MenuModel} model
   */
  constructor(root, model) {
    this.root = root;
    this.model = model;
    model.onChange(() => this.render());
    this.render();
  }

  render() {
    const items = this.model.items();
    const children = [this.renderHeading()];
    if (items.length === 0) {
      children.push(this.renderEmpty());
    } else {
      children.push(this.renderSummary(), ...Meal.ALL.map((m) => this.renderMeal(m, items)), this.renderBalance(), this.renderReference(), this.renderActions());
    }
    this.root.replaceChildren(...children);
  }

  /** @returns {HTMLElement} */
  renderHeading() {
    const head = Dom.el("header", "plan__head");
    head.append(Dom.el("p", "plan__date", `${CalendarDay.label(this.model.day)}の献立`));
    if (this.model.isStale()) {
      head.append(Dom.el("p", "plan__stale", "前の日に作った献立です。料理を足したり外したりすると、今日の献立になります。"));
    }
    return head;
  }

  /** @returns {HTMLElement} */
  renderEmpty() {
    const box = Dom.el("section", "plan__empty");
    const link = Dom.el("a", "", "レシピ一覧");
    link.href = "index.html";
    const p = Dom.el("p", "", "まだ料理が入っていません。");
    const p2 = Dom.el("p");
    p2.append(link, "のカードの「献立に追加」で料理を選んでください。");
    box.append(p, p2);
    return box;
  }

  /** @returns {HTMLElement} 1 日の合計 */
  renderSummary() {
    const totals = this.model.totals();
    const section = Dom.el("section", "plan__summary");
    section.setAttribute("aria-label", "1日の合計");
    section.append(Dom.el("h2", "section-title", "1日の合計"));
    const grid = Dom.el("dl", "plan__totals");
    for (const n of Nutrient.ALL) {
      const cell = Dom.el("div", `plan__total plan__total--${n.key}`);
      cell.append(Dom.el("dt", "", n.label), Dom.el("dd", "", n.format(totals[n.key])));
      grid.append(cell);
    }
    section.append(grid, Dom.el("p", "plan__note", "レシピの材料から概算した 1 人分の目安の合計です。"));
    return section;
  }

  /**
   * @param {Meal} meal
   * @param {ResolvedItem[]} all
   * @returns {HTMLElement}
   */
  renderMeal(meal, all) {
    const items = all.filter((x) => x.meal === meal);
    const section = Dom.el("section", "plan__meal");
    const head = Dom.el("div", "plan__meal-head");
    head.append(
      Dom.el("h2", "plan__meal-title", meal.label),
      Dom.el("span", "plan__meal-kcal", items.length ? Nutrient.ALL[0].format(this.model.totals(meal).kcal) : "まだありません"));
    section.append(head);

    if (items.length > 0) {
      const list = Dom.el("ul", "plan__list");
      for (const item of items) list.append(this.renderItem(item));
      section.append(list);
    }
    section.append(Dom.button("plan__rice", "＋ ご飯を1杯足す", () => this.model.addRice(meal)));
    return section;
  }

  /**
   * @param {ResolvedItem} item
   * @returns {HTMLElement}
   */
  renderItem(item) {
    const li = Dom.el("li", "plan__item");
    const thumb = Dom.el("div", "plan__thumb");
    if (item.image) {
      const img = Dom.el("img");
      img.src = item.image;
      img.alt = "";
      img.loading = "lazy";
      thumb.append(img);
    }
    const name = item.href ? Dom.el("a", "plan__name", item.name) : Dom.el("span", "plan__name", item.name);
    if (item.href && name instanceof HTMLAnchorElement) name.href = item.href;
    const n = item.nutrition;
    const detail = Dom.el("span", "plan__detail",
      `${Nutrient.ALL[0].format(n.kcal)} / たんぱく質 ${Math.round(n.protein)}g / 塩分 ${n.salt.toFixed(1)}g`);
    const info = Dom.el("div", "plan__info");
    info.append(name, detail);

    const select = /** @type {HTMLSelectElement} */ (Dom.el("select", "plan__select"));
    select.setAttribute("aria-label", `${item.name}の食事`);
    for (const m of Meal.ALL) {
      const opt = /** @type {HTMLOptionElement} */ (Dom.el("option", "", m.label));
      opt.value = m.key;
      opt.selected = m === item.meal;
      select.append(opt);
    }
    select.addEventListener("change", () => this.model.moveTo(item.index, Meal.of(select.value)));
    const remove = Dom.button("plan__remove", "外す", () => this.model.removeAt(item.index));
    remove.setAttribute("aria-label", `${item.name}を献立から外す`);
    const actions = Dom.el("div", "plan__item-actions");
    actions.append(select, remove);

    li.append(thumb, info, actions);
    return li;
  }

  /** @returns {HTMLElement} PFC バランス。エネルギーに占める割合を目標の範囲と並べる */
  renderBalance() {
    const t = this.model.totals();
    const energy = DailyReference.PFC.reduce((sum, p) => sum + t[p.key] * p.factor, 0);
    const section = Dom.el("section", "plan__balance");
    section.append(Dom.el("h2", "section-title", "PFCバランス"));
    const list = Dom.el("ul", "balance");
    for (const p of DailyReference.PFC) {
      const ratio = energy > 0 ? (t[p.key] * p.factor / energy) * 100 : 0;
      const inRange = ratio >= p.min && ratio <= p.max;
      const li = Dom.el("li", `balance__row${inRange ? "" : " balance__row--out"}`);
      const bar = Dom.el("div", "balance__bar");
      const target = Dom.el("span", "balance__target");
      target.style.left = `${p.min}%`;
      target.style.width = `${p.max - p.min}%`;
      const fill = Dom.el("span", "balance__fill");
      fill.style.width = `${Math.min(ratio, 100)}%`;
      bar.append(target, fill);
      li.append(
        Dom.el("span", "balance__label", p.label),
        bar,
        Dom.el("span", "balance__value", `${Math.round(ratio)}%(目標 ${p.min}〜${p.max}%)`));
      list.append(li);
    }
    section.append(list, Dom.el("p", "plan__note", "色の帯が目標の範囲です。たんぱく質、脂質、炭水化物から計算したエネルギーに占める割合を表しています。"));
    return section;
  }

  /** @returns {HTMLElement} 1 日の目安と比べる */
  renderReference() {
    const t = this.model.totals();
    const e = DailyReference.ENERGY;
    const s = DailyReference.SALT;
    const kcal = Nutrient.ALL[0];
    const salt = Nutrient.ALL[4];
    const section = Dom.el("section", "plan__reference");
    section.append(Dom.el("h2", "section-title", "1日の目安と比べる"));
    const table = Dom.el("table", "reference");
    const head = Dom.el("tr");
    head.append(Dom.el("th", "", ""), Dom.el("th", "", "今日の合計"), Dom.el("th", "", "男性の目安"), Dom.el("th", "", "女性の目安"));
    const energyRow = Dom.el("tr");
    energyRow.append(Dom.el("th", "", "エネルギー"), Dom.el("td", "", kcal.format(t.kcal)),
      Dom.el("td", "", `${kcal.format(e.male)}(${Math.round((t.kcal / e.male) * 100)}%)`),
      Dom.el("td", "", `${kcal.format(e.female)}(${Math.round((t.kcal / e.female) * 100)}%)`));
    const saltRow = Dom.el("tr");
    saltRow.append(Dom.el("th", "", "塩分"), Dom.el("td", "", salt.format(t.salt)),
      Dom.el("td", "", `${salt.format(s.male)}未満`), Dom.el("td", "", `${salt.format(s.female)}未満`));
    table.append(head, energyRow, saltRow);
    section.append(table, Dom.el("p", "plan__note",
      "目安は日本人の食事摂取基準(2020年版)の 30〜49 歳、身体活動レベルふつうの値です。エネルギーは推定エネルギー必要量、塩分は目標量です。"));
    return section;
  }

  /** @returns {HTMLElement} */
  renderActions() {
    const box = Dom.el("div", "plan__actions");
    const add = Dom.el("a", "plan__action", "料理を足す");
    add.href = "index.html";
    const clear = Dom.button("plan__action plan__action--sub", "献立をすべて外す", () => {
      if (window.confirm("献立をすべて外しますか?")) this.model.clear();
    });
    box.append(add, clear);
    return box;
  }
}

/** 要素を作る小さな道具 */
class Dom {
  /**
   * @param {string} tag
   * @param {string} [className]
   * @param {string} [text]
   * @returns {HTMLElement}
   */
  static el(tag, className = "", text = "") {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text) node.textContent = text;
    return node;
  }

  /**
   * @param {string} className
   * @param {string} text
   * @param {() => void} onClick
   * @returns {HTMLButtonElement}
   */
  static button(className, text, onClick) {
    const btn = /** @type {HTMLButtonElement} */ (Dom.el("button", className, text));
    btn.type = "button";
    btn.addEventListener("click", onClick);
    return btn;
  }
}

document.addEventListener("DOMContentLoaded", () => {
  const card = document.querySelector("[data-base-servings]");
  if (card) new ServingsScaler(card, new QuantityFormatter());

  const steps = document.querySelector(".steps");
  if (steps) new StepTracker(steps);

  // 一覧と献立のページは catalog.js の目録を読む。レシピのページでは読み込まない
  if (typeof RECIPE_CATALOG === "undefined") return;
  const normalizer = new TextNormalizer();
  const catalog = new RecipeCatalog(RECIPE_CATALOG, normalizer);
  const model = new MenuModel(catalog, new MenuStore());

  const container = document.querySelector("#recipe-cards");
  if (container instanceof HTMLElement) {
    const list = new CardList(container, catalog);

    const status = document.querySelector("[data-catalog-status]");
    if (status instanceof HTMLElement) new CatalogStatus(status, list);

    const filter = document.querySelector("[data-genre-filter]");
    if (filter instanceof HTMLElement) new GenreFilter(filter, list);

    const search = document.querySelector("[data-recipe-search]");
    if (search instanceof HTMLInputElement) new RecipeSearch(search, list, normalizer);

    const bar = document.querySelector("[data-menu-bar]");
    if (bar instanceof HTMLElement) new MenuBar(bar, model, list);
  }

  const page = document.querySelector("[data-menu-page]");
  if (page instanceof HTMLElement) new MenuPage(page, model);
});
