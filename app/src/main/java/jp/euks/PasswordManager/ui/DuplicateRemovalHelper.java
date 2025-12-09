package jp.euks.PasswordManager.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import jp.euks.PasswordManager.database.PasswordDatabase;
import jp.euks.PasswordManager.model.PasswordEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * パスワードの重複削除機能を提供するヘルパークラス。
 * 重複検出ダイアログ表示と削除処理を管理します。
 */
public class DuplicateRemovalHelper {
    private static final Logger logger = LogManager.getLogger(DuplicateRemovalHelper.class);
    
    /**
     * 重複削除ダイアログを表示します。
     * URL・ユーザー名・パスワードが同じエントリーの重複グループを検出し、
     * ユーザーが削除対象を選択できるようにします。
     * 
     * @param database パスワードデータベース
     * @param onComplete 完了時のコールバック
     */
    public static void showRemoveDuplicatesDialog(PasswordDatabase database, Runnable onComplete) {
        Map<String, List<PasswordEntry>> duplicates = database.findDuplicates();
        
        if (duplicates.isEmpty()) {
            showInfoAlert("重複なし", "重複するエントリーはありません。");
            return;
        }
        
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("重複エントリーの削除");
        dialog.setHeaderText("%d個の重複グループが見つかりました".formatted(duplicates.size()));
        dialog.setResizable(true);
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        
        VBox groupsContainer = new VBox(15);
        groupsContainer.setPadding(new Insets(5));
        
        List<Long> selectedForDeletion = new ArrayList<>();
        
        // 重複グループごとにUIを構築
        int groupIndex = 1;
        for (List<PasswordEntry> group : duplicates.values()) {
            VBox groupBox = createDuplicateGroupBox(group, selectedForDeletion, groupIndex++);
            groupsContainer.getChildren().add(groupBox);
        }
        
        scrollPane.setContent(groupsContainer);
        content.getChildren().add(scrollPane);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(700);
        dialog.getDialogPane().setPrefHeight(500);
        
        ButtonType deleteButtonType = new ButtonType("削除", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("キャンセル", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(deleteButtonType, cancelButtonType);
        
        if (dialog.showAndWait().orElse(cancelButtonType) == deleteButtonType) {
            if (!selectedForDeletion.isEmpty()) {
                database.deleteEntries(selectedForDeletion);
                showInfoAlert("削除完了", "%d件の重複エントリーを削除しました。".formatted(selectedForDeletion.size()));
                logger.info("Removed {} duplicate entries", selectedForDeletion.size());
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        }
    }
    
    /**
     * 重複グループのUIコンポーネントを作成します。
     * 各グループ内で削除対象を選択できるCheckBoxを提供します。
     * 
     * @param group 重複グループのエントリーリスト（古い順でソート済み）
     * @param selectedForDeletion 削除対象ID集合（参照で渡す）
     * @param groupIndex グループ番号
     * @return 重複グループのUIコンテナ
     */
    private static VBox createDuplicateGroupBox(List<PasswordEntry> group, List<Long> selectedForDeletion, int groupIndex) {
        VBox groupBox = new VBox(8);
        groupBox.setStyle("-fx-border-color: #ccc; -fx-border-radius: 5; -fx-padding: 10;");
        
        Label groupLabel = new Label("【重複グループ %d】".formatted(groupIndex));
        groupLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        groupBox.getChildren().add(groupLabel);
        
        for (int i = 0; i < group.size(); i++) {
            PasswordEntry entry = group.get(i);
            boolean isOldest = (i < group.size() - 1); // 最後の1件（最新）以外が削除対象
            
            HBox entryBox = new HBox(10);
            entryBox.setAlignment(Pos.CENTER_LEFT);
            
            CheckBox checkBox = new CheckBox();
            checkBox.setSelected(isOldest); // 古い順でデフォルト選択
            checkBox.setOnAction(e -> {
                if (checkBox.isSelected()) {
                    if (!selectedForDeletion.contains(entry.getId())) {
                        selectedForDeletion.add(entry.getId());
                    }
                } else {
                    selectedForDeletion.remove(entry.getId());
                }
            });
            
            if (isOldest) {
                selectedForDeletion.add(entry.getId());
            }
            
            Label label = new Label("%s / %s (作成: %s)%s".formatted(
                entry.getUrl(),
                entry.getUsername(),
                entry.getCreatedAt(),
                isOldest ? " 【削除対象】" : " 【保持】"
            ));
            label.setStyle(isOldest ? "-fx-text-fill: #999;" : "-fx-text-fill: #000; -fx-font-weight: bold;");
            
            entryBox.getChildren().addAll(checkBox, label);
            groupBox.getChildren().add(entryBox);
        }
        
        return groupBox;
    }
    
    /**
     * 情報ダイアログを表示します。
     */
    private static void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
