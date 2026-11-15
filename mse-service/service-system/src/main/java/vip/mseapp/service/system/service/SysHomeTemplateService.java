package vip.mseapp.service.system.service;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import vip.mseapp.service.system.entity.SysHomeTemplate;

/**
 * 门户首页模板（两端物理分库、相互独立：平台库份=平台模板 / 租户库份=本租户模板；按当前登录端定位读写范围）。
 */
public interface SysHomeTemplateService extends IRepository<SysHomeTemplate> {

    /**
     * 管理端读取：返回本端模板记录（平台=平台库 tenant_id=0 行，租户=本租户行），
     * 无记录时返回空底稿（前端以内置默认布局填充编辑器），保存即成为本端专属配置。
     */
    TemplateEditVO getForEdit();

    /** 管理端保存：按登录端 upsert 本端记录（首存即建） */
    void save(String name, String layoutJson, Integer status);

    /** 管理端恢复默认：删除本端记录，回退前端内置默认首页 */
    void reset();

    /** 使用端解析本端生效模板：本端启用记录 → null（前端回退内置默认） */
    SysHomeTemplate resolveEffective();

    /** 管理端编辑视图：template 为待编辑内容（本端记录或空底稿）。 */
    record TemplateEditVO(SysHomeTemplate template) {
    }
}
