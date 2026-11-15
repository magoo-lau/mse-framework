package vip.mseapp.service.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.service.system.entity.SysMicroApp;
import vip.mseapp.service.system.mapper.SysMicroAppMapper;
import vip.mseapp.service.system.service.SysMicroAppService;

import java.util.List;

public class SysMicroAppServiceImpl extends CrudRepository<SysMicroAppMapper, SysMicroApp>
        implements SysMicroAppService {

    @Override
    public List<SysMicroApp> listEnabled() {
        return list(new LambdaQueryWrapper<SysMicroApp>()
                .eq(SysMicroApp::getStatus, 0)
                .orderByAsc(SysMicroApp::getSort)
                .orderByAsc(SysMicroApp::getId));
    }

    @Override
    public void create(SysMicroApp app) {
        validate(app, null);
        app.setId(null);
        // uk_name 唯一键含逻辑删除墓碑行：validate 只过滤存活行，物理清理墓碑后重建才不撞唯一键
        getBaseMapper().deleteTombstoneByName(app.getName());
        save(app);
    }

    @Override
    public void update(SysMicroApp app) {
        if (app.getId() == null || getById(app.getId()) == null) {
            throw new ServiceException(404, "子应用不存在");
        }
        validate(app, app.getId());
        getBaseMapper().deleteTombstoneByName(app.getName());
        updateById(app);
    }

    private void validate(SysMicroApp app, Long selfId) {
        if (app.getName() == null || !app.getName().matches("[a-z][a-z0-9-]{1,29}")) {
            // name 进入路由前缀 /sub/{name}，限定小写字母开头的短横线串
            throw new ServiceException(400, "标识须为小写字母开头的 2-30 位小写字母/数字/短横线");
        }
        // name 唯一（排除自身）；name 进入路由前缀 /sub/{name}，全局不可重复
        Long duplicated = count(new LambdaQueryWrapper<SysMicroApp>()
                .eq(SysMicroApp::getName, app.getName())
                .ne(selfId != null, SysMicroApp::getId, selfId));
        if (duplicated != null && duplicated > 0) {
            throw new ServiceException(400, "标识已存在：/sub/" + app.getName() + " 路由冲突");
        }
        if (app.getTitle() == null || app.getTitle().isBlank()) {
            throw new ServiceException(400, "显示名不能为空");
        }
        if (app.getProdEntry() == null || app.getProdEntry().isBlank()) {
            throw new ServiceException(400, "生产入口不能为空（子应用静态资源部署路径）");
        }
        if (app.getSide() == null || app.getSide() < SysMicroApp.SIDE_TENANT
                || app.getSide() > SysMicroApp.SIDE_BOTH) {
            throw new ServiceException(400, "可见端非法（1 租户端 / 2 平台端 / 3 双端）");
        }
    }
}
