package jp.euks.PasswordManager;

/**
 * JavaFXアプリ起動用のラッパークラス。
 *
 * <p>Appクラス（Application継承）を直接起動すると、
 * 一部の配布形態でランタイム判定により起動失敗するため、
 * 通常のmainクラス経由で起動します。</p>
 */
public final class Launcher {
    private Launcher() {
        // インスタンス化禁止
    }

    /**
     * アプリケーションのエントリーポイント。
     *
     * @param args コマンドライン引数
     */
    public static void main(String[] args) {
        App.main(args);
    }
}
