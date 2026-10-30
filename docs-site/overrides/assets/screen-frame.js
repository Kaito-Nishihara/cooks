// 画面一覧のページの、試作の枠を、ページの幅に合わせて縮める。
// 中の画面は幅 1,100px(広い画面の並び)で描き、枠の幅に収まるように全体を縮めて見せる。
// 縮めずに枠の幅で描くと、本文の幅が 820px より狭いときに、スマホの並びになってしまう。
(function () {
  "use strict";

  var BASE_WIDTH = 1100; // 中の画面を描く幅(px)
  var BASE_HEIGHT = 900; // 中の画面を描く高さ(px)

  /** 枠を 1 つ、今の幅に合わせる */
  function fit(frame) {
    var iframe = frame.querySelector("iframe");
    if (!iframe) return;
    var scale = Math.min(1, frame.clientWidth / BASE_WIDTH);
    iframe.style.transform = "scale(" + scale + ")";
    frame.style.height = Math.round(BASE_HEIGHT * scale) + "px";
  }

  /** ページの中の枠を、すべて合わせる */
  function fitAll() {
    document.querySelectorAll(".screen-frame").forEach(fit);
  }

  window.addEventListener("resize", fitAll);
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", fitAll);
  } else {
    fitAll();
  }
})();
