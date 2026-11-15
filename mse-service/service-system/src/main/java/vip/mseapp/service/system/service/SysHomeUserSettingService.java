package vip.mseapp.service.system.service;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import vip.mseapp.service.system.entity.SysHomeUserSetting;

/**
 * 用户门户首页个性化设置（在模板可选块范围内挑卡、摆放、选主题的结果）。
 */
public interface SysHomeUserSettingService extends IRepository<SysHomeUserSetting> {

    /** 当前登录者的个性化设置；无记录返回 null（跟随模板默认） */
    SysHomeUserSetting getMySetting();

    /** 保存当前登录者的个性化设置（整体覆盖） */
    void saveMySetting(String layoutJson);

    /** 恢复默认：删除当前登录者的个性化设置 */
    void resetMySetting();
}
