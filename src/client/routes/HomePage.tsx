import { useCurrentUser } from "../hooks/useCurrentUser";

/**
 * 仮のホーム画面。
 * いまは認証が通っていることと、利用者を引けることだけを確認する。
 */
export function HomePage(): React.ReactElement {
  const session = useCurrentUser();

  if (session.status === "loading") {
    return <Shell>読み込み中</Shell>;
  }

  if (session.status === "not-registered") {
    return (
      <Shell>
        <p className="text-sm">
          このアカウントは mana-cooks の利用者として登録されていない。
          登録するには、データベースの利用者一覧に追加する必要がある。
        </p>
      </Shell>
    );
  }

  if (session.status === "error") {
    return <Shell>読み込みに失敗した: {session.message}</Shell>;
  }

  return (
    <Shell>
      <p className="text-sm text-gray-600 dark:text-gray-400">
        {session.user.displayName} としてログイン中
      </p>
    </Shell>
  );
}

function Shell({ children }: { children: React.ReactNode }): React.ReactElement {
  return (
    <main className="mx-auto max-w-2xl p-4">
      <h1 className="text-2xl font-bold">mana-cooks</h1>
      <div className="mt-2">{children}</div>
    </main>
  );
}
