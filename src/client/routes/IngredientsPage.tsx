import { useCallback, useEffect, useMemo, useState } from "react";
import { indexPreferences } from "@/domain/preference";
import {
  INGREDIENT_CATEGORIES,
  INGREDIENT_CATEGORY_LABELS,
  PREFERENCE_LABELS,
  PREFERENCE_LEVELS,
  type IngredientCategory,
  type PreferenceLevel,
} from "@/shared/enums";
import type { Ingredient } from "@/shared/ingredient";
import type { Preference } from "@/shared/preference";
import { ApiError } from "../api/request";
import {
  clearPreference,
  createIngredient,
  deleteIngredient,
  fetchIngredients,
  fetchPreferences,
  savePreference,
} from "../api/ingredients";
import { useCurrentUser } from "../hooks/useCurrentUser";

interface Data {
  ingredients: Ingredient[];
  truncated: boolean;
  users: { id: number; displayName: string }[];
  preferences: Preference[];
}

/**
 * 食材と好き嫌いの画面。
 * 分類ごとに食材を並べ、二人分の嗜好を横に並べて比較できるようにする。
 */
export function IngredientsPage(): React.ReactElement {
  const session = useCurrentUser();
  const [data, setData] = useState<Data | undefined>();
  const [query, setQuery] = useState("");
  const [error, setError] = useState<string | undefined>();
  const [loading, setLoading] = useState(true);

  const load = useCallback(async (q: string) => {
    setLoading(true);
    try {
      const [list, prefs] = await Promise.all([fetchIngredients({ q }), fetchPreferences()]);
      setData({
        ingredients: list.ingredients,
        truncated: list.truncated,
        users: prefs.users,
        preferences: prefs.preferences,
      });
      setError(undefined);
    } catch (err) {
      setError(err instanceof Error ? err.message : "読み込みに失敗した");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // 入力のたびに問い合わせないよう、少し待ってからまとめて送る。
    const timer = setTimeout(() => void load(query), 250);
    return () => clearTimeout(timer);
  }, [query, load]);

  const byUser = useMemo(() => indexPreferences(data?.preferences ?? []), [data]);

  const grouped = useMemo(() => {
    const map = new Map<IngredientCategory, Ingredient[]>();
    for (const item of data?.ingredients ?? []) {
      const bucket = map.get(item.category);
      if (bucket) bucket.push(item);
      else map.set(item.category, [item]);
    }
    return INGREDIENT_CATEGORIES.filter((c) => map.has(c)).map((c) => ({
      category: c,
      items: map.get(c) ?? [],
    }));
  }, [data]);

  if (session.status !== "ready") {
    return <Shell>{session.status === "loading" ? "読み込み中" : "利用できない"}</Shell>;
  }

  const currentUser = session.user;
  const canEdit = (userId: number): boolean =>
    userId === currentUser.id || currentUser.role === "OWNER";

  const handleChange = async (
    userId: number,
    ingredientId: number,
    level: PreferenceLevel,
  ): Promise<void> => {
    try {
      // 行が無い状態が「普通」を表すため、NEUTRAL は登録の削除で表現する。
      if (level === "NEUTRAL") await clearPreference(userId, ingredientId);
      else await savePreference(userId, ingredientId, level);
      await load(query);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "保存に失敗した");
    }
  };

  const handleDelete = async (ingredient: Ingredient): Promise<void> => {
    if (!confirm(`${ingredient.name} を削除する。よいか。`)) return;
    try {
      await deleteIngredient(ingredient.id);
      await load(query);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "削除に失敗した");
    }
  };

  return (
    <Shell>
      <AddIngredientForm
        onAdded={() => void load(query)}
        onError={(message) => setError(message)}
      />

      <input
        type="search"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder="食材を探す"
        className="mt-4 w-full rounded border border-gray-300 px-3 py-2 dark:border-gray-700 dark:bg-gray-900"
      />

      {error && (
        <p role="alert" className="mt-3 rounded bg-red-50 p-2 text-sm text-red-700">
          {error}
        </p>
      )}

      {data?.truncated && (
        <p className="mt-3 text-sm text-amber-700">
          件数が多いため一部だけ表示している。絞り込んで探すこと。
        </p>
      )}

      {loading && !data && <p className="mt-4 text-sm">読み込み中</p>}

      {data && data.ingredients.length === 0 && (
        <p className="mt-4 text-sm text-gray-600 dark:text-gray-400">該当する食材が無い。</p>
      )}

      {grouped.map(({ category, items }) => (
        <section key={category} className="mt-6">
          <h2 className="text-sm font-bold text-gray-500">
            {INGREDIENT_CATEGORY_LABELS[category]}
          </h2>
          <ul className="mt-2 divide-y divide-gray-200 dark:divide-gray-800">
            {items.map((ingredient) => (
              <li key={ingredient.id} className="py-3">
                <div className="flex items-baseline justify-between gap-2">
                  <span className="font-medium">{ingredient.name}</span>
                  <button
                    type="button"
                    onClick={() => void handleDelete(ingredient)}
                    className="text-xs text-gray-500 underline"
                  >
                    削除
                  </button>
                </div>
                <div className="mt-2 grid grid-cols-2 gap-2">
                  {(data?.users ?? []).map((user) => {
                    const preference = byUser.get(user.id)?.get(ingredient.id);
                    const level = preference?.level ?? "NEUTRAL";
                    return (
                      <label key={user.id} className="text-xs">
                        <span className="block text-gray-500">{user.displayName}</span>
                        <PreferenceSelect
                          level={level}
                          disabled={!canEdit(user.id)}
                          onChange={(next) => void handleChange(user.id, ingredient.id, next)}
                        />
                        {preference?.note && (
                          <span className="mt-1 block text-gray-500">{preference.note}</span>
                        )}
                      </label>
                    );
                  })}
                </div>
              </li>
            ))}
          </ul>
        </section>
      ))}
    </Shell>
  );
}

/**
 * 嗜好の選択。
 * アレルギーは安全に関わるため、選択中は色で他と区別する。
 */
function PreferenceSelect({
  level,
  disabled,
  onChange,
}: {
  level: PreferenceLevel;
  disabled: boolean;
  onChange: (level: PreferenceLevel) => void;
}): React.ReactElement {
  const emphasis =
    level === "ALLERGY"
      ? "border-red-500 bg-red-50 font-bold text-red-700"
      : level === "HATE"
        ? "border-orange-400 text-orange-700"
        : "border-gray-300 dark:border-gray-700 dark:bg-gray-900";

  return (
    <select
      value={level}
      disabled={disabled}
      onChange={(e) => onChange(e.target.value as PreferenceLevel)}
      className={`mt-1 w-full rounded border px-2 py-1 disabled:opacity-50 ${emphasis}`}
    >
      {PREFERENCE_LEVELS.map((value) => (
        <option key={value} value={value}>
          {PREFERENCE_LABELS[value]}
        </option>
      ))}
    </select>
  );
}

function AddIngredientForm({
  onAdded,
  onError,
}: {
  onAdded: () => void;
  onError: (message: string) => void;
}): React.ReactElement {
  const [name, setName] = useState("");
  const [category, setCategory] = useState<IngredientCategory>("VEGETABLE");
  const [unit, setUnit] = useState("g");
  const [saving, setSaving] = useState(false);

  const submit = async (event: React.FormEvent): Promise<void> => {
    event.preventDefault();
    if (!name.trim()) return;
    setSaving(true);
    try {
      await createIngredient({ name: name.trim(), category, standardUnit: unit.trim() });
      setName("");
      onAdded();
    } catch (err) {
      onError(err instanceof ApiError ? err.message : "登録に失敗した");
    } finally {
      setSaving(false);
    }
  };

  return (
    <form onSubmit={(e) => void submit(e)} className="flex flex-wrap gap-2">
      <input
        value={name}
        onChange={(e) => setName(e.target.value)}
        placeholder="食材を追加"
        className="min-w-0 flex-1 rounded border border-gray-300 px-3 py-2 dark:border-gray-700 dark:bg-gray-900"
      />
      <select
        value={category}
        onChange={(e) => setCategory(e.target.value as IngredientCategory)}
        className="rounded border border-gray-300 px-2 py-2 dark:border-gray-700 dark:bg-gray-900"
      >
        {INGREDIENT_CATEGORIES.map((value) => (
          <option key={value} value={value}>
            {INGREDIENT_CATEGORY_LABELS[value]}
          </option>
        ))}
      </select>
      <input
        value={unit}
        onChange={(e) => setUnit(e.target.value)}
        aria-label="単位"
        className="w-16 rounded border border-gray-300 px-2 py-2 dark:border-gray-700 dark:bg-gray-900"
      />
      <button
        type="submit"
        disabled={saving}
        className="rounded bg-gray-800 px-4 py-2 text-white disabled:opacity-50 dark:bg-gray-200 dark:text-gray-900"
      >
        追加
      </button>
    </form>
  );
}

function Shell({ children }: { children: React.ReactNode }): React.ReactElement {
  return (
    <main className="mx-auto max-w-2xl p-4">
      <h1 className="text-xl font-bold">食材と好み</h1>
      <div className="mt-3">{children}</div>
    </main>
  );
}
