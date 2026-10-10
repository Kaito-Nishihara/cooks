import { Hono } from "hono";
import { z } from "zod";
import {
  ingredientInputSchema,
  ingredientListQuerySchema,
  type IngredientListResponse,
} from "@/shared/ingredient";
import { getDb } from "../db/client";
import {
  createIngredient,
  deleteIngredient,
  listIngredients,
  updateIngredient,
} from "../db/queries/ingredients";
import { validate } from "../lib/validator";
import { requireUser, type AppEnv } from "../middleware/auth";

export const ingredientRoutes = new Hono<AppEnv>();

const idParamSchema = z.object({
  id: z.coerce.number().int().positive(),
});

ingredientRoutes.use("/api/ingredients/*", requireUser);
ingredientRoutes.use("/api/ingredients", requireUser);

/** 食材の一覧。名称と分類で絞り込める。 */
ingredientRoutes.get("/api/ingredients", validate("query", ingredientListQuerySchema), async (c) => {
  const { q, category } = c.req.valid("query");
  const { items, truncated } = await listIngredients(getDb(c.env), { q, category });

  const body: IngredientListResponse = {
    ingredients: items.map((item) => ({
      id: item.id,
      name: item.name,
      category: item.category,
      standardUnit: item.standardUnit,
    })),
    truncated,
  };
  return c.json(body);
});

/** 食材を登録する。食材マスタは二人の共有データのため、役割による制限は設けない。 */
ingredientRoutes.post("/api/ingredients", validate("json", ingredientInputSchema), async (c) => {
  const created = await createIngredient(getDb(c.env), c.req.valid("json"));
  return c.json(created, 201);
});

ingredientRoutes.patch(
  "/api/ingredients/:id",
  validate("param", idParamSchema),
  validate("json", ingredientInputSchema),
  async (c) => {
    const { id } = c.req.valid("param");
    const updated = await updateIngredient(getDb(c.env), id, c.req.valid("json"));
    return c.json(updated);
  },
);

/** 食材を削除する。参照されている場合は 409 を返す。 */
ingredientRoutes.delete("/api/ingredients/:id", validate("param", idParamSchema), async (c) => {
  const { id } = c.req.valid("param");
  await deleteIngredient(getDb(c.env), id);
  return c.body(null, 204);
});
