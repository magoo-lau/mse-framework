package vip.mseapp.service.system.service;

import vip.mseapp.core.result.PageResult;
import vip.mseapp.service.system.entity.SysOperLog;
import com.baomidou.mybatisplus.extension.repository.IRepository;

import java.util.List;

/**
 * 操作日志（租户隔离；写入由 OperLogRecorder 异步完成，本接口面向查询与清理）。
 */
public interface SysOperLogService extends IRepository<SysOperLog> {

    PageResult<SysOperLog> pageOperLogs(long pageNum, long pageSize, String module, String operUsername,
                                        String operType, Integer status);

    void deleteByIds(List<Long> ids);

    /** 清空当前租户操作日志 */
    void clean();
}
