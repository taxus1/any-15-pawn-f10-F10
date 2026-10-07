package com.somepro.interfaces.rest.pawner.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 修改当户入参（用户接口层）。
 *
 * 字段按全量传；这里只改档案信息（姓名/身份证/电话/地址），不含状态 ——
 * 冻结/解冻走专门的 /freeze、/unfreeze 用例（幂等且并发安全），注销走 /close。
 */
@Getter
@Setter
public class PawnerUpdateRequest {

    private Long id;

    private String name;

    private String idCard;

    private String phone;

    private String address;
}
