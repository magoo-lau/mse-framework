package vip.mseapp.core.schedule;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 调度管控控制状态（Redis 控制哈希的单任务条目，管理面写入、实例侧监控线程消费）。
 *
 * <p>管控下行快通道的载体：平台/standalone 管理面在启停/编辑落库后，把本条目以 JSON
 * 写入 Redis 哈希 {@link #CONTROL_HASH_KEY}（field=taskKey）；各服务实例的调度监控线程
 * （独立守护线程，不属于调度中心任务清单）周期 HGETALL 差量应用 pause/resume 与调度覆盖。
 * 哈希缺失/字段缺失 = 无指令、不动作（缺失绝不被解释为 enabled=true）。
 *
 * <p>common-core 纯 JDK 实现（无 Jackson 依赖）：JSON 仅含扁平字段，自带最小转义与
 * 容错解析（非法返回 null，调用方按"无指令"跳过）。ver 为写入方毫秒时间戳，仅作观测，
 * 语义以字段值为准。
 *
 * @param taskKey      任务编号（@MseScheduled.taskId）
 * @param enabled      启停目标态（true 恢复 / false 暂停）
 * @param exprOverride 调度覆盖（null = 恢复代码默认；字段缺失 = 无指令）
 * @param ver          写入时间戳（毫秒）
 */
public record ScheduleControlState(String taskKey, boolean enabled, String exprOverride, long ver) {

    /** 控制哈希键（全局键，非租户域）：field=taskKey，value=本条目 JSON */
    public static final String CONTROL_HASH_KEY = "mse:schedule:control";

    /**
     * 触发踢键（全局键，非租户域）：管理面「立即触发」落库 trigger_pending 后写入毫秒时间戳，
     * 实例侧控制节拍读到新值即发起一次全量 sync 抢占 pending（无 TTL——实例重启首拍读到
     * 旧值也能接管停机期间遗留的 pending）。
     */
    public static final String KICK_KEY = "mse:schedule:kick";

    /**
     * 租户并发闸门参数键（全局键，非租户域）：管理面「调度器」设置页保存后写入
     * {@code key=limit} 行格式（分号分隔，如 {@code data=2;<key>=<limit>}）；
     * 实例侧控制节拍读到后按 {@code TenantTaskGates.key()} 差量应用，5s 级收敛。
     * 与控制哈希同生命周期：平台侧去抖重建一并重写（对账自愈），键缺失 = 无指令不动作。
     */
    public static final String GATES_KEY = "mse:schedule:gates";

    /** 闸门参数编码：key=limit 行格式（分号分隔；键为空/非法字符由调用方规避——约定 [-\w.]+） */
    public static String encodeGates(Map<String, Integer> limits) {
        if (limits == null || limits.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(48);
        limits.forEach((k, v) -> {
            if (k != null && !k.isBlank() && v != null) {
                if (sb.length() > 0) {
                    sb.append(';');
                }
                sb.append(k.trim()).append('=').append(Math.max(1, v));
            }
        });
        return sb.toString();
    }

    /** 闸门参数容错解析：格式非法返回空 Map（调用方按"无指令"跳过，绝不抛出） */
    public static Map<String, Integer> decodeGates(String raw) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String entry : raw.split(";")) {
            int eq = entry.indexOf('=');
            if (eq <= 0 || eq == entry.length() - 1) {
                continue;
            }
            String key = entry.substring(0, eq).trim();
            try {
                out.put(key, Math.max(1, Integer.parseInt(entry.substring(eq + 1).trim())));
            } catch (NumberFormatException ignored) {
                // 非法行跳过
            }
        }
        return out;
    }

    /** 序列化为紧凑 JSON（exprOverride 为 null 时输出 null 字面量，字段恒在） */
    public static String encode(ScheduleControlState state) {
        StringBuilder sb = new StringBuilder(96);
        sb.append("{\"taskKey\":\"").append(escape(state.taskKey())).append("\",\"enabled\":")
                .append(state.enabled())
                .append(",\"expr\":").append(state.exprOverride() == null ? "null"
                        : "\"" + escape(state.exprOverride()) + "\"")
                .append(",\"ver\":").append(state.ver()).append('}');
        return sb.toString();
    }

    /** 容错解析：格式非法或缺 taskKey 返回 null（调用方按"无指令"跳过，绝不抛出） */
    public static ScheduleControlState decode(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> fields = new Parser(json).parseObject();
            if (!(fields.get("taskKey") instanceof String taskKey) || taskKey.isBlank()) {
                return null;
            }
            Object expr = fields.get("expr");
            Object ver = fields.get("ver");
            return new ScheduleControlState(taskKey, !"false".equals(String.valueOf(fields.get("enabled"))),
                    expr == null ? null : String.valueOf(expr),
                    ver instanceof Number number ? number.longValue() : 0L);
        } catch (Exception e) {
            return null;
        }
    }

    private static String escape(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /** 极简 JSON 扁平对象解析器（仅支持 string/bool/number/null 值，够本条目用） */
    private static final class Parser {
        private final String s;
        private int i;

        Parser(String s) {
            this.s = s;
        }

        Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> out = new LinkedHashMap<>();
            ws();
            if (peek() == '}') {
                i++;
                return out;
            }
            while (true) {
                ws();
                String key = string();
                ws();
                expect(':');
                out.put(key, value());
                ws();
                char c = next();
                if (c == '}') {
                    return out;
                }
                if (c != ',') {
                    throw new IllegalArgumentException("unexpected char at " + (i - 1));
                }
            }
        }

        private Object value() {
            ws();
            if (peek() == '"') {
                return string();
            }
            int start = i;
            while (i < s.length() && ",}:".indexOf(s.charAt(i)) < 0) {
                i++;
            }
            String literal = s.substring(start, i).trim();
            if ("true".equals(literal) || "false".equals(literal)) {
                return Boolean.parseBoolean(literal);
            }
            if ("null".equals(literal)) {
                return null;
            }
            try {
                return Long.parseLong(literal);
            } catch (NumberFormatException e) {
                return literal;
            }
        }

        private String string() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> {
                            sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                        }
                        default -> throw new IllegalArgumentException("bad escape \\" + e);
                    }
                } else {
                    sb.append(c);
                }
            }
        }

        private void ws() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
        }

        private char peek() {
            if (i >= s.length()) {
                throw new IllegalArgumentException("unexpected end");
            }
            return s.charAt(i);
        }

        private char next() {
            char c = peek();
            i++;
            return c;
        }

        private void expect(char c) {
            if (next() != c) {
                throw new IllegalArgumentException("expected " + c + " at " + (i - 1));
            }
        }
    }
}
