# Docker Compose 構成定義および起動仕様書

本書は、自動車保険見積システムのコンテナ実行環境（Docker / Docker Compose）の構成定義、設計方針、および起動手順をまとめた交付文書（提出物）です。

---

## 1. システムコンテナ構成図 (Topology)

本システムは、ブラウザからのリクエストを受け付けるフロントエンド（Nginx）を起点とし、同一コンテナネットワーク内でバックエンドおよびデータベースがセキュアに連携する3層アーキテクチャ（3-Tier Architecture）を採用しています。

```mermaid
graph TD
    User([ブラウザ / 利用者]) -- HTTP (Port 80) --> FE[frontendコンテナ: Nginx]
    FE -- SPA静的配信 --> User
    FE -- リバースプロキシ /api/* --> BE[backendコンテナ: Spring Boot]
    BE -- データ永続化 (Port 5432) --> DB[(dbコンテナ: PostgreSQL 16)]
    
    subgraph Container Network (quote-network)
        FE
        BE
        DB
    end
    
    classDef container fill:#e1f5fe,stroke:#03a9f4,stroke-width:2px;
    class FE,BE,DB container;
```

---

## 2. 構成ファイル一覧 (Component Files)

本環境は、以下の9つの定義ファイルにより構成されています。各ファイルは役割が明確に分離され、保守性の高い設計となっています。

| # | ファイル名 / リンク | 役割・設計上の意図 |
|---|---|---|
| 1 | [docker-compose.yml](file:///c:/Users/26329/Desktop/TaSProject/docker/docker-compose.yml) | コンテナ全体（db, backend, frontend）のビルド・連携定義、ポートマッピング、環境変数定義。 |
| 2 | [backend/Dockerfile](file:///c:/Users/26329/Desktop/TaSProject/backend/Dockerfile) | Javaバックエンドのビルド環境（Maven）と実行環境（JRE）を分離したマルチステージビルド定義。 |
| 3 | [frontend/Dockerfile](file:///c:/Users/26329/Desktop/TaSProject/frontend/Dockerfile) | Reactフロントエンドのビルド（Node.js）と配信（Nginx）を分離したマルチステージビルド定義。 |
| 4 | [frontend/nginx.conf](file:///c:/Users/26329/Desktop/TaSProject/frontend/nginx.conf) | SPAのルーティング用フォールバック設定、およびCORSを回避するためのAPIリバースプロキシ設定。 |
| 5 | [docker/db/init/01_schema.sql](file:///c:/Users/26329/Desktop/TaSProject/docker/db/init/01_schema.sql) | データベース初期テーブル作成用DDL（冪等性を担保した設計）。 |
| 6 | [docker/db/init/02_data.sql](file:///c:/Users/26329/Desktop/TaSProject/docker/db/init/02_data.sql) | 初期評価用料率マスタ（rate_masters）のデータ挿入用DML。 |
| 7 | `.env.example` | DBの接続情報やJWTシークレット等の秘匿情報を外部管理するためのテンプレート。 |
| 8 | `backend/.dockerignore` | バックエンドビルド時に不要なローカル中間ファイル（target, .idea等）をコンテナ内へコピーするのを防ぐ除外ルール。 |
| 9 | `frontend/.dockerignore` | フロントエンドビルド時に不要な依存フォルダ（node_modules等）を除外するルール。 |

---

## 3. コンテナ化における設計上の工夫点 (Architectural Highlights)

### ① ヘルスチェック（Healthcheck）と起動依存制御
データベース（PostgreSQL）が完全に起動し、クライアントからの接続を受け入れ可能（Healthy）な状態になるまで、バックエンドコンテナの起動を待機させる制御を導入しています。これにより、起動順序のズレによるデータベース接続エラー（Connection Refused）を完全に防止しています。
```yaml
# docker-compose.yml より抜粋
backend:
  depends_on:
    db:
      condition: service_healthy # dbコンテナのHealthcheck通過を条件に起動
```

### ② マルチステージビルド（Multi-stage Build）による軽量化とセキュリティ強化
バックエンド、フロントエンド双方のDockerfileにおいてマルチステージビルドを採用しています。
* **バックエンド**: ビルド時のみJDK/Mavenを使用し、実行ステージには軽量かつ安全な **Eclipse Temurin JRE (Alpine Linuxベース)** のみを使用。
* **フロントエンド**: ビルド時のみNode.jsを使用し、実行ステージには超軽量な **Nginx (Alpine Linuxベース)** のみを使用。
不要なビルドツールやソースコードを実行環境に内包しないことで、イメージサイズを削減し、本番環境の脆弱性リスク（CVE）を大幅に低減しています。

### ③ Nginxリバースプロキシによる同一ドメイン運用とCORS回避
Nginxにて `/api/` へのアクセスをバックエンドへプロキシ転送する設定を行っています。これにより、フロントエンドとバックエンドのドメイン・ポート（Port 80）がブラウザ側から同一に見えるため、**CORS設定によるトラブルや本番環境でのドメイン間通信の脆弱性を本質的に回避**しています。

### ④ 冪等性（Idempotency）を担保したデータベース初期化
コンテナ起動時に実行される `01_schema.sql` にには `CREATE TABLE IF NOT EXISTS` や `CREATE INDEX IF NOT EXISTS` を定義しています。これにより、コンテナが再起動された場合や永続化データが存在する場合でも、テーブルの二重作成エラーが発生せず、安全にスキップされる堅牢な設計となっています。

---

## 4. 起動手順 (Execution Procedures)

DockerおよびDocker Composeがインストールされた環境において、以下の手順で起動してください。

### ステップ1: 環境変数の準備
リポジトリ直下の環境変数テンプレートをコピーして `.env` ファイルを作成します。
```bash
# Windows PowerShell
copy .env.example .env

# または Linux / macOS / Git Bash
cp .env.example .env
```
※ 必要に応じて、`.env` 内の `JWT_SECRET`（JWT署名キー）などを推測不可能な文字列に書き換えてください。

### ステップ2: コンテナの一括ビルドとバックグラウンド起動
`docker` ディレクトリへ移動し、一括起動コマンドを実行します。
```bash
cd docker
docker compose up --build -d
```
このコマンドにより、各コンテナイメージのビルド、ネットワーク構築、ボリュームの紐付け、およびデータベースのテーブル構築・初期データ投入までが完全に自動連携で実行されます。

### ステップ3: 実行状態の確認
以下のコマンドですべてのサービスが `Up`（または `running`、dbは `healthy`）になっていることを確認します。
```bash
docker compose ps
```

### ステップ4: 停止および完全初期化
* **通常の停止**（コンテナおよびネットワークの削除）:
  ```bash
  docker compose down
  ```
* **完全初期化**（データベースの永続化ボリュームも含めてすべてクリーンにする場合）:
  ```bash
  docker compose down -v
  ```

---

## 5. 動作検証端点 (Endpoints)

コンテナ起動完了後、以下のURLから各機能にアクセスし、動作確認を行うことができます。

| 機能 | 接続先URL | 認証有無 |
|---|---|---|
| **フロントエンド（見積サイト）** | [http://localhost](http://localhost) | 不要 |
| **API仕様書（Swagger UI）** | [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) | 不要 |
| **管理者初期ログインID** | `admin` | - |
| **管理者初期ログインパスワード** | `admin123` | - |

---
以上
