package com.ywf.mianshiya.crawler;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.ywf.mianshiya.common.ErrorCode;
import com.ywf.mianshiya.exception.BusinessException;
import com.ywf.mianshiya.manager.CounterManager;
import com.ywf.mianshiya.model.entity.User;
import com.ywf.mianshiya.service.UserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Component
public class CrawlerDetect {
    @Resource
    private CounterManager counterManager;
    @Resource
    private UserService userService;
    // 调用多少次时告警
    @NacosValue(value = "${warn.count:10}", autoRefreshed = true)
    private Integer warnCount;

    @NacosValue(value = "${ban.count:20}", autoRefreshed = true)
    private Integer banCount;

    @NacosValue(value = "${crawler.detect.timeInterval:1}", autoRefreshed = true)
    private Integer timeInterval;

    @NacosValue(value = "${crawler.detect.timeUnit:MINUTES}", autoRefreshed = true)
    private String timeUnit;

    /**
     * 检测爬虫
     *
     * @param loginUserId
     */
    public void crawlerDetect(long loginUserId) {

        // 拼接访问 key
        String key = String.format("user:access:%s", loginUserId);
        // 一分钟内访问次数，180 秒过期
        long count = counterManager.incrAndGetCounter(key, timeInterval, TimeUnit.valueOf(timeUnit), 180);
        // 是否封号
        if (count > banCount) {
            // 踢下线
            StpUtil.kickout(loginUserId);
            // 封号
            User updateUser = new User();
            updateUser.setId(loginUserId);
            updateUser.setUserRole("ban");
            userService.updateById(updateUser);
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "访问太频繁，已被封号");
        }
        // 是否告警
        if (count == warnCount) {
            // 可以改为向管理员发送邮件通知
            throw new BusinessException(110, "警告访问太频繁");
        }
    }

}
