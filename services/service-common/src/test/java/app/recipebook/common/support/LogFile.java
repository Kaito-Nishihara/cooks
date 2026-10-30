package app.recipebook.common.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;
import org.awaitility.Awaitility;

/** テストの間にファイルへ出した JSON のログを読む。 */
public final class LogFile {

    private static final Path PATH = Path.of("target/test-logs/app.json");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** 要求の記録を出すロガーの名前 */
    private static final String ACCESS_LOGGER = "app.recipebook.common.logging.AccessLog";

    private LogFile() {
    }

    /** ファイルの中身を、そのままの文字列で返す。 */
    public static String rawText() {
        try {
            return Files.exists(PATH) ? Files.readString(PATH, StandardCharsets.UTF_8) : "";
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * JSON の行をすべて返す。1 行が 1 件の JSON であることも、ここで確かめる。
     * テストでは、アプリを組み立てる間の行が、ログの設定を読む前の形でファイルに入る。その行は除く。
     */
    public static List<JsonNode> lines() {
        return rawText().lines().filter(line -> line.startsWith("{")).map(LogFile::parse).toList();
    }

    /** 要求の識別子が同じ行を返す。 */
    public static List<JsonNode> linesOf(String requestId) {
        return lines().stream().filter(hasRequestId(requestId)).toList();
    }

    /** 要求の識別子が同じ「要求の記録」を返す。 */
    public static List<JsonNode> accessRecordsOf(String requestId) {
        return linesOf(requestId).stream().filter(LogFile::isAccessRecord).toList();
    }

    /**
     * 要求の記録が出るまで待つ。
     * 記録は応答を返し終えた後に出るので、応答を受け取った直後には、まだないことがある。
     */
    public static void awaitAccessRecord(String requestId, String route) {
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(50))
                .until(() -> accessRecordsOf(requestId).stream()
                        .anyMatch(line -> route.equals(line.path("route").asText(null))));
    }

    private static boolean isAccessRecord(JsonNode line) {
        return ACCESS_LOGGER.equals(line.path("logger").asText());
    }

    private static Predicate<JsonNode> hasRequestId(String requestId) {
        return line -> requestId.equals(line.path("requestId").asText(null));
    }

    private static JsonNode parse(String line) {
        try {
            return MAPPER.readTree(line);
        } catch (IOException e) {
            throw new UncheckedIOException("ログの行が JSON として読めない: " + line, e);
        }
    }
}
