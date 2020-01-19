package com.imooc.pay.service.impl;

import com.imooc.pay.service.IPayService;
import com.lly835.bestpay.config.WxPayConfig;
import com.lly835.bestpay.enums.BestPayPlatformEnum;
import com.lly835.bestpay.enums.BestPayTypeEnum;
import com.lly835.bestpay.model.PayRequest;
import com.lly835.bestpay.model.PayResponse;
import com.lly835.bestpay.service.BestPayService;
import com.lly835.bestpay.service.impl.BestPayServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class PayServiceImpl implements IPayService {

    @Autowired
    private BestPayService bestPayService;

    @Override
    public PayResponse create(String orderId, BigDecimal amount,BestPayTypeEnum bestPayTypeEnum) {

        if(bestPayTypeEnum != BestPayTypeEnum.WXPAY_NATIVE  && bestPayTypeEnum != BestPayTypeEnum.ALIPAY_PC){
           throw  new RuntimeException("暂不支持的支付类型");
        }
        PayRequest request = new PayRequest();
        request.setOrderName("448279-支付SDK");
        request.setOrderId(orderId);
        request.setOrderAmount(amount.doubleValue());
        request.setPayTypeEnum(bestPayTypeEnum);

        PayResponse response = bestPayService.pay(request);
        log.info("response={}",response);
        return response;
    }

    @Override
    public String asyncNotify(String notifyData) {

        //签名校验
        PayResponse payResponse = bestPayService.asyncNotify(notifyData);
        log.info("payResponse ={}",payResponse);

        if(payResponse.getPayPlatformEnum() == BestPayPlatformEnum.WX){
            //通知微信
            return "<xml>\n" +
                    "\n" +
                    "  <return_code><![CDATA[SUCCESS]]></return_code>\n" +
                    "  <return_msg><![CDATA[OK]]></return_msg>\n" +
                    "</xml>";
        }else if(payResponse.getPayPlatformEnum() == BestPayPlatformEnum.ALIPAY){
            return "success";
        }

        throw new RuntimeException("异步通知错误的支付平台");

    }
}
