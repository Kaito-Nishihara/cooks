import { currentUserSchema, type CurrentUser } from "@/shared/user";

/** 認証は済んでいるが、このアプリの利用者として登録されていない。 */
export class NotRegisteredError extends Error {}

/** 現在ログインしている利用者を取得する。 */
export async function fetchCurrentUser(): Promise<CurrentUser> {
  const res = await fetch("/api/me");

  if (res.status === 403) {
    throw new NotRegisteredError("このアカウントは利用者として登録されていない");
  }
  if (!res.ok) {
    throw new Error(`利用者を取得できない: ${res.status}`);
  }

  return currentUserSchema.parse(await res.json());
}
