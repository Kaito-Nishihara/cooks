import { relations } from "drizzle-orm";
import {
  index,
  integer,
  primaryKey,
  real,
  sqliteTable,
  text,
  uniqueIndex,
} from "drizzle-orm/sqlite-core";
import {
  INGREDIENT_CATEGORIES,
  MEAL_TYPES,
  PREFERENCE_LEVELS,
  RECIPE_CATEGORIES,
  USER_ROLES,
} from "@/shared/enums";

/**
 * 日時と日付の使い分け。
 *
 * 記録した瞬間を表す値(createdAt 等)は Unix 時間の整数で持つ。
 * 暦の上の一日を表す値(献立の日付、調理した日)は "YYYY-MM-DD" の文字列で持つ。
 * 後者を時刻付きで持つと、時間帯の扱いで前後の日にずれるためである。
 * 文字列でも辞書順と日付順が一致するため、範囲の絞り込みと並べ替えに使える。
 */
const timestamps = {
  createdAt: integer("created_at", { mode: "timestamp" }).notNull(),
  updatedAt: integer("updated_at", { mode: "timestamp" }).notNull(),
};

// ---------------------------------------------------------------------------
// 利用者
// ---------------------------------------------------------------------------

/**
 * 利用者。
 *
 * 認証は Cloudflare Access が担うため、パスワードを保持しない。
 * Access が確認したメールアドレスと email を突き合わせて、この表の行を引く。
 * ここに行が無いメールアドレスは、Access を通っていてもアプリを使えない。
 * 初期ユーザーはシード(scripts/seed.ts)で作成する。
 */
export const users = sqliteTable(
  "users",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    email: text("email").notNull(),
    displayName: text("display_name").notNull(),
    role: text("role", { enum: USER_ROLES }).notNull(),
    ...timestamps,
  },
  (t) => [uniqueIndex("users_email_idx").on(t.email)],
);

// ---------------------------------------------------------------------------
// 食材
// ---------------------------------------------------------------------------

/**
 * 食材マスタ。人が選ぶ単位で持つ。
 * 成分表(public/data/foods.json)より粗く、「こまつな」のような日常の呼び名で登録する。
 */
export const ingredients = sqliteTable(
  "ingredients",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    name: text("name").notNull(),
    category: text("category", { enum: INGREDIENT_CATEGORIES }).notNull(),
    /** 分量の既定の単位。"g" や "個" など。 */
    standardUnit: text("standard_unit").notNull(),
    /**
     * 成分表の代表食品番号。public/data/foods.json のキーを指す。
     * JSON への参照のため外部キー制約は張れない。存在検証はアプリ側で行う。
     * 未設定の食材は栄養計算の対象外になる。
     */
    foodNumber: text("food_number"),
    ...timestamps,
  },
  (t) => [uniqueIndex("ingredients_name_idx").on(t.name)],
);

/** 食材の別名。表記ゆれを吸収して検索に使う。 */
export const ingredientAliases = sqliteTable(
  "ingredient_aliases",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    ingredientId: integer("ingredient_id")
      .notNull()
      .references(() => ingredients.id, { onDelete: "cascade" }),
    name: text("name").notNull(),
  },
  (t) => [uniqueIndex("ingredient_aliases_name_idx").on(t.name)],
);

/**
 * 単位から g への換算率。
 * 初期値は public/data/unit-conversions.json から投入し、以降は利用者が編集する。
 * 未登録の組み合わせは栄養計算の対象外になる。
 */
export const unitConversions = sqliteTable(
  "unit_conversions",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    ingredientId: integer("ingredient_id")
      .notNull()
      .references(() => ingredients.id, { onDelete: "cascade" }),
    /** "大さじ"、"個" など。材料の分量の単位と一致させる。 */
    unit: text("unit").notNull(),
    /** この単位 1 つあたりのグラム数。 */
    grams: real("grams").notNull(),
    ...timestamps,
  },
  (t) => [uniqueIndex("unit_conversions_ingredient_unit_idx").on(t.ingredientId, t.unit)],
);

/** 利用者ごとの嗜好。未登録の食材は NEUTRAL として扱うため、行が無い状態が普通である。 */
export const ingredientPreferences = sqliteTable(
  "ingredient_preferences",
  {
    userId: integer("user_id")
      .notNull()
      .references(() => users.id, { onDelete: "cascade" }),
    ingredientId: integer("ingredient_id")
      .notNull()
      .references(() => ingredients.id, { onDelete: "cascade" }),
    level: text("level", { enum: PREFERENCE_LEVELS }).notNull(),
    /** 「火を通せば食べられる」のような補足。 */
    note: text("note"),
    ...timestamps,
  },
  (t) => [primaryKey({ columns: [t.userId, t.ingredientId] })],
);

// ---------------------------------------------------------------------------
// レシピ
// ---------------------------------------------------------------------------

/** レシピ本体。二人の共有データのため、authorId は登録者の記録にすぎず権限には使わない。 */
export const recipes = sqliteTable(
  "recipes",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    title: text("title").notNull(),
    description: text("description"),
    /** R2 のオブジェクトキー。画像そのものは D1 に置かない。 */
    imageKey: text("image_key"),
    /** 何人分か。栄養値を 1 人分へ割るときに使う。 */
    servings: integer("servings").notNull().default(2),
    prepMinutes: integer("prep_minutes"),
    cookMinutes: integer("cook_minutes"),
    sourceUrl: text("source_url"),
    category: text("category", { enum: RECIPE_CATEGORIES }).notNull(),
    authorId: integer("author_id")
      .notNull()
      .references(() => users.id),
    ...timestamps,
  },
  (t) => [index("recipes_category_idx").on(t.category), index("recipes_title_idx").on(t.title)],
);

/**
 * レシピの材料。
 * 分量は「数値 + 単位」を基本とし、数値化できないものは freeText に入れる。
 * freeText の材料と、換算率が無い材料は栄養計算から除外する。
 */
export const recipeIngredients = sqliteTable(
  "recipe_ingredients",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    recipeId: integer("recipe_id")
      .notNull()
      .references(() => recipes.id, { onDelete: "cascade" }),
    ingredientId: integer("ingredient_id")
      .notNull()
      .references(() => ingredients.id),
    quantity: real("quantity"),
    unit: text("unit"),
    /** 「少々」「適量」など数値化できない分量。quantity と排他で使う。 */
    freeText: text("free_text"),
    /** 任意材料。これに該当する場合、嗜好による除外を行わず「抜けば作れる」と表示する。 */
    optional: integer("optional", { mode: "boolean" }).notNull().default(false),
    sortOrder: integer("sort_order").notNull().default(0),
  },
  (t) => [index("recipe_ingredients_recipe_idx").on(t.recipeId)],
);

/** レシピの手順。 */
export const recipeSteps = sqliteTable(
  "recipe_steps",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    recipeId: integer("recipe_id")
      .notNull()
      .references(() => recipes.id, { onDelete: "cascade" }),
    sortOrder: integer("sort_order").notNull(),
    body: text("body").notNull(),
    imageKey: text("image_key"),
  },
  (t) => [index("recipe_steps_recipe_idx").on(t.recipeId)],
);

/** タグ。利用者が自由に増やせるため、カテゴリと違いテーブルで持つ。 */
export const tags = sqliteTable(
  "tags",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    name: text("name").notNull(),
  },
  (t) => [uniqueIndex("tags_name_idx").on(t.name)],
);

export const recipeTags = sqliteTable(
  "recipe_tags",
  {
    recipeId: integer("recipe_id")
      .notNull()
      .references(() => recipes.id, { onDelete: "cascade" }),
    tagId: integer("tag_id")
      .notNull()
      .references(() => tags.id, { onDelete: "cascade" }),
  },
  (t) => [primaryKey({ columns: [t.recipeId, t.tagId] })],
);

/** お気に入り。利用者ごとに持つ。 */
export const favorites = sqliteTable(
  "favorites",
  {
    userId: integer("user_id")
      .notNull()
      .references(() => users.id, { onDelete: "cascade" }),
    recipeId: integer("recipe_id")
      .notNull()
      .references(() => recipes.id, { onDelete: "cascade" }),
    createdAt: integer("created_at", { mode: "timestamp" }).notNull(),
  },
  (t) => [primaryKey({ columns: [t.userId, t.recipeId] })],
);

/** 調理ログ。レシピ提案で直近に作ったものを減点するために使う。 */
export const cookingLogs = sqliteTable(
  "cooking_logs",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    userId: integer("user_id")
      .notNull()
      .references(() => users.id, { onDelete: "cascade" }),
    recipeId: integer("recipe_id")
      .notNull()
      .references(() => recipes.id, { onDelete: "cascade" }),
    /** 作った日。"YYYY-MM-DD"。 */
    cookedOn: text("cooked_on").notNull(),
    /** 5 段階評価。未入力を許す。 */
    rating: integer("rating"),
    memo: text("memo"),
    createdAt: integer("created_at", { mode: "timestamp" }).notNull(),
  },
  (t) => [
    index("cooking_logs_recipe_cooked_on_idx").on(t.recipeId, t.cookedOn),
    index("cooking_logs_cooked_on_idx").on(t.cookedOn),
  ],
);

// ---------------------------------------------------------------------------
// 献立と買い物リスト
// ---------------------------------------------------------------------------

/**
 * 献立の一コマ。
 * レシピを割り当てない自由記述の予定(外食など)も登録できるため、recipeId は任意とする。
 */
export const menuEntries = sqliteTable(
  "menu_entries",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    /** 献立の日付。"YYYY-MM-DD"。 */
    date: text("date").notNull(),
    mealType: text("meal_type", { enum: MEAL_TYPES }).notNull(),
    recipeId: integer("recipe_id").references(() => recipes.id, { onDelete: "cascade" }),
    /** レシピを割り当てない場合の予定。「外食」など。 */
    note: text("note"),
    sortOrder: integer("sort_order").notNull().default(0),
    ...timestamps,
  },
  (t) => [index("menu_entries_date_idx").on(t.date, t.mealType)],
);

/** 買い物リスト。献立の期間から生成する。 */
export const shoppingLists = sqliteTable(
  "shopping_lists",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    /** 集約の対象期間。"YYYY-MM-DD"。 */
    startDate: text("start_date").notNull(),
    endDate: text("end_date").notNull(),
    ...timestamps,
  },
  (t) => [index("shopping_lists_start_date_idx").on(t.startDate)],
);

/**
 * 買い物リストの項目。
 * 単位が揃わないため分量を合算せず、レシピごとの分量を detail に併記する。
 * 手動で追加した項目は ingredientId を持たず、label だけを持つ。
 */
export const shoppingListItems = sqliteTable(
  "shopping_list_items",
  {
    id: integer("id").primaryKey({ autoIncrement: true }),
    listId: integer("list_id")
      .notNull()
      .references(() => shoppingLists.id, { onDelete: "cascade" }),
    ingredientId: integer("ingredient_id").references(() => ingredients.id),
    /** 表示名。食材から生成した場合は食材名、手動追加の場合は入力値。 */
    label: text("label").notNull(),
    /** 「カレー 200g、肉じゃが 大さじ1」のような内訳。 */
    detail: text("detail"),
    /** 購入済みの印。二人で共有する。 */
    checked: integer("checked", { mode: "boolean" }).notNull().default(false),
    sortOrder: integer("sort_order").notNull().default(0),
  },
  (t) => [index("shopping_list_items_list_idx").on(t.listId)],
);

// ---------------------------------------------------------------------------
// リレーション定義(Drizzle のクエリ API 用)
// ---------------------------------------------------------------------------

export const usersRelations = relations(users, ({ many }) => ({
  preferences: many(ingredientPreferences),
  cookingLogs: many(cookingLogs),
  favorites: many(favorites),
}));

export const ingredientsRelations = relations(ingredients, ({ many }) => ({
  aliases: many(ingredientAliases),
  unitConversions: many(unitConversions),
  preferences: many(ingredientPreferences),
  recipeIngredients: many(recipeIngredients),
}));

export const ingredientAliasesRelations = relations(ingredientAliases, ({ one }) => ({
  ingredient: one(ingredients, {
    fields: [ingredientAliases.ingredientId],
    references: [ingredients.id],
  }),
}));

export const unitConversionsRelations = relations(unitConversions, ({ one }) => ({
  ingredient: one(ingredients, {
    fields: [unitConversions.ingredientId],
    references: [ingredients.id],
  }),
}));

export const ingredientPreferencesRelations = relations(ingredientPreferences, ({ one }) => ({
  user: one(users, { fields: [ingredientPreferences.userId], references: [users.id] }),
  ingredient: one(ingredients, {
    fields: [ingredientPreferences.ingredientId],
    references: [ingredients.id],
  }),
}));

export const recipesRelations = relations(recipes, ({ one, many }) => ({
  author: one(users, { fields: [recipes.authorId], references: [users.id] }),
  ingredients: many(recipeIngredients),
  steps: many(recipeSteps),
  tags: many(recipeTags),
  favorites: many(favorites),
  cookingLogs: many(cookingLogs),
  menuEntries: many(menuEntries),
}));

export const recipeIngredientsRelations = relations(recipeIngredients, ({ one }) => ({
  recipe: one(recipes, { fields: [recipeIngredients.recipeId], references: [recipes.id] }),
  ingredient: one(ingredients, {
    fields: [recipeIngredients.ingredientId],
    references: [ingredients.id],
  }),
}));

export const recipeStepsRelations = relations(recipeSteps, ({ one }) => ({
  recipe: one(recipes, { fields: [recipeSteps.recipeId], references: [recipes.id] }),
}));

export const tagsRelations = relations(tags, ({ many }) => ({
  recipes: many(recipeTags),
}));

export const recipeTagsRelations = relations(recipeTags, ({ one }) => ({
  recipe: one(recipes, { fields: [recipeTags.recipeId], references: [recipes.id] }),
  tag: one(tags, { fields: [recipeTags.tagId], references: [tags.id] }),
}));

export const favoritesRelations = relations(favorites, ({ one }) => ({
  user: one(users, { fields: [favorites.userId], references: [users.id] }),
  recipe: one(recipes, { fields: [favorites.recipeId], references: [recipes.id] }),
}));

export const cookingLogsRelations = relations(cookingLogs, ({ one }) => ({
  user: one(users, { fields: [cookingLogs.userId], references: [users.id] }),
  recipe: one(recipes, { fields: [cookingLogs.recipeId], references: [recipes.id] }),
}));

export const menuEntriesRelations = relations(menuEntries, ({ one }) => ({
  recipe: one(recipes, { fields: [menuEntries.recipeId], references: [recipes.id] }),
}));

export const shoppingListsRelations = relations(shoppingLists, ({ many }) => ({
  items: many(shoppingListItems),
}));

export const shoppingListItemsRelations = relations(shoppingListItems, ({ one }) => ({
  list: one(shoppingLists, { fields: [shoppingListItems.listId], references: [shoppingLists.id] }),
  ingredient: one(ingredients, {
    fields: [shoppingListItems.ingredientId],
    references: [ingredients.id],
  }),
}));

// ---------------------------------------------------------------------------
// 推論型
// ---------------------------------------------------------------------------

export type User = typeof users.$inferSelect;
export type NewUser = typeof users.$inferInsert;
export type Ingredient = typeof ingredients.$inferSelect;
export type NewIngredient = typeof ingredients.$inferInsert;
export type UnitConversion = typeof unitConversions.$inferSelect;
export type IngredientPreference = typeof ingredientPreferences.$inferSelect;
export type Recipe = typeof recipes.$inferSelect;
export type NewRecipe = typeof recipes.$inferInsert;
export type RecipeIngredient = typeof recipeIngredients.$inferSelect;
export type RecipeStep = typeof recipeSteps.$inferSelect;
export type Tag = typeof tags.$inferSelect;
export type CookingLog = typeof cookingLogs.$inferSelect;
export type MenuEntry = typeof menuEntries.$inferSelect;
export type ShoppingList = typeof shoppingLists.$inferSelect;
export type ShoppingListItem = typeof shoppingListItems.$inferSelect;
