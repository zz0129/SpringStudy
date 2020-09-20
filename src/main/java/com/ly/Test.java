package com.ly;

import com.ly.framework.LyApplicationContext;
import com.ly.service.OrderService;

public class Test {

    public static void main(String[] args) {

        //启动类
        LyApplicationContext lyApplicationContext = new LyApplicationContext(AppConfig.class);
        OrderService orderService = (OrderService) lyApplicationContext.getBean("orderService");
        orderService.test();
    }
}
