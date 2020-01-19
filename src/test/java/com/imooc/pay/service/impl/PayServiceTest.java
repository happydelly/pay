package com.imooc.pay.service.impl;

import com.imooc.pay.PayApplicationTests;
import com.lly835.bestpay.enums.BestPayTypeEnum;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

public class PayServiceTest extends PayApplicationTests {

    @Autowired
    private PayServiceImpl payService;

    @Test
    public void create(){
        payService.create("12312312312312", BigDecimal.valueOf(0.01), BestPayTypeEnum.WXPAY_NATIVE);
    }

}