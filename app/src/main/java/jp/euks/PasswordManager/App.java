package jp.euks.PasswordManager;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import jp.euks.PasswordManager.ui.PasswordManagerController;

/**
 * パスワードマネージャーアプリケーションのメインクラス。
 * JavaFXアプリケーションのエントリーポイントとして機能し、
 * Chrome/Edgeと互換性のあるパスワード管理機能を提供します。
 * 
 * @author Password Manager
 * @version 1.0
 */
public class App extends Application {

    /**
     * JavaFXアプリケーションの起動メソッド。
     * メインウィンドウを初期化し、PasswordManagerControllerを配置します。
     * 
     * @param primaryStage アプリケーションのメインステージ
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            PasswordManagerController controller = new PasswordManagerController();
            Scene scene = new Scene(controller.getRoot(), 1280, 800);

            primaryStage.setTitle("パスワードマネージャー");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(500);
            primaryStage.show();

            // アプリケーション終了時にデータベース接続をクリーンアップ
            primaryStage.setOnCloseRequest(e -> controller.cleanup());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * アプリケーションのエントリーポイント。
     * JavaFXアプリケーションを起動します。
     * 
     * @param args コマンドライン引数
     */
    public static void main(String[] args) {
        launch(args);
    }
}