package jp.euks.PasswordManager.database;

import jp.euks.PasswordManager.model.PasswordEntry;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * H2 Databaseを使用したパスワードデータベース管理クラス。
 * 
 * <p>
 * このクラスは以下の機能を提供します：
 * </p>
 * <ul>
 * <li>H2 Databaseへの接続管理</li>
 * <li>パスワードエントリーのCRUD操作（作成、読み取り、更新、削除）</li>
 * <li>AES-128 CBCモードによるパスワードの暗号化・復号化</li>
 * <li>URLまたはユーザー名による検索機能</li>
 * </ul>
 * 
 * <p>
 * パスワードはデータベースに保存される前に暗号化され、
 * 読み取り時に復号化されます。
 * </p>
 * 
 * @author Password Manager
 * @version 1.0
 */
public class PasswordDatabase {
    private static final Logger logger = LogManager.getLogger(PasswordDatabase.class);
    private static final String DATABASE_PATH = "./passwords";
    // 暗号化キー（本番環境では外部設定ファイルから読み込むべき）
    private static final String SECRET_KEY = "MySecretKey12345";
    private final JdbcTemplate jdbcTemplate;
    
    // RowMapper for PasswordEntry
    private final RowMapper<PasswordEntry> rowMapper = (rs, rowNum) -> {
        PasswordEntry entry = new PasswordEntry();
        entry.setId(rs.getLong("id"));
        entry.setName(rs.getString("name"));
        entry.setUrl(rs.getString("url"));
        entry.setUsername(rs.getString("username"));
        entry.setPassword(decryptPassword(rs.getString("password_hash")));
        entry.setNotes(rs.getString("notes"));
        entry.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        entry.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return entry;
    };

    /**
     * PasswordDatabaseのコンストラクタ。
     * H2 Databaseへの接続を初期化し、テーブルを作成します。
     * 
     * @throws SQLException データベース接続またはテーブル作成に失敗した場合
     */
    public PasswordDatabase() throws SQLException {
        this.jdbcTemplate = initializeDatabase();
        createTablesIfNotExists();
    }

    /**
     * データベース接続を初期化します。
     * H2 Databaseに埋め込みモードで接続し、JdbcTemplateを作成します。
     * 
     * @return JdbcTemplateインスタンス
     */
    private JdbcTemplate initializeDatabase() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:%s;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1".formatted(DATABASE_PATH));
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        
        logger.info("H2 Database connected: {}", DATABASE_PATH);
        return new JdbcTemplate(dataSource);
    }

    /**
     * password_entriesテーブルが存在しない場合、作成します。
     */
    private void createTablesIfNotExists() {
        String createTableSQL = """
                CREATE TABLE IF NOT EXISTS password_entries (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(500),
                    url VARCHAR(500),
                    username VARCHAR(255),
                    password_hash VARCHAR(255),
                    notes TEXT,
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP
                )
                """;
        
        jdbcTemplate.execute(createTableSQL);
        logger.info("Table password_entries created or already exists");
    }

    /**
     * パスワードエントリーをデータベースに保存します。
     * パスワードはAES暗号化されて保存されます。
     * 
     * @param entry 保存するパスワードエントリー
     */
    public void saveEntry(PasswordEntry entry) {
        String sql = """
                INSERT INTO password_entries (name, url, username, password_hash, notes, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        logger.debug("Saving entry: {}", entry.getUrl());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, entry.getName());
            ps.setString(2, entry.getUrl());
            ps.setString(3, entry.getUsername());
            ps.setString(4, encryptPassword(entry.getPassword()));
            ps.setString(5, entry.getNotes());
            ps.setTimestamp(6, Timestamp.valueOf(entry.getCreatedAt()));
            ps.setTimestamp(7, Timestamp.valueOf(entry.getUpdatedAt()));
            return ps;
        }, keyHolder);

        if (Objects.nonNull(keyHolder.getKey())) {
            entry.setId(keyHolder.getKey().longValue());
            logger.debug("Generated ID: {}", entry.getId());
        }
    }

    /**
     * データベースに保存されている全てのパスワードエントリーを取得します。
     * パスワードは復号化されて返されます。
     * 
     * @return 全エントリーのリスト（URL順にソート済み）
     */
    public List<PasswordEntry> getAllEntries() {
        String sql = "SELECT * FROM password_entries ORDER BY url";
        logger.debug("Loading all entries...");
        
        List<PasswordEntry> entries = jdbcTemplate.query(sql, rowMapper);
        logger.info("Loaded {} entries", entries.size());
        
        return entries;
    }

    /**
     * 既存のパスワードエントリーを更新します。
     * パスワードは再暗号化されて保存されます。
     * 
     * @param entry 更新するパスワードエントリー（ID必須）
     */
    public void updateEntry(PasswordEntry entry) {
        String sql = """
                UPDATE password_entries
                SET name = ?, url = ?, username = ?, password_hash = ?, notes = ?, updated_at = ?
                WHERE id = ?
                """;

        logger.debug("Updating entry ID: {}", entry.getId());

        int rowsUpdated = jdbcTemplate.update(sql,
            entry.getName(),
            entry.getUrl(),
            entry.getUsername(),
            encryptPassword(entry.getPassword()),
            entry.getNotes(),
            Timestamp.valueOf(LocalDateTime.now()),
            entry.getId()
        );
        
        logger.debug("Rows updated: {}", rowsUpdated);
    }

    /**
     * 指定されたIDのパスワードエントリーを削除します。
     * 
     * @param id 削除するエントリーのID
     */
    public void deleteEntry(Long id) {
        String sql = "DELETE FROM password_entries WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    /**
     * URLまたはユーザー名でパスワードエントリーを検索します。
     * パスワードは復号化されて返されます。
     * 
     * @param searchTerm 検索キーワード（部分一致）
     * @return 検索結果のリスト（URL順にソート済み）
     */
    public List<PasswordEntry> searchEntries(String searchTerm) {
        String sql = """
                SELECT * FROM password_entries
                WHERE url LIKE ? OR username LIKE ?
                ORDER BY url
                """;
        
        String searchPattern = "%%%s%%".formatted(searchTerm);
        return jdbcTemplate.query(sql, rowMapper, searchPattern, searchPattern);
    }

    /**
     * 重複するエントリーを検出します。
     * URL・ユーザー名・パスワードが完全に同じエントリーを重複と判定します。
     * 重複グループごとに、古い順でソート済みのリストを返却します。
     * 
     * @return 重複グループのマップ（キー: "url|||username|||password", 値: エントリーのリスト）
     */
    public Map<String, List<PasswordEntry>> findDuplicates() {
        logger.debug("Finding duplicate entries...");
        List<PasswordEntry> entries = getAllEntries();
        Map<String, List<PasswordEntry>> groups = new LinkedHashMap<>();
        
        for (PasswordEntry entry : entries) {
            // キー: URL|||ユーザー名|||パスワード で重複グループを作成
            String key = "%s|||%s|||%s".formatted(
                Objects.nonNull(entry.getUrl()) ? entry.getUrl() : "",
                Objects.nonNull(entry.getUsername()) ? entry.getUsername() : "",
                Objects.nonNull(entry.getPassword()) ? entry.getPassword() : ""
            );
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
        }
        
        // 2件以上あるグループのみ抽出し、各グループを作成日時でソート（古い順）
        Map<String, List<PasswordEntry>> duplicates = groups.entrySet().stream()
            .filter(e -> e.getValue().size() > 1)
            .peek(e -> e.getValue().sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt())))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                LinkedHashMap::new
            ));
        
        logger.info("Found {} duplicate groups", duplicates.size());
        return duplicates;
    }

    /**
     * 指定されたエントリーIDを複数削除します。
     * 通常は古いエントリーを削除する際に使用します。
     * 
     * @param entryIds 削除するエントリーのID一覧
     * @return 削除されたエントリー数
     */
    public int deleteEntries(List<Long> entryIds) {
        int deletedCount = 0;
        for (Long id : entryIds) {
            deleteEntry(id);
            deletedCount++;
        }
        logger.info("Deleted {} duplicate entries", deletedCount);
        return deletedCount;
    }

    /**
     * パスワードをAES-128 CBCモードで暗号化します。
     * ランダムIVを生成し、暗号化データと結合してBase64エンコードします。
     * 
     * @param password 暗号化する平文パスワード
     * @return Base64エンコードされた暗号化パスワード（IV + 暗号化データ）
     * @throws RuntimeException 暗号化に失敗した場合
     */
    private String encryptPassword(String password) {
        try {
            // 16バイトのキーを生成
            byte[] key = SECRET_KEY.getBytes("UTF-8");
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

            // IVを生成
            SecureRandom random = new SecureRandom();
            byte[] iv = new byte[16];
            random.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // 暗号化
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            byte[] encrypted = cipher.doFinal(password.getBytes("UTF-8"));

            // IV + 暗号化データを結合
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            throw new RuntimeException("パスワード暗号化に失敗しました", e);
        }
    }

    /**
     * 暗号化されたパスワードをAES-128 CBCモードで復号化します。
     * Base64デコード後、IVと暗号化データを分離して復号化します。
     * 
     * @param encryptedPassword Base64エンコードされた暗号化パスワード
     * @return 復号化された平文パスワード（エラー時は空文字列）
     */
    private String decryptPassword(String encryptedPassword) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedPassword);

            // IVを取得
            byte[] iv = new byte[16];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // 暗号化データを取得
            byte[] encrypted = new byte[combined.length - iv.length];
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

            // 復号化
            byte[] key = SECRET_KEY.getBytes("UTF-8");
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, "UTF-8");

        } catch (Exception e) {
            System.err.println("パスワード復号化エラー: " + e.getMessage());
            return "";
        }
    }

    /**
     * データベース接続を閉じてリソースを解放します。
     * Spring JDBCでは接続管理は自動的に行われるため、このメソッドは何もしません。
     */
    public void close() {
        // Spring JDBCは接続を自動的に管理するため、明示的なクローズは不要
        logger.info("Database connection managed by Spring JDBC");
    }
}