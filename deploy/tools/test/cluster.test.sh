#!/usr/bin/env bash
# クラスタの作成と削除を確かめる。道具のコンテナの中で動く。
# 今あるクラスタを消してから始めるので、中のデータは残らない。
#
# 確かめる受け入れ条件(「共通設計: 実行環境」の 7 章):
#   AC-RT-01 の前半  何もない状態から up を 1 回動かすと、クラスタができる
#   AC-RT-12         down の後に up を動かすと、前のデータが残っておらず、起動に成功する
set -uo pipefail

readonly TOOLS="/work/deploy/tools/bin/tools"
readonly NAMESPACE="recipe-book"
readonly MARKER="cluster-test-marker"

passed=0
failed=0

# 条件を 1 つ確かめる。引数は、名前と、成り立つべきコマンド
check() {
  local name="$1"
  shift
  if "$@" >/dev/null 2>&1; then
    printf 'ok   %s\n' "${name}"
    passed=$((passed + 1))
  else
    printf 'FAIL %s\n' "${name}"
    failed=$((failed + 1))
  fi
}

tools() { bash "${TOOLS}" "$@"; }
node_ready() { [ "$(tools kubectl get nodes -o jsonpath='{.items[0].status.conditions[?(@.type=="Ready")].status}')" = "True" ]; }
node_count_is_one() { [ "$(tools kubectl get nodes -o name | wc -l)" -eq 1 ]; }
namespace_exists() { tools kubectl get namespace "${NAMESPACE}"; }
marker_exists() { tools kubectl get configmap "${MARKER}" -n "${NAMESPACE}"; }
marker_missing() { ! marker_exists; }
cluster_missing() { ! kind get clusters 2>/dev/null | grep -qx recipe-book; }
host_port_is_local_8080() {
  [ "$(docker port recipe-book-control-plane 30080/tcp)" = "127.0.0.1:8080" ]
}

echo "# 何もない状態にする"
tools down

echo "# AC-RT-01 の前半: 何もない状態から up を 1 回動かす"
check "AC-RT-01 前: クラスタがない" cluster_missing
check "AC-RT-01 up が成功する" tools up
check "AC-RT-01 ノードが 1 台である" node_count_is_one
check "AC-RT-01 ノードが Ready である" node_ready
check "AC-RT-01 名前空間 recipe-book がある" namespace_exists
check "AC-RT-01 ホストの 127.0.0.1:8080 が、ノードの 30080 につながっている" host_port_is_local_8080

echo "# up をもう一度動かしても、作り直さない"
tools kubectl create configmap "${MARKER}" -n "${NAMESPACE}" --from-literal=made=before-down >/dev/null
check "up を 2 回動かしても成功する" tools up
check "up を 2 回動かしても、前のデータが残っている" marker_exists

echo "# AC-RT-12: down の後に up を動かす"
check "AC-RT-12 down が成功する" tools down
check "AC-RT-12 down の後、クラスタがない" cluster_missing
check "AC-RT-12 down の後の up が成功する" tools up
check "AC-RT-12 ノードが Ready である" node_ready
check "AC-RT-12 前のデータが残っていない" marker_missing

echo "# 後片付け"
tools down

printf '\n%d 件が通り、%d 件が失敗した\n' "${passed}" "${failed}"
[ "${failed}" -eq 0 ]
