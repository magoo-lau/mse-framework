package vip.mseapp.core.schedule;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 调度控制状态 JSON 往返与容错：管理面 encode → 实例监控线程 decode；
 * 非法/残缺输入必须返回 null（调用方按"无指令"跳过，绝不抛出）。
 */
class ScheduleControlStateTest {

    @Test
    void encodeDecodeRoundTrip() {
        ScheduleControlState state = new ScheduleControlState("bridge-base-data-push", false, "0 */5 * * * ?", 1695952896000L);
        ScheduleControlState decoded = ScheduleControlState.decode(ScheduleControlState.encode(state));
        assertEquals(state, decoded);
    }

    @Test
    void nullOverrideSurvivesRoundTrip() {
        // expr=null 字面量必须原样往返（显式"恢复代码默认"指令），不得丢失字段语义
        ScheduleControlState state = new ScheduleControlState("mes-andon-escalate", true, null, 42L);
        ScheduleControlState decoded = ScheduleControlState.decode(ScheduleControlState.encode(state));
        assertEquals(state, decoded);
        assertTrue(decoded.enabled());
        assertNull(decoded.exprOverride());
    }

    @Test
    void specialCharsInKeysSurviveRoundTrip() {
        ScheduleControlState state = new ScheduleControlState("task\"with\\quote\n\t", true, "0 0 * * * ?", 1L);
        ScheduleControlState decoded = ScheduleControlState.decode(ScheduleControlState.encode(state));
        assertEquals(state, decoded);
    }

    @Test
    void malformedInputReturnsNullNeverThrows() {
        assertNull(ScheduleControlState.decode(null));
        assertNull(ScheduleControlState.decode(""));
        assertNull(ScheduleControlState.decode("   "));
        assertNull(ScheduleControlState.decode("not json"));
        assertNull(ScheduleControlState.decode("{\"taskKey\":\"x\"")); // 截断
        assertNull(ScheduleControlState.decode("{}")); // 缺 taskKey
        assertNull(ScheduleControlState.decode("{\"taskKey\":\"\"}")); // 空 taskKey
        assertNull(ScheduleControlState.decode("[1,2]")); // 非对象
    }
}
