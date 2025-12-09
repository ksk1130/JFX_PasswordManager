package jp.euks.PasswordManager.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import jp.euks.PasswordManager.database.PasswordDatabase;
import jp.euks.PasswordManager.model.PasswordEntry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * パスワードマネージャーのメインUIコントローラー。
 * 
 * <p>
 * このクラスは以下の機能を提供します：
 * </p>
 * <ul>
 * <li>パスワードエントリーの作成、編集、削除</li>
 * <li>URLまたはユーザー名による検索機能</li>
 * <li>パスワードの表示・非表示切り替え（プレス中のみ表示）</li>
 * <li>パスワードのクリップボードコピー</li>
 * <li>Chrome/Edge互換のCSVインポート・エクスポート機能</li>
 * </ul>
 * 
 * @author Password Manager
 * @version 1.0
 */
public class PasswordManagerController {
    private static final Logger logger = LogManager.getLogger(PasswordManagerController.class);
    private BorderPane root;
    private TableView<PasswordEntry> entryTable;
    private ObservableList<PasswordEntry> entryList;
    private PasswordDatabase database;

    // 入力フィールド
    private TextField nameField;
    private TextField urlField;
    private TextField usernameField;
    private TextField passwordField;
    private TextArea notesArea;
    private TextField searchField;

    // 現在選択されているエントリー
    private PasswordEntry selectedEntry;

    /**
     * PasswordManagerControllerのコンストラクタ。
     * データベース接続を初期化し、UIを構築し、エントリーを読み込みます。
     */
    public PasswordManagerController() {
        // UIを先に初期化（エラー表示のため）
        initializeUI();
        
        try {
            database = new PasswordDatabase();
            loadEntries();
        } catch (SQLException e) {
            e.printStackTrace();
            showErrorAlert("データベース初期化エラー", "データベースの初期化に失敗しました: " + e.getMessage());
        }
    }

    /**
     * UIコンポーネントを初期化します。
     * 検索バー、エントリーリスト、詳細パネル、ボタンバーを構築します。
     */
    private void initializeUI() {
        root = new BorderPane();
        root.setPadding(new Insets(10));

        // 上部：検索バー
        HBox searchBox = createSearchBar();
        root.setTop(searchBox);

        // 左側：エントリーリスト
        VBox leftPanel = createEntryList();

        // 右側：詳細/編集パネル
        VBox rightPanel = createDetailPanel();

        // 中央にSplitPaneを配置
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(leftPanel, rightPanel);
        splitPane.setDividerPositions(0.6);

        root.setCenter(splitPane);

        // 下部：ボタン
        HBox buttonBox = createButtonBar();
        root.setBottom(buttonBox);
    }

    /**
     * 検索バーUIを作成します。
     * 
     * @return 検索バーのHBoxコンテナ
     */
    private HBox createSearchBar() {
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(0, 0, 10, 0));

        Label searchLabel = new Label("検索:");
        searchField = new TextField();
        searchField.setPromptText("URLまたはユーザー名で検索...");
        searchField.setPrefWidth(300);

        Button searchButton = new Button("検索");
        Button clearButton = new Button("クリア");

        searchButton.setOnAction(e -> performSearch());
        clearButton.setOnAction(e -> clearSearch());

        searchField.setOnAction(e -> performSearch());

        searchBox.getChildren().addAll(searchLabel, searchField, searchButton, clearButton);
        return searchBox;
    }

    /**
     * エントリーリストUIを作成します。
     * TableViewで名前、URL、ユーザー名、ノート、更新日を表示します。
     * 
     * @return エントリーリストのVBoxコンテナ
     */
    private VBox createEntryList() {
        VBox leftPanel = new VBox(10);
        leftPanel.setPrefWidth(600);

        Label listLabel = new Label("パスワードエントリー");
        listLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // テーブルビューの設定
        entryTable = new TableView<>();
        entryList = FXCollections.observableArrayList();
        entryTable.setItems(entryList);

        TableColumn<PasswordEntry, String> nameColumn = new TableColumn<>("名前");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setPrefWidth(120);

        TableColumn<PasswordEntry, String> urlColumn = new TableColumn<>("URL");
        urlColumn.setCellValueFactory(new PropertyValueFactory<>("url"));
        urlColumn.setPrefWidth(150);

        TableColumn<PasswordEntry, String> usernameColumn = new TableColumn<>("ユーザー名");
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        usernameColumn.setPrefWidth(100);

        TableColumn<PasswordEntry, String> notesColumn = new TableColumn<>("ノート");
        notesColumn.setCellValueFactory(new PropertyValueFactory<>("notes"));
        notesColumn.setPrefWidth(100);

        TableColumn<PasswordEntry, String> updatedColumn = new TableColumn<>("更新日");
        updatedColumn.setCellValueFactory(cellData -> {
            if(Objects.nonNull(cellData.getValue().getUpdatedAt())){
                return new SimpleStringProperty(
                        cellData.getValue().getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")));
            }
            return new SimpleStringProperty("");
        });
        updatedColumn.setPrefWidth(80);

        entryTable.getColumns().add(nameColumn);
        entryTable.getColumns().add(urlColumn);
        entryTable.getColumns().add(usernameColumn);
        entryTable.getColumns().add(notesColumn);
        entryTable.getColumns().add(updatedColumn);

        // 初期ソート順を名前列の昇順に設定
        nameColumn.setSortType(TableColumn.SortType.ASCENDING);
        entryTable.getSortOrder().add(nameColumn);

        // テーブル選択イベント
        entryTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (Objects.nonNull(newSelection)) {
                selectedEntry = newSelection;
                populateFields(newSelection);
            }
        });

        leftPanel.getChildren().addAll(listLabel, entryTable);
        VBox.setVgrow(entryTable, Priority.ALWAYS);

        return leftPanel;
    }

    /**
     * エントリー詳細・編集パネルUIを作成します。
     * 名前、URL、ユーザー名、パスワード、ノートの入力フィールドを含みます。
     * 
     * @return 詳細パネルのVBoxコンテナ
     */
    private VBox createDetailPanel() {
        VBox rightPanel = new VBox(10);
        rightPanel.setPrefWidth(350);

        Label detailLabel = new Label("エントリー詳細");
        detailLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);

        // フォームフィールド
        formGrid.add(new Label("名前:"), 0, 0);
        nameField = new TextField();
        nameField.setPromptText("サイト名（例：Google）");
        formGrid.add(nameField, 1, 0);

        formGrid.add(new Label("URL:"), 0, 1);
        urlField = new TextField();
        urlField.setPromptText("https://example.com");
        formGrid.add(urlField, 1, 1);

        formGrid.add(new Label("ユーザー名:"), 0, 2);
        usernameField = new TextField();
        formGrid.add(usernameField, 1, 2);

        formGrid.add(new Label("パスワード:"), 0, 3);
        passwordField = new PasswordField();
        formGrid.add(passwordField, 1, 3);

        formGrid.add(new Label("ノート:"), 0, 4);
        notesArea = new TextArea();
        notesArea.setPrefRowCount(3);
        formGrid.add(notesArea, 1, 4);

        // パスワードボタン用のHBox
        HBox passwordButtonBox = new HBox(5);

        // パスワード表示ボタン（押している間だけ表示）
        Button showPasswordButton = new Button("表示");
        showPasswordButton.setOnMousePressed(e -> showPassword());
        showPasswordButton.setOnMouseReleased(e -> hidePassword());

        // パスワードコピーボタン
        Button copyPasswordButton = new Button("コピー");
        copyPasswordButton.setOnAction(e -> copyPasswordToClipboard());

        passwordButtonBox.getChildren().addAll(showPasswordButton, copyPasswordButton);
        formGrid.add(passwordButtonBox, 2, 3);

        // カラム設定
        ColumnConstraints col1 = new ColumnConstraints(80);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        ColumnConstraints col3 = new ColumnConstraints(110);
        formGrid.getColumnConstraints().addAll(col1, col2, col3);

        rightPanel.getChildren().addAll(detailLabel, formGrid);
        return rightPanel;
    }

    /**
     * ボタンバーUIを作成します。
     * 新規、保存、削除、クリア、CSVインポート、CSVエクスポートボタンを含みます。
     * 
     * @return ボタンバーのHBoxコンテナ
     */
    private HBox createButtonBar() {
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        Button newButton = new Button("新規");
        Button saveButton = new Button("保存");
        Button deleteButton = new Button("削除");
        Button clearButton = new Button("クリア");
        Button importButton = new Button("CSVインポート");
        Button exportButton = new Button("CSVエクスポート");
        Button removeDuplicatesButton = new Button("重複を削除");

        newButton.setOnAction(e -> newEntry());
        saveButton.setOnAction(e -> saveEntry());
        deleteButton.setOnAction(e -> deleteEntry());
        clearButton.setOnAction(e -> clearFields());
        importButton.setOnAction(e -> importFromCSV());
        exportButton.setOnAction(e -> exportToCSV());
        removeDuplicatesButton.setOnAction(e -> DuplicateRemovalHelper.showRemoveDuplicatesDialog(database, this::loadEntries));

        buttonBox.getChildren().addAll(newButton, saveButton, deleteButton, clearButton, importButton, exportButton, removeDuplicatesButton);
        return buttonBox;
    }

    /**
     * データベースから全てのエントリーを読み込み、TableViewに表示します。
     */
    private void loadEntries() {
        entryList.clear();
        entryList.addAll(database.getAllEntries());
    }

    /**
     * 選択されたエントリーの情報を入力フィールドに表示します。
     * 
     * @param entry 表示するエントリー
     */
    private void populateFields(PasswordEntry entry) {
        nameField.setText(entry.getName());
        urlField.setText(entry.getUrl());
        usernameField.setText(entry.getUsername());
        passwordField.setText(entry.getPassword());
        notesArea.setText(entry.getNotes());
    }

    /**
     * 全ての入力フィールドをクリアし、選択状態を解除します。
     */
    private void clearFields() {
        nameField.clear();
        urlField.clear();
        usernameField.clear();
        passwordField.clear();
        notesArea.clear();
        selectedEntry = null;
        entryTable.getSelectionModel().clearSelection();
    }

    /**
     * 新規エントリー作成モードに切り替えます。
     * 入力フィールドをクリアし、URLフィールドにフォーカスを移動します。
     */
    private void newEntry() {
        clearFields();
        urlField.requestFocus();
    }

    /**
     * 現在編集中のエントリーを保存します。
     * 新規エントリーの場合はinsert、既存の場合はupdateします。
     */
    private void saveEntry() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            showWarningAlert("入力エラー", "URLを入力してください。");
            return;
        }

            if (Objects.isNull(selectedEntry)) {
                // 新規エントリー
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    name = url; // nameが空の場合はURLを使用
                }
                PasswordEntry newEntry = new PasswordEntry(
                        name,
                        url,
                        usernameField.getText().trim(),
                        passwordField.getText(),
                        notesArea.getText().trim());
                logger.debug("Creating new entry: {}", url);
                database.saveEntry(newEntry);
                logger.debug("Entry saved with ID: {}", newEntry.getId());

                // データベースから再読み込み
                loadEntries();
            } else {
                // 既存エントリーの更新
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    name = url; // nameが空の場合はURLを使用
                }
                selectedEntry.setName(name);
                selectedEntry.setUrl(url);
                selectedEntry.setUsername(usernameField.getText().trim());
                selectedEntry.setPassword(passwordField.getText());
                selectedEntry.setNotes(notesArea.getText().trim());
                logger.debug("Updating entry ID: {}", selectedEntry.getId());
                database.updateEntry(selectedEntry);

                // データベースから再読み込み
                loadEntries();
            }

            showInfoAlert("保存完了", "エントリーが保存されました。");
            clearFields();
    }

    /**
     * 選択されたエントリーを削除します。
     * 削除前に確認ダイアログを表示します。
     */
    private void deleteEntry() {
        if (Objects.isNull(selectedEntry)) {
            showWarningAlert("選択エラー", "削除するエントリーを選択してください。");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("削除確認");
        confirmAlert.setHeaderText("エントリーを削除しますか？");
        confirmAlert.setContentText("この操作は元に戻せません。");

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            database.deleteEntry(selectedEntry.getId());
            entryList.remove(selectedEntry);
            clearFields();
            showInfoAlert("削除完了", "エントリーが削除されました。");
        }
    }

    /**
     * 検索キーワードに基づいてエントリーを検索します。
     * URLまたはユーザー名がキーワードを含むエントリーを表示します。
     */
    private void performSearch() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            loadEntries();
            return;
        }

        entryList.clear();
        entryList.addAll(database.searchEntries(searchTerm));
    }

    /**
     * 検索をクリアし、全てのエントリーを再表示します。
     */
    private void clearSearch() {
        searchField.clear();
        loadEntries();
    }

    private TextField tempPasswordField = null;

    /**
     * パスワードを可視化します。
     * PasswordFieldを一時的にTextFieldに置き換えます（ボタンプレス中のみ）。
     */
    private void showPassword() {
        if (passwordField instanceof PasswordField && passwordField.getParent() instanceof GridPane) {
            String password = passwordField.getText();
            GridPane parent = (GridPane) passwordField.getParent();
            int rowIndex = GridPane.getRowIndex(passwordField);
            int colIndex = GridPane.getColumnIndex(passwordField);

            // TextFieldを作成
            tempPasswordField = new TextField(password);
            tempPasswordField.setEditable(false);
            tempPasswordField.setPrefWidth(passwordField.getPrefWidth());
            tempPasswordField.setStyle("-fx-background-color: #ffffcc;");

            // 一時的に置き換え
            parent.getChildren().remove(passwordField);
            parent.add(tempPasswordField, colIndex, rowIndex);
        }
    }

    /**
     * パスワードを再度非表示にします。
     * TextFieldをオリジナルのPasswordFieldに戻します。
     */
    private void hidePassword() {
        if (Objects.nonNull(tempPasswordField) && tempPasswordField.getParent() instanceof GridPane) {
            GridPane parent = (GridPane) tempPasswordField.getParent();
            int rowIndex = GridPane.getRowIndex(tempPasswordField);
            int colIndex = GridPane.getColumnIndex(tempPasswordField);

            // 元のPasswordFieldに戻す
            parent.getChildren().remove(tempPasswordField);
            parent.add(passwordField, colIndex, rowIndex);
            tempPasswordField = null;
        }
    }

    /**
     * パスワードをシステムクリップボードにコピーします。
     */
    private void copyPasswordToClipboard() {
        String password = passwordField.getText();
        if (Objects.nonNull(password) && !password.isEmpty()) {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(password);
            clipboard.setContent(content);
            showInfoAlert("コピー完了", "パスワードをクリップボードにコピーしました。");
        } else {
            showWarningAlert("エラー", "コピーするパスワードがありません。");
        }
    }

    /**
     * 情報アラートダイアログを表示します。
     * 
     * @param title   ダイアログのタイトル
     * @param message 表示するメッセージ
     */
    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 警告アラートダイアログを表示します。
     * 
     * @param title   ダイアログのタイトル
     * @param message 表示するメッセージ
     */
    private void showWarningAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * エラーアラートダイアログを表示します。
     * 
     * @param title   ダイアログのタイトル
     * @param message 表示するエラーメッセージ
     */
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Chrome/Edge互換のCSVファイルからパスワードエントリーをインポートします。
     * CSV形式: name,url,username,password,note
     */
    private void importFromCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("EdgeのCSVファイルを選択");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSVファイル", "*.csv"));

        File file = fileChooser.showOpenDialog(root.getScene().getWindow());
        if (Objects.isNull(file)) {
            return;
        }

        try {
            List<PasswordEntry> importedEntries = parseEdgeCSV(file);

            if (importedEntries.isEmpty()) {
                showWarningAlert("インポートエラー", "インポート可能なエントリーが見つかりませんでした。");
                return;
            }

            // 確認ダイアログ
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("インポート確認");
            confirmAlert.setHeaderText("%d件のエントリーが見つかりました".formatted(importedEntries.size()));
            confirmAlert.setContentText("これらのエントリーをインポートしますか？");

            if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                int successCount = 0;
                int errorCount = 0;

                for (PasswordEntry entry : importedEntries) {
                    database.saveEntry(entry);
                    successCount++;
                }

                loadEntries();
                showInfoAlert("インポート完了",
                    "%d件のエントリーをインポートしました。%s".formatted(
                        successCount,
                        errorCount > 0 ? "\n%d件のエラーがありました。".formatted(errorCount) : ""));
            }

        } catch (Exception e) {
            showErrorAlert("インポートエラー", "CSVファイルの読み込みに失敗しました: %s".formatted(e.getMessage()));
            e.printStackTrace();
        }
    }

    /**
     * Edge/ChromeのCSVファイルをパースしてPasswordEntryリストを生成します。
     * 
     * @param file パースするCSVファイル
     * @return パースされたPasswordEntryのリスト
     * @throws Exception CSVファイルの読み込みに失敗した場合
     */
    private List<PasswordEntry> parseEdgeCSV(File file) throws Exception {
        List<PasswordEntry> entries = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new FileReader(file, java.nio.charset.StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // ヘッダー行をスキップ

            if (Objects.nonNull(line)) {
                logger.debug("CSV Header: {}", line);
            }

            while (Objects.nonNull(line = reader.readLine())) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    PasswordEntry entry = parseCSVLine(line);
                    if (Objects.nonNull(entry)) {
                        entries.add(entry);
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing line: " + line);
                    System.err.println("Error: " + e.getMessage());
                }
            }
        }

        return entries;
    }

    /**
     * CSVの1行をパースしてPasswordEntryを生成します。
     * ダブルクォートで囲まれたフィールドとエスケープされたクォートを正しく処理します。
     * 
     * @param line パースするCSV行
     * @return パースされたPasswordEntry（無効な場合はnull）
     */
    private PasswordEntry parseCSVLine(String line) {
        // EdgeのCSV形式: name,url,username,password
        // カンマで区切るが、ダブルクォートで囲まれた部分は1つのフィールドとして扱う
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // エスケープされたダブルクォート
                    field.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(field.toString());
                field = new StringBuilder();
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString()); // 最後のフィールドを追加

        // フィールド数チェック
        if (fields.size() < 4) {
            System.err.println("Invalid CSV line (less than 4 fields): " + line);
            return null;
        }

        String name = fields.get(0).trim();
        String url = fields.get(1).trim();
        String username = fields.get(2).trim();
        String password = fields.get(3).trim();
        String note = fields.size() > 4 ? fields.get(4).trim() : "";

        // URLが空の場合はスキップ
        if (url.isEmpty()) {
            return null;
        }

        // nameが空の場合はURLを使用
        if (name.isEmpty()) {
            name = url;
        }

        PasswordEntry entry = new PasswordEntry(name, url, username, password, note);
        return entry;
    }

    /**
     * 全てのパスワードエントリーをChrome/Edge互換のCSV形式でエクスポートします。
     * CSV形式: name,url,username,password,note
     */
    private void exportToCSV() {
        if (entryList.isEmpty()) {
            showWarningAlert("エクスポートエラー", "エクスポートするエントリーがありません。");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("CSVファイルを保存");
        fileChooser.setInitialFileName("passwords.csv");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSVファイル", "*.csv"));

        File file = fileChooser.showSaveDialog(root.getScene().getWindow());
        if (Objects.isNull(file)) {
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
            // ヘッダー行を書き込み（Chrome/Edge形式）
            writer.write("name,url,username,password,note");
            writer.newLine();

            // 各エントリーを書き込み
            for (PasswordEntry entry : entryList) {
                String name = escapeCSVField(entry.getName());
                String url = escapeCSVField(entry.getUrl());
                String username = escapeCSVField(entry.getUsername());
                String password = escapeCSVField(entry.getPassword());
                String note = escapeCSVField(entry.getNotes());

                writer.write("%s,%s,%s,%s,%s".formatted(name, url, username, password, note));
                writer.newLine();
            }

            showInfoAlert("エクスポート完了",
                    "%d件のエントリーをエクスポートしました。\nファイル: %s".formatted(entryList.size(), file.getName()));

        } catch (Exception e) {
            showErrorAlert("エクスポートエラー", "CSVファイルの書き込みに失敗しました: %s".formatted(e.getMessage()));
            e.printStackTrace();
        }
    }

    /**
     * CSVフィールドをエスケープします。
     * カンマ、ダブルクォート、改行を含む場合はダブルクォートで囲みます。
     * 
     * @param field エスケープするフィールド
     * @return エスケープされたフィールド
     */
    private String escapeCSVField(String field) {
        if (Objects.isNull(field)) {
            return "";
        }

        // カンマ、ダブルクォート、改行が含まれる場合はダブルクォートで囲む
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            // ダブルクォートをエスケープ
            field = field.replace("\"", "\"\"");
            return "\"%s\"".formatted(field);
        }

        return field;
    }

    /**
     * UIのルートBorderPaneを取得します。
     * 
     * @return ルートBorderPane
     */
    public BorderPane getRoot() {
        return root;
    }

    /**
     * リソースをクリーンアップします。
     * データベース接続を閉じ、アプリケーション終了時に呼び出されます。
     */
    public void cleanup() {
        if (Objects.nonNull(database)) {
            database.close();
        }
    }
}