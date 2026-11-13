package cn.tiangong.framework.security.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 登录用户信息：登录成功后构建，以 JSON 存入 Redis（key 见 TokenService），
 * 请求时由 TokenAuthenticationFilter 还原并放入 SecurityContext。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {

    /** 用户编号 */
    private Long id;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 部门编号 */
    private Long deptId;

    /** 公司编号（M0 恒为 1，多公司拦截器启用后由请求头/切换逻辑决定） */
    private Long companyId;

    /** 权限标识集合，超管为 ["*:*:*"] */
    private Set<String> permissions;
}
