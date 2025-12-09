package jp.euks.PasswordManager.model;

import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * パスワードエントリーのモデルクラス。
 * Chrome/Edgeのパスワード管理と互換性のあるデータ構造を提供します。
 * 
 * <p>このクラスは以下のフィールドを持ちます：</p>
 * <ul>
 *   <li>id: データベース内の一意識別子</li>
 *   <li>name: サイト名（Chrome/Edge互換）</li>
 *   <li>url: WebサイトのURL</li>
 *   <li>username: ログインユーザー名</li>
 *   <li>password: 暗号化されたパスワード</li>
 *   <li>notes: 追加メモ</li>
 *   <li>createdAt: 作成日時</li>
 *   <li>updatedAt: 更新日時</li>
 * </ul>
 * 
 * @author Password Manager
 * @version 1.0
 */
@Data
@NoArgsConstructor
public class PasswordEntry {
    private Long id;
    private String name;
    private String url;
    private String username;
    private String password;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 全フィールドを指定するコンストラクタ（Chrome/Edge互換）。
     * 
     * @param name サイト名
     * @param url WebサイトのURL
     * @param username ログインユーザー名
     * @param password パスワード
     * @param notes 追加メモ
     */
    public PasswordEntry(String name, String url, String username, String password, String notes) {
        this.name = name;
        this.url = url;
        this.username = username;
        this.password = password;
        this.notes = notes;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * レガシー互換コンストラクタ。
     * nameフィールドにURLを自動設定します。
     * 
     * @param url WebサイトのURL（nameとしても使用）
     * @param username ログインユーザー名
     * @param password パスワード
     * @param notes 追加メモ
     */
    public PasswordEntry(String url, String username, String password, String notes) {
        this(url, url, username, password, notes);
    }

    /**
     * エントリーの文字列表現を返します。
     * 
     * @return URLとユーザー名を含む文字列
     */
    @Override
    public String toString() {
        return Objects.nonNull(username) && !username.isEmpty() 
            ? "%s (%s)".formatted(url, username)
            : url;
    }
    
    /**
     * フィールド設定時に更新日時を自動更新するカスタムsetter。
     */
    public void setName(String name) {
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }

    public void setUrl(String url) {
        this.url = url;
        this.updatedAt = LocalDateTime.now();
    }

    public void setUsername(String username) {
        this.username = username;
        this.updatedAt = LocalDateTime.now();
    }

    public void setPassword(String password) {
        this.password = password;
        this.updatedAt = LocalDateTime.now();
    }

    public void setNotes(String notes) {
        this.notes = notes;
        this.updatedAt = LocalDateTime.now();
    }
}