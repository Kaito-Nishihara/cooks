# 手元のクラスタを扱う道具のコンテナ。
# kind、kubectl、helm を、ホスト側に入れずに使うためのものである。
# 版はここで固定する(「共通設計: 実行環境」の 4.1)。
FROM alpine:3.22

# 版と、配布元が公開している検査用の値(SHA-256)。
# 版を上げるときは、検査用の値も一緒に替える。
ARG KIND_VERSION=0.33.0
ARG KIND_SHA256=aee6151561422756b764a4ae28e7f44cda5af5a9eead3cc9985112b1de8d8e0d
# kubectl は、クラスタの Kubernetes(deploy/kind/cluster.yaml のノードの版)と同じ版にする
ARG KUBECTL_VERSION=1.34.11
ARG KUBECTL_SHA256=8efbb9435132a190920eb65a47a8c1ecf755ad85ab57a600c9bedbab460bb7a8
ARG HELM_VERSION=4.3.0
ARG HELM_SHA256=86584a54def73570558f66f5111cc53dfed56689637ae32c1201205d494f54fb

RUN apk add --no-cache bash ca-certificates curl docker-cli jq

# 取ってきたものは、検査用の値と合うことを確かめてから置く
RUN set -eux; \
    curl -fsSLo /tmp/kind "https://kind.sigs.k8s.io/dl/v${KIND_VERSION}/kind-linux-amd64"; \
    echo "${KIND_SHA256}  /tmp/kind" | sha256sum -c -; \
    install -m 0755 /tmp/kind /usr/local/bin/kind; \
    curl -fsSLo /tmp/kubectl "https://dl.k8s.io/release/v${KUBECTL_VERSION}/bin/linux/amd64/kubectl"; \
    echo "${KUBECTL_SHA256}  /tmp/kubectl" | sha256sum -c -; \
    install -m 0755 /tmp/kubectl /usr/local/bin/kubectl; \
    curl -fsSLo /tmp/helm.tar.gz "https://get.helm.sh/helm-v${HELM_VERSION}-linux-amd64.tar.gz"; \
    echo "${HELM_SHA256}  /tmp/helm.tar.gz" | sha256sum -c -; \
    tar -xzf /tmp/helm.tar.gz -C /tmp; \
    install -m 0755 /tmp/linux-amd64/helm /usr/local/bin/helm; \
    rm -rf /tmp/kind /tmp/kubectl /tmp/helm.tar.gz /tmp/linux-amd64

# コマンドの本体は、イメージに入れずに、リポジトリを載せて読む。
# スクリプトを直すたびに、イメージを作り直さなくて済むようにするためである。
WORKDIR /work
ENTRYPOINT ["bash", "/work/deploy/tools/bin/tools"]
CMD ["help"]
