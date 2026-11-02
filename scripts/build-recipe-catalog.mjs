// recipes/catalog/ の 1 品 1 ファイルの目録を確かめ、一覧と献立が読む recipes/assets/catalog.js にまとめる。
// まとめたファイルはリポジトリに入れない。並行の PR が同じファイルの末尾に足して衝突するためである。
// 使い方: node scripts/build-recipe-catalog.mjs [recipes のディレクトリ]
import { readdir, readFile, writeFile, access } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

/** 1 品の目録の項目を確かめる。問題を文の配列で返す */
class CatalogEntryValidator {
  static REQUIRED_STRINGS = ["slug", "name", "kicker", "reading"];
  static NUTRITION_KEYS = ["kcal", "protein", "fat", "carb", "salt"];
  // ページのカロリーと、三大栄養素から計算したカロリーの、許す差の割合
  static KCAL_TOLERANCE = 0.1;

  /**
   * @param {string} recipesDir
   */
  constructor(recipesDir) {
    this.recipesDir = recipesDir;
  }

  /**
   * @param {string} fileSlug ファイル名から拡張子を除いたもの
   * @param {any} entry
   * @returns {Promise<string[]>}
   */
  async validate(fileSlug, entry) {
    const problems = [];
    for (const key of CatalogEntryValidator.REQUIRED_STRINGS) {
      if (typeof entry[key] !== "string" || entry[key] === "") problems.push(`${key} がない`);
    }
    if (entry.slug !== fileSlug) problems.push(`slug "${entry.slug}" がファイル名と違う`);
    if (!Number.isInteger(entry.time) || entry.time <= 0) problems.push("time が正の整数でない");
    for (const key of ["genres", "ingredients"]) {
      if (!Array.isArray(entry[key]) || entry[key].length === 0) problems.push(`${key} が空`);
    }
    problems.push(...this.validateNutrition(entry.nutrition));
    if (!(await this.pageExists(fileSlug))) problems.push(`ページ ${fileSlug}.html がない`);
    return problems;
  }

  /**
   * @param {any} n
   * @returns {string[]}
   */
  validateNutrition(n) {
    if (typeof n !== "object" || n === null) return ["nutrition がない"];
    const missing = CatalogEntryValidator.NUTRITION_KEYS.filter((k) => typeof n[k] !== "number");
    if (missing.length > 0) return [`nutrition に ${missing.join("、")} がない`];
    const calc = n.protein * 4 + n.fat * 9 + n.carb * 4;
    if (Math.abs(calc - n.kcal) > n.kcal * CatalogEntryValidator.KCAL_TOLERANCE) {
      return [`kcal ${n.kcal} が三大栄養素からの計算 ${calc} と 10% を超えて違う`];
    }
    return [];
  }

  /**
   * @param {string} slug
   */
  async pageExists(slug) {
    try {
      await access(path.join(this.recipesDir, `${slug}.html`));
      return true;
    } catch {
      return false;
    }
  }
}

/** 目録のファイルを読み、確かめ、1 つの JavaScript のファイルに書き出す */
class RecipeCatalogBuilder {
  static HEADER = `// このファイルは scripts/build-recipe-catalog.mjs が recipes/catalog/ から作る。手で直さない
// 一覧(index.html)と今日の献立(menu.html)がここから料理の情報を読む
// nutrition は 1 人分の目安(kcal、たんぱく質 g、脂質 g、炭水化物 g、塩分 g)で、kcal はページの表示と同じ値にする

/**
 * @typedef {{ kcal: number, protein: number, fat: number, carb: number, salt: number }} Nutrition
 * @typedef {{ slug: string, name: string, title?: string, arrange?: boolean, kicker: string, time: number,
 *   genres: string[], reading: string, ingredients: string[], nutrition: Nutrition }} RecipeData
 */

/** @type {RecipeData[]} */
`;

  /**
   * @param {string} recipesDir
   * @param {CatalogEntryValidator} validator
   */
  constructor(recipesDir, validator) {
    this.catalogDir = path.join(recipesDir, "catalog");
    this.outFile = path.join(recipesDir, "assets", "catalog.js");
    this.validator = validator;
  }

  async build() {
    const files = (await readdir(this.catalogDir)).filter((f) => f.endsWith(".json")).sort();
    const entries = [];
    const problems = [];
    for (const file of files) {
      const slug = path.basename(file, ".json");
      let entry;
      try {
        entry = JSON.parse(await readFile(path.join(this.catalogDir, file), "utf8"));
      } catch (e) {
        problems.push(`${file}: JSON として読めない(${e.message})`);
        continue;
      }
      for (const p of await this.validator.validate(slug, entry)) problems.push(`${file}: ${p}`);
      entries.push(entry);
    }
    if (problems.length > 0) throw new Error(problems.join("\n"));
    entries.sort(RecipeCatalogBuilder.compare);
    await writeFile(this.outFile, RecipeCatalogBuilder.HEADER + `const RECIPE_CATALOG = ${JSON.stringify(entries, null, 2)};\n`);
    return entries.length;
  }

  /**
   * 読みの最初の語の五十音順に並べる。同じ読みは slug の順にする
   * @param {any} a
   * @param {any} b
   */
  static compare(a, b) {
    const ra = a.reading.split(" ")[0];
    const rb = b.reading.split(" ")[0];
    return ra.localeCompare(rb, "ja") || a.slug.localeCompare(b.slug);
  }
}

const here = path.dirname(fileURLToPath(import.meta.url));
const recipesDir = path.resolve(process.argv[2] ?? path.join(here, "..", "recipes"));
try {
  const count = await new RecipeCatalogBuilder(recipesDir, new CatalogEntryValidator(recipesDir)).build();
  console.log(`目録に ${count} 品をまとめた`);
} catch (e) {
  console.error(`目録をまとめられなかった:\n${e.message}`);
  process.exit(1);
}
