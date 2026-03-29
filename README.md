# JFX Password Manager

JavaFX + H2 で動作するデスクトップ向けパスワード管理アプリです。
ローカルDBに保存したパスワードを一覧管理し、CSVのインポート/エクスポートや重複削除を行えます。

## 概要

- Java 21 / JavaFX 21 ベースのGUIアプリ
- H2組み込みDBにパスワード情報を保存
- パスワードは AES-128-CBC で暗号化して保存
- CSV（Chrome/Edge互換形式）インポート/エクスポート対応
- 重複エントリー検出と一括削除機能

## 主な機能

- エントリーの作成 / 更新 / 削除
- URL・ユーザー名による検索
- パスワードの一時表示（押下中のみ）
- クリップボードコピー
- 重複グループ単位での削除（最新を保持）

## 技術スタック

- Java 21
- JavaFX (controls, fxml)
- Gradle (Application Plugin)
- H2 Database
- Spring JDBC
- Lombok
- Log4j2

## 構成

```text
JFX_PasswordManager/
├─ app/
│  ├─ build.gradle
│  └─ src/main/
│     ├─ java/jp/euks/PasswordManager/
│     │  ├─ App.java                         # エントリポイント
│     │  ├─ database/PasswordDatabase.java   # DB操作・暗号化処理
│     │  ├─ model/PasswordEntry.java         # エントリーモデル
│     │  └─ ui/
│     │     ├─ PasswordManagerController.java # 画面/UIロジック
│     │     └─ DuplicateRemovalHelper.java    # 重複削除ダイアログ
│     └─ resources/
│        ├─ log4j2.xml
│        └─ styles.css
├─ gradle/
├─ gradlew
└─ settings.gradle
```

## 前提条件

- JDK 21（`jpackage` が利用可能なJDK）
- Windows 10/11（app-image 配布は Windows 向け）

## ビルド方法

プロジェクトルートで実行:

```bash
# Windows (PowerShell / cmd)
.\gradlew.bat build

# macOS / Linux
./gradlew build
```

## 実行方法

```bash
# Windows
.\gradlew.bat :app:run

# macOS / Linux
./gradlew :app:run
```

アプリのメインクラスは `jp.euks.PasswordManager.Launcher` です。

## JavaFX依存の方針（アーキテクチャ別）

- ARM64 環境
	- JavaFX は Maven から取得せず、JDK 同梱の `jmods` を使用します。
	- `JAVA_HOME`（または実行中 Java）の `jmods` に以下が必要です。
		- `javafx.base.jmod`
		- `javafx.graphics.jmod`
		- `javafx.controls.jmod`
		- `javafx.fxml.jmod`
	- 例: `C:/bin/jdk-21`

- ARM64 以外（x64 など）
	- JavaFX は Maven Central から依存取得します。

補足:
- ARM64 で JavaFX 同梱ではない JDK を使用すると、配布タスクでエラーになります。

## テスト実行

```bash
# Windows
.\gradlew.bat test

# macOS / Linux
./gradlew test
```

## 配布パッケージ作成（Windows / app-image 推奨）

インストール不要の app-image を作成し、生成された Exe を直接起動できます。

```bash
.\gradlew.bat :app:createWindowsAppImage
```

生成物:

- `app/build/distribution/PasswordManager/PasswordManager.exe`

起動方法:

- `PasswordManager.exe` をダブルクリック

## データ保存先

- H2 DB: 実行ディレクトリ配下の `./passwords`（`passwords.mv.db` 等）

## CSV形式

インポート/エクスポートで扱うヘッダー:

```text
name,url,username,password,note
```

## 注意事項

- 現状は暗号化キーがコード内固定値です。
- 本番運用では、環境変数や外部設定ファイルでキー管理する構成を推奨します。
