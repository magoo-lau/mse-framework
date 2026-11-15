package vip.mseapp.service.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.service.system.entity.SysOperLog;
import vip.mseapp.service.system.mapper.SysOperLogMapper;
import vip.mseapp.service.system.service.SysOperLogService;
import org.springframework.util.StringUtils;

import java.util.List;

public class SysOperLogServiceImpl extends CrudRepository<SysOperLogMapper, SysOperLog> implements SysOperLogService {

    @Override
    public PageResult<SysOperLog> pageOperLogs(long pageNum, long pageSize, String module, String operUsername,
                                               String operType, Integer status) {
        Page<SysOperLog> page = page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysOperLog>()
                        .like(StringUtils.hasText(module), SysOperLog::getModule, module)
                        .like(StringUtils.hasText(operUsername), SysOperLog::getOperUsername, operUsername)
                        .eq(StringUtils.hasText(operType), SysOperLog::getOperType, operType)
                        .eq(status != null, SysOperLog::getStatus, status)
                        .orderByDesc(SysOperLog::getId));
        return PageResult.of(page.getTotal(), pageNum, pageSize, page.getRecords());
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            removeByIds(ids);
        }
    }

    @Override
    public void clean() {
        remove(new LambdaQueryWrapper<>());
    }
}
