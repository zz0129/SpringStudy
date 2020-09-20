package com.ly.service;

import com.ly.framework.Autowired;
import com.ly.framework.BeanNameAware;
import com.ly.framework.Component;
import com.ly.framework.InitializingBean;

@Component("orderService")
public class OrderService implements BeanNameAware, InitializingBean {

    @Autowired
    private UserService userService;

    private String beanName;

    private String userName;

    public void test() {
        System.out.println(userService);
        System.out.println(beanName);
        System.out.println(userName);
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public void afterPropertiesSet() {
        this.userName = userService.getName();
    }
}
