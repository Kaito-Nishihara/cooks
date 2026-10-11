// 見た目の試作の共通の処理。
// ヘッダーとフッターを、どのページにも同じ形で差し込む。開閉と切り替えだけを動かす。
// API は呼ばない。押しても、保存や画面の移動は起きない。
(function () {
  "use strict";

  /** body の属性から、試作の状態を読む */
  var body = document.body;
  var state = {
    auth: body.dataset.auth || "in", // in: ログインしている。out: ログインしていない
    current: body.dataset.current || "", // 今いる画面。recipes か menu
    book: body.dataset.book || "みさきのレシピ帳(持ち主)",
    user: body.dataset.user || "みさき",
  };

  /** 今いる画面の入口に、印を付けるための属性を返す */
  function cur(name) {
    return state.current === name ? ' aria-current="page"' : "";
  }

  /** ヘッダーの HTML を組み立てる */
  function headerHtml() {
    var nav =
      '<a href="list.html"' + cur("recipes") + ">レシピ</a>" +
      '<a href="' + (state.auth === "in" ? "menu.html" : "login.html") + '"' + cur("menu") + ">今日の献立</a>";
    var right, sheet;
    if (state.auth === "in") {
      right =
        '<label class="row t-sm">レシピ帳 <select class="select select--auto" aria-label="レシピ帳を切り替える">' +
        "<option>" + state.book + "</option><option>けんたのレシピ帳(編集できる)</option><option>実家のレシピ帳(見るだけ)</option></select></label>" +
        '<div class="usermenu"><button class="btn btn--quiet btn--sm" type="button" data-toggle="usermenu" aria-expanded="false">' + state.user + " ▼</button>" +
        '<div class="usermenu__list" id="usermenu" hidden><a href="book.html">レシピ帳の設定</a><a href="account.html">アカウント</a><a href="list-anonymous.html">ログアウト</a></div></div>';
      sheet =
        '<label class="field"><span class="field__label t-sm">レシピ帳</span><select class="select"><option>' + state.book +
        "</option><option>けんたのレシピ帳(編集できる)</option><option>実家のレシピ帳(見るだけ)</option></select></label><hr>" +
        '<a href="list.html">レシピ</a><a href="menu.html">今日の献立</a><hr>' +
        '<span class="sheet-item soft t-sm">' + state.user + "</span>" +
        '<a href="book.html">レシピ帳の設定</a><a href="account.html">アカウント</a><a href="list-anonymous.html">ログアウト</a>';
    } else {
      right = '<a href="login.html" class="link-quiet">ログイン</a><a href="register.html" class="btn btn--primary btn--sm">登録</a>';
      sheet = '<a href="list-anonymous.html">レシピ</a><a href="login.html">今日の献立</a><hr><a href="login.html">ログイン</a><a href="register.html">登録</a>';
    }
    return (
      '<div class="mock-note">見た目の試作です。ボタンを押しても保存されません。 <a href="../design/mockups/" target="_top">試作の一覧へ</a></div>' +
      '<header class="header"><div class="wrap"><div class="header__bar">' +
      '<a class="header__name" href="' + (state.auth === "in" ? "list.html" : "list-anonymous.html") + '">おうちレシピ帖</a>' +
      '<nav class="header__nav" aria-label="画面">' + nav + "</nav>" +
      '<div class="header__right">' + right + "</div>" +
      '<button class="btn btn--quiet btn--sm header__menu-btn" type="button" data-toggle="sheet" aria-expanded="false">メニュー</button>' +
      '</div><div class="header__sheet" id="sheet" hidden>' + sheet + "</div></div></header>"
    );
  }

  /** フッターの HTML を組み立てる */
  function footerHtml() {
    return '<footer class="footer"><div class="wrap">おうちレシピ帖</div></footer>';
  }

  body.insertAdjacentHTML("afterbegin", headerHtml());
  body.insertAdjacentHTML("beforeend", footerHtml());

  // 開閉するもの(狭い画面のメニュー、利用者のメニュー)
  document.addEventListener("click", function (e) {
    var t = e.target.closest("[data-toggle]");
    if (t) {
      var box = document.getElementById(t.dataset.toggle);
      var open = box.hidden;
      box.hidden = !open;
      t.setAttribute("aria-expanded", String(open));
      if (t.dataset.toggle === "sheet") t.textContent = open ? "閉じる" : "メニュー";
      return;
    }
    // ダイアログを開く、閉じる
    var o = e.target.closest("[data-open]");
    if (o) document.getElementById(o.dataset.open).showModal();
    var c = e.target.closest("[data-close]");
    if (c) c.closest("dialog").close();
    // 絞り込みの印。同じ組の中で 1 つだけを選ぶ
    var chip = e.target.closest(".chip");
    if (chip) {
      chip.parentElement.querySelectorAll(".chip").forEach(function (x) {
        x.setAttribute("aria-pressed", String(x === chip));
      });
    }
    // 手順の「済」
    var done = e.target.closest("[data-done]");
    if (done) {
      var on = done.getAttribute("aria-pressed") !== "true";
      done.setAttribute("aria-pressed", String(on));
      done.closest(".step").classList.toggle("is-done", on);
      done.textContent = on ? "✓ 済" : "済";
    }
  });

  // URL に #dialog=<id> が付いていたら、そのダイアログを開いた状態で見せる
  var m = location.hash.match(/dialog=([\w-]+)/);
  if (m && document.getElementById(m[1])) document.getElementById(m[1]).showModal();
})();
