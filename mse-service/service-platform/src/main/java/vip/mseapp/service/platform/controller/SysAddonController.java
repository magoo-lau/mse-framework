package vip.mseapp.service.platform.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.core.result.Result;
import vip.mseapp.service.platform.entity.SysAddon;
import vip.mseapp.service.platform.entity.SysAddonItem;
import vip.mseapp.service.platform.entity.SysEntitlementItem;
import vip.mseapp.service.platform.mapper.SysAddonItemMapper;
import vip.mseapp.service.platform.mapper.SysAddonMapper;
import vip.mseapp.service.platform.mapper.SysEntitlementItemMapper;
import vip.mseapp.service.platform.service.EntitlementService;
import vip.mseapp.web.log.OperLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 附加项管理（0.1.0 权益模型，平台端）：附加项定义 + 其授权的权益项清单；
 * 租户经「租户权益覆盖」页或开通流程订购。变更即失效权益缓存（全局）+ FEATURE_CHANGED 事件。
 */
@Validated
@RestController
@RequestMapping("/tenant/addon")
public class SysAddonController {

    private final SysAddonMapper addonMapper;

    private final SysAddonItemMapper addonItemMapper;

    private final SysEntitlementItemMapper itemMapper;

    private final EntitlementService entitlementService;

    public SysAddonController(SysAddonMapper addonMapper, SysAddonItemMapper addonItemMapper,
                              SysEntitlementItemMapper itemMapper, EntitlementService entitlementService) {
        this.addonMapper = addonMapper;
        this.addonItemMapper = addonItemMapper;
        this.itemMapper = itemMapper;
        this.entitlementService = entitlementService;
    }

    /** 附加项清单（含授权项明细；附带权益项全集供下拉） */
    @SaCheckPermission(value = "tenant:addon:list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/list")
    public Result<Map<String, Object>> list() {
        List<SysAddon> addons = addonMapper.selectList(
                new LambdaQueryWrapper<SysAddon>().orderByAsc(SysAddon::getId));
        Map<Long, List<SysAddonItem>> itemsByAddon = addonItemMapper.selectList(
                        new LambdaQueryWrapper<>()).stream()
                .collect(Collectors.groupingBy(SysAddonItem::getAddonId));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysAddon addon : addons) {
            rows.add(Map.of("addon", addon, "items", itemsByAddon.getOrDefault(addon.getId(), List.of())));
        }
        return Result.ok(Map.of("rows", rows, "entitlementItems",
                itemMapper.selectList(new LambdaQueryWrapper<SysEntitlementItem>()
                        .eq(SysEntitlementItem::getStatus, 0).orderByAsc(SysEntitlementItem::getId))));
    }

    @OperLog(module = "tenant:addon", type = "新增")
    @SaCheckPermission(value = "tenant:addon:create", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> create(@RequestBody @Validated AddonSaveRequest request) {
        SysAddon addon = request.toEntity();
        checkCodeUnique(addon.getCode(), null);
        addon.setId(null);
        // uk_addon_code 唯一键含逻辑删除墓碑：物理清理后重建才不撞唯一键
        addonMapper.deleteTombstoneByCode(addon.getCode());
        addonMapper.insert(addon);
        // 事务内保存明细：权益项校验失败连同主行一起回滚（无事务曾致主行落库、明细缺失）
        saveItems(addon.getId(), request.items());
        entitlementService.evictGlobal();
        return Result.ok(addon.getId());
    }

    @OperLog(module = "tenant:addon", type = "修改")
    @SaCheckPermission(value = "tenant:addon:update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PutMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(@RequestBody @Validated AddonSaveRequest request) {
        SysAddon addon = request.toEntity();
        if (addon.getId() == null || addonMapper.selectById(addon.getId()) == null) {
            return Result.fail(GlobalErrorCode.NOT_FOUND);
        }
        checkCodeUnique(addon.getCode(), addon.getId());
        addonMapper.updateById(addon);
        // uk_addon_item 唯一键含逻辑删除墓碑：整体替换须物理删旧明细，否则同项重插撞唯一键
        addonItemMapper.deletePhysicalByAddonId(addon.getId());
        saveItems(addon.getId(), request.items());
        entitlementService.evictGlobal();
        return Result.ok();
    }

    @OperLog(module = "tenant:addon", type = "删除")
    @SaCheckPermission(value = "tenant:addon:delete", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addonMapper.deleteById(id);
        addonItemMapper.deletePhysicalByAddonId(id);
        entitlementService.evictGlobal();
        return Result.ok();
    }

    private void saveItems(Long addonId, List<ItemEntry> items) {
        if (items == null) {
            return;
        }
        for (ItemEntry entry : items) {
            if (entry.itemId() == null || itemMapper.selectById(entry.itemId()) == null) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "权益项不存在：" + entry.itemId());
            }
            SysAddonItem item = new SysAddonItem();
            item.setAddonId(addonId);
            item.setItemId(entry.itemId());
            item.setItemValue(entry.itemValue());
            addonItemMapper.insert(item);
        }
    }

    private void checkCodeUnique(String code, Long excludeId) {
        Long count = addonMapper.selectCount(new LambdaQueryWrapper<SysAddon>()
                .eq(SysAddon::getCode, code).ne(excludeId != null, SysAddon::getId, excludeId));
        if (count != null && count > 0) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "附加项编码已存在：" + code);
        }
    }

    public record ItemEntry(@NotNull Long itemId, String itemValue) {
    }

    public record AddonSaveRequest(
            Long id,
            @NotBlank(message = "编码不能为空") String code,
            @NotBlank(message = "名称不能为空") String name,
            String remark,
            Integer status,
            List<ItemEntry> items) {

        SysAddon toEntity() {
            SysAddon addon = new SysAddon();
            addon.setId(id);
            addon.setCode(code == null ? null : code.trim());
            addon.setName(name);
            addon.setRemark(remark);
            addon.setStatus(status == null ? 0 : status);
            return addon;
        }
    }
}
