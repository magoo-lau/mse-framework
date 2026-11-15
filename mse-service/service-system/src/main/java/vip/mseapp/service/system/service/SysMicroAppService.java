package vip.mseapp.service.system.service;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import vip.mseapp.service.system.entity.SysMicroApp;

import java.util.List;

/**
 * 微前端子应用注册（平台端维护；登录端可见集合由 AccessService 按端与套餐裁剪）。
 */
public interface SysMicroAppService extends IRepository<SysMicroApp> {

    /** 全量启用子应用（sort 升序；/me 下发与套餐圈定选择器的数据源） */
    List<SysMicroApp> listEnabled();

    /** 校验并新增（name 全局唯一；menu_json 结构校验） */
    void create(SysMicroApp app);

    /** 校验并修改 */
    void update(SysMicroApp app);
}
