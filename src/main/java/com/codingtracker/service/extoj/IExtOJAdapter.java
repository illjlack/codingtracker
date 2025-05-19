package com.codingtracker.service.extoj;

import com.codingtracker.model.User;
import com.codingtracker.model.UserTryProblem;
import com.codingtracker.model.ExtOjPbInfo;
import com.codingtracker.model.ExtOjLink;
import com.codingtracker.model.OJPlatform;

import java.util.List;

/**
 * 外部 OJ 适配器接口，定义从各 OJ 平台抓取数据的统一方法
 */
public interface IExtOJAdapter {

    /**
     * 在线获取某用户在本 OJ 平台的所有尝试记录（含 AC、WA 等）
     *
     * @param user 用户实体
     * @return 用户尝试记录列表
     */
    List<UserTryProblem> getUserTriesOnline(User user);

    /**
     * 在线获取本 OJ 平台所有题目的统计信息
     *
     * @return 题目统计信息列表
     */
    List<ExtOjPbInfo> getAllPbInfoOnline();

    /**
     * 获取本 OJ 平台的链接配置，包含用户记录和题目统计页链接
     *
     * @return 平台链接对象
     */
    ExtOjLink getOjLink();

    /**
     * 获取本 OJ 平台的枚举类型
     *
     * @return OJ 平台枚举
     */
    OJPlatform getOjType();
}
