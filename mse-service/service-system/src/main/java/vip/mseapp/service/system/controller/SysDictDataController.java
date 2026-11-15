package vip.mseapp.service.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckOr;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.core.result.Result;
import vip.mseapp.service.system.entity.SysDictData;
import vip.mseapp.service.system.service.SysDictDataService;
import vip.mseapp.web.log.OperLog;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 字典数据管理（管理端）：/system/dict/data/**。
 *
 * <p>按类型取启用数据（/type/{dictType}）是所有业务下拉/标签的公共依赖，
 * 租户端与平台端（双账号体系）登录即可读，维护接口仅租户端开放。
 */
@Validated
@RestController
@RequestMapping("/system/dict/data")
public class SysDictDataController {

    private final SysDictDataService dictDataService;

    public SysDictDataController(SysDictDataService dictDataService) {
        this.dictDataService = dictDataService;
    }

    @SaCheckPermission(value = "system:dict:list", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/page")
    public Result<PageResult<SysDictData>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                @RequestParam String dictType,
                                                @RequestParam(required = false) String dictLabel,
                                                @RequestParam(required = false) Integer status) {
        return Result.ok(dictDataService.pageDictData(pageNum, pageSize, dictType, dictLabel, status));
    }

    /** 双端登录均可读：平台端页面（平台用户/角色/部门等）同样依赖字典渲染 */
    @SaCheckOr(login = {
            @SaCheckLogin(type = MseConstants.LOGIN_TYPE_TENANT),
            @SaCheckLogin(type = MseConstants.LOGIN_TYPE_PLATFORM)
    })
    @GetMapping("/type/{dictType}")
    public Result<List<SysDictData>> listByType(@PathVariable String dictType) {
        return Result.ok(dictDataService.listEnabledByType(dictType));
    }

    @OperLog(module = "system:dict", type = "新增")
    @SaCheckPermission(value = "system:dict:create", type = MseConstants.LOGIN_TYPE_TENANT)
    @PostMapping
    public Result<Long> create(@RequestBody @Validated DictDataSaveRequest request) {
        return Result.ok(dictDataService.createDictData(toEntity(request, new SysDictData())));
    }

    @OperLog(module = "system:dict", type = "修改")
    @SaCheckPermission(value = "system:dict:update", type = MseConstants.LOGIN_TYPE_TENANT)
    @PutMapping
    public Result<Void> update(@RequestBody @Validated DictDataUpdateRequest request) {
        SysDictData dictData = toEntity(request.save(), new SysDictData());
        dictData.setId(request.id());
        dictDataService.updateDictData(dictData);
        return Result.ok();
    }

    @OperLog(module = "system:dict", type = "删除")
    @SaCheckPermission(value = "system:dict:delete", type = MseConstants.LOGIN_TYPE_TENANT)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dictDataService.deleteDictData(id);
        return Result.ok();
    }

    private SysDictData toEntity(DictDataSaveRequest request, SysDictData dictData) {
        dictData.setDictType(request.dictType());
        dictData.setDictLabel(request.dictLabel());
        dictData.setDictValue(request.dictValue());
        dictData.setDictSort(request.dictSort());
        dictData.setListClass(request.listClass());
        dictData.setIsDefault(Boolean.TRUE.equals(request.isDefault()) ? 1 : 0);
        dictData.setStatus(request.status() == null ? 0 : request.status());
        dictData.setRemark(request.remark());
        return dictData;
    }

    // -------------------- 请求体 --------------------

    public record DictDataSaveRequest(
            @NotBlank(message = "字典类型不能为空") String dictType,
            @NotBlank(message = "数据标签不能为空") String dictLabel,
            @NotBlank(message = "数据键值不能为空") String dictValue,
            Integer dictSort,
            String listClass,
            Boolean isDefault,
            Integer status,
            String remark) {
    }

    public record DictDataUpdateRequest(
            @NotNull(message = "数据编号不能为空") Long id,
            @NotBlank(message = "字典类型不能为空") String dictType,
            @NotBlank(message = "数据标签不能为空") String dictLabel,
            @NotBlank(message = "数据键值不能为空") String dictValue,
            Integer dictSort,
            String listClass,
            Boolean isDefault,
            Integer status,
            String remark) {

        DictDataSaveRequest save() {
            return new DictDataSaveRequest(dictType(), dictLabel(), dictValue(), dictSort(),
                    listClass(), isDefault(), status(), remark());
        }
    }
}
