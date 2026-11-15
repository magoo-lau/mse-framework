package vip.mseapp.service.system.service;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.service.system.entity.SysPost;

import java.util.List;

/**
 * 岗位管理（租户隔离；岗位仅挂职务，不参与鉴权）。
 */
public interface SysPostService extends IRepository<SysPost> {

    PageResult<SysPost> pagePosts(long pageNum, long pageSize, String postCode, String postName, Integer status);

    /** 全部正常岗位（用户新增/编辑的岗位下拉） */
    List<SysPost> listEnabled();

    Long createPost(SysPost post);

    void updatePost(SysPost post);

    /** 有用户挂载的岗位不可删除 */
    void deletePost(Long id);
}
