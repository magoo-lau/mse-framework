package vip.mseapp.service.system.service;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.service.system.entity.SysLogininfor;

import java.util.List;

/**
 * 登录日志（租户隔离；写入经 RemoteLogService 由认证模块投递，本接口面向查询与清理）。
 */
public interface SysLogininforService extends IRepository<SysLogininfor> {

    PageResult<SysLogininfor> pageLogininfor(long pageNum, long pageSize, String username, String ip, Integer status);

    /** 登录链路写入（显式指定租户，无租户上下文场景安全） */
    void record(SysLogininfor logininfor);

    void deleteByIds(List<Long> ids);

    /** 清空当前租户登录日志 */
    void clean();
}
