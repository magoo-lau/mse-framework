package vip.mseapp.service.system.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 用户导入/导出行（EasyExcel）：导入模板与导出文件同结构。
 *
 * <p>部门按编号（sys_dept.id）填写，避免名称歧义；密码仅导入时可选填写（散列后入库），
 * 导出不含密码列。
 */
@Data
public class UserExcelRow {

    /** 导出用用户编号（导入忽略） */
    @ExcelProperty(value = "用户编号")
    @ColumnWidth(24)
    private Long id;

    @ExcelProperty(value = "登录名称")
    @ColumnWidth(16)
    private String username;

    @ExcelProperty(value = "用户昵称")
    @ColumnWidth(16)
    private String nickname;

    @ExcelProperty(value = "部门编号")
    @ColumnWidth(16)
    private Long deptId;

    @ExcelProperty(value = "手机号码")
    @ColumnWidth(16)
    private String mobile;

    @ExcelProperty(value = "邮箱")
    @ColumnWidth(24)
    private String email;

    /** 0 启用 1 停用（导入默认 0） */
    @ExcelProperty(value = "用户状态")
    @ColumnWidth(10)
    private Integer status;

    /** 仅导入：不填时取参数 sys.user.initPassword */
    @ExcelProperty(value = "初始密码")
    @ColumnWidth(14)
    private String password;
}
