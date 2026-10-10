import { useEffect, useState } from "react";
import { fetchCurrentUser, NotRegisteredError } from "../api/me";
import type { CurrentUser } from "@/shared/user";

export type SessionState =
  | { status: "loading" }
  | { status: "ready"; user: CurrentUser }
  | { status: "not-registered" }
  | { status: "error"; message: string };

/**
 * 現在の利用者を読む。
 *
 * ログイン画面は持たない。Cloudflare Access が Worker の手前で認証を終えているため、
 * 画面が開けている時点で認証は済んでいる。
 * ここで扱うのは「認証は通ったがこのアプリの利用者ではない」場合の表示だけである。
 */
export function useCurrentUser(): SessionState {
  const [state, setState] = useState<SessionState>({ status: "loading" });

  useEffect(() => {
    let active = true;

    fetchCurrentUser()
      .then((user) => {
        if (active) setState({ status: "ready", user });
      })
      .catch((err: unknown) => {
        if (!active) return;
        if (err instanceof NotRegisteredError) {
          setState({ status: "not-registered" });
          return;
        }
        setState({
          status: "error",
          message: err instanceof Error ? err.message : "原因不明のエラー",
        });
      });

    return () => {
      active = false;
    };
  }, []);

  return state;
}
