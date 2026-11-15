package vip.mseapp.core.result;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页返回体（与 MP 的 IPage 解耦，供 HTTP/DTO 层使用）。
 *
 * @param <T> 记录类型
 */
public record PageResult<T>(long total, long pageNum, long pageSize, List<T> records) implements Serializable {

    public static <T> PageResult<T> empty(long pageNum, long pageSize) {
        return new PageResult<>(0, pageNum, pageSize, Collections.emptyList());
    }

    public static <T> PageResult<T> of(long total, long pageNum, long pageSize, List<T> records) {
        return new PageResult<>(total, pageNum, pageSize, records == null ? Collections.emptyList() : records);
    }
}
