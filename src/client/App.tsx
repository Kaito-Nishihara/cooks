import { NavLink, Route, Routes } from "react-router";
import { HomePage } from "./routes/HomePage";
import { IngredientsPage } from "./routes/IngredientsPage";

/**
 * 画面の振り分け。
 * ここでのパスはブラウザの URL であり、Worker のルートではない。
 * Worker はどのパスに対しても同じ HTML を返す(wrangler.jsonc の single-page-application)。
 */
export function App(): React.ReactElement {
  return (
    <div className="min-h-dvh pb-16">
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/ingredients" element={<IngredientsPage />} />
      </Routes>
      <NavBar />
    </div>
  );
}

/** 画面下部の移動。親指の届く位置に置く。 */
function NavBar(): React.ReactElement {
  return (
    <nav className="fixed inset-x-0 bottom-0 border-t border-gray-200 bg-white pb-[env(safe-area-inset-bottom)] dark:border-gray-800 dark:bg-gray-950">
      <ul className="mx-auto flex max-w-2xl">
        <NavItem to="/" label="ホーム" />
        <NavItem to="/ingredients" label="食材と好み" />
      </ul>
    </nav>
  );
}

function NavItem({ to, label }: { to: string; label: string }): React.ReactElement {
  return (
    <li className="flex-1">
      <NavLink
        to={to}
        end
        className={({ isActive }) =>
          `block py-3 text-center text-sm ${isActive ? "font-bold" : "text-gray-500"}`
        }
      >
        {label}
      </NavLink>
    </li>
  );
}
