import { Hono } from "hono";
import type { CurrentUser } from "@/shared/user";
import { requireUser, type AppEnv } from "../middleware/auth";

export const meRoutes = new Hono<AppEnv>();

/** 現在ログインしている利用者。画面はこれで表示名と役割を得る。 */
meRoutes.get("/api/me", requireUser, (c) => {
  const user = c.get("user");
  const body: CurrentUser = {
    id: user.id,
    email: user.email,
    displayName: user.displayName,
    role: user.role,
  };
  return c.json(body);
});
