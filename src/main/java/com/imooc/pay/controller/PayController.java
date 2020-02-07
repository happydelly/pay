package com.imooc.pay.controller;

import com.imooc.pay.pojo.PayInfo;
import com.imooc.pay.service.IPayService;
import com.lly835.bestpay.config.WxPayConfig;
import com.lly835.bestpay.enums.BestPayTypeEnum;
import com.lly835.bestpay.model.PayResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/pay")
public class PayController {

    @Autowired
    private IPayService payService;

    @Autowired
    private WxPayConfig wxPayConfig;

    @GetMapping("/create")
    public ModelAndView create(@RequestParam("orderId") String orderId,
                               @RequestParam("amount") BigDecimal amount,
                               @RequestParam("payType") BestPayTypeEnum bestPayTypeEnum){
        PayResponse response = payService.create(orderId, amount, bestPayTypeEnum);
        Map<String,String> map = new HashMap<>();
        if(bestPayTypeEnum == BestPayTypeEnum.WXPAY_NATIVE){
            map.put("codeUrl",response.getCodeUrl());
            map.put("orderId",orderId);
            map.put("returnUrl",wxPayConfig.getReturnUrl());
            return  new ModelAndView("createForWxNative",map);
        }else if(bestPayTypeEnum == BestPayTypeEnum.ALIPAY_PC){
            map.put("body",response.getBody());
            return  new ModelAndView("createForAlipayPc",map);
        }
        throw  new RuntimeException("暂不支持的支付类型 ");
    }

    @PostMapping("/notify")
    @ResponseBody
    public String asyncNotify(@RequestBody String notifyData){
        log.info("notifyData={}",notifyData);
        return payService.asyncNotify(notifyData);
    }

    @GetMapping("queryByOrderId")
    @ResponseBody
    public PayInfo queryByOrderId(@RequestParam("orderId") String orderid){
        log.info("查询支付记录....");
        return payService.queryByOrderId(orderid);
    }

}
