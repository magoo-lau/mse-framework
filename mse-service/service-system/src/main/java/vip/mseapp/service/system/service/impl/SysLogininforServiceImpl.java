package vip.mseapp.service.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.service.system.entity.SysLogininfor;
import vip.mseapp.service.system.mapper.SysLogininforMapper;
import vip.mseapp.service.system.service.SysLogininforService;
import org.springframework.util.StringUtils;

import java.util.List;

public class SysLogininforServiceImpl extends CrudRepository<SysLogininforMapper, SysLogininfor> implements SysLogininforService {

    @Override
    public PageResult<SysLogininfor> pageLogininfor(long pageNum, long pageSize, String username, String ip, Integer status) {
        Page<SysLogininfor> page = page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysLogininfor>()
                        .like(StringUtils.hasText(username), SysLogininfor::getUsername, username)
                        .like(StringUtils.hasText(ip), SysLogininfor::getIp, ip)
                        .eq(status != null, SysLogininfor::getStatus, status)
                        .orderByDesc(SysLogininfor::getId));
        return PageResult.of(page.getTotal(), pageNum, pageSize, page.getRecords());
    }

    @Override
    public void record(SysLogininfor logininfor) {
        // 登录链路（尤其失败场景）可能没有租户上下文，按事件显式租户建立后写入
        TenantContext.runAs(logininfor.getTenantId() == null ? 0L : logininfor.getTenantId(),
                () -> save(logininfor));
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
