import { describe, expect, it } from "vitest";
import type { Preference } from "@/shared/preference";
import {
  indexPreferences,
  isAlwaysExcluded,
  isExcludedByDefault,
  resolveLevel,
  scoreOf,
} from "./index";

const preference = (
  userId: number,
  ingredientId: number,
  level: Preference["level"],
): Preference => ({ userId, ingredientId, level, note: null });

describe("resolveLevel", () => {
  const preferences = [preference(1, 10, "LOVE"), preference(2, 10, "ALLERGY")];

  it("登録された嗜好を返す", () => {
    expect(resolveLevel(preferences, 1, 10)).toBe("LOVE");
    expect(resolveLevel(preferences, 2, 10)).toBe("ALLERGY");
  });

  it("登録が無い食材は NEUTRAL として扱う", () => {
    expect(resolveLevel(preferences, 1, 99)).toBe("NEUTRAL");
  });

  it("別の利用者の嗜好を取り違えない", () => {
    // 同じ食材に対して二人が別の嗜好を持つ場合に、利用者を跨いで拾わないこと。
    expect(resolveLevel(preferences, 1, 10)).not.toBe(resolveLevel(preferences, 2, 10));
  });

  it("嗜好が一件も無ければ NEUTRAL を返す", () => {
    expect(resolveLevel([], 1, 10)).toBe("NEUTRAL");
  });
});

describe("indexPreferences", () => {
  it("利用者ごと、食材ごとに引ける形へまとめる", () => {
    const index = indexPreferences([
      preference(1, 10, "LOVE"),
      preference(1, 11, "DISLIKE"),
      preference(2, 10, "HATE"),
    ]);

    expect(index.get(1)?.get(10)?.level).toBe("LOVE");
    expect(index.get(1)?.get(11)?.level).toBe("DISLIKE");
    expect(index.get(2)?.get(10)?.level).toBe("HATE");
    expect(index.get(2)?.get(11)).toBeUndefined();
  });

  it("空の入力では空の索引を返す", () => {
    expect(indexPreferences([]).size).toBe(0);
  });
});

describe("除外の判定", () => {
  it("アレルギーは常に除外する", () => {
    // 安全に関わるため、画面の切り替えでも表示させてはならない。
    expect(isAlwaysExcluded("ALLERGY")).toBe(true);
    expect(isExcludedByDefault("ALLERGY")).toBe(false);
  });

  it("食べられないものは既定で除外し、切り替えで表示できる", () => {
    expect(isAlwaysExcluded("HATE")).toBe(false);
    expect(isExcludedByDefault("HATE")).toBe(true);
  });

  it("苦手は除外せず、減点で表す", () => {
    expect(isAlwaysExcluded("DISLIKE")).toBe(false);
    expect(isExcludedByDefault("DISLIKE")).toBe(false);
    expect(scoreOf("DISLIKE")).toBeLessThan(0);
  });

  it("好き嫌いの無い嗜好は除外しない", () => {
    for (const level of ["LOVE", "LIKE", "NEUTRAL"] as const) {
      expect(isAlwaysExcluded(level)).toBe(false);
      expect(isExcludedByDefault(level)).toBe(false);
    }
  });
});

describe("scoreOf", () => {
  it("要件どおりの重みを返す", () => {
    expect(scoreOf("LOVE")).toBe(3);
    expect(scoreOf("LIKE")).toBe(1);
    expect(scoreOf("NEUTRAL")).toBe(0);
    expect(scoreOf("DISLIKE")).toBe(-2);
  });

  it("除外の対象になる嗜好は加点減点に混ぜない", () => {
    // 除外と減点を二重に効かせないため、ここでは 0 とする。
    expect(scoreOf("HATE")).toBe(0);
    expect(scoreOf("ALLERGY")).toBe(0);
  });
});
