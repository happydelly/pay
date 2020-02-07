package com.imooc.pay.service.impl;

import com.google.gson.Gson;
import com.imooc.pay.dao.PayInfoMapper;
import com.imooc.pay.enums.PayPlatformEnum;
import com.imooc.pay.pojo.PayInfo;
import com.imooc.pay.service.IPayService;
import com.lly835.bestpay.config.WxPayConfig;
import com.lly835.bestpay.enums.BestPayPlatformEnum;
import com.lly835.bestpay.enums.BestPayTypeEnum;
import com.lly835.bestpay.enums.OrderStatusEnum;
import com.lly835.bestpay.model.PayRequest;
import com.lly835.bestpay.model.PayResponse;
import com.lly835.bestpay.service.BestPayService;
import com.lly835.bestpay.service.impl.BestPayServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class PayServiceImpl implements IPayService {

    private final static String QUEUE_PAY_NOTIFY = "payNotify";
    @Autowired
    private BestPayService bestPayService;

    @Autowired
    private PayInfoMapper payInfoMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Override
    public PayResponse create(String orderId, BigDecimal amount,BestPayTypeEnum bestPayTypeEnum) {

        if(bestPayTypeEnum != BestPayTypeEnum.WXPAY_NATIVE  && bestPayTypeEnum != BestPayTypeEnum.ALIPAY_PC){
           throw  new RuntimeException("暂不支持的支付类型");
        }
        //写入数据库
        PayInfo payInfo = new PayInfo(Long.parseLong(orderId),
                PayPlatformEnum.getByBestPayTypeEnum(bestPayTypeEnum).getCode(),
                OrderStatusEnum.NOTPAY.name(),amount);

        payInfoMapper.insertSelective(payInfo);
        PayRequest request = new PayRequest();
        request.setOrderName("448279-支付SDK");
        request.setOrderId(orderId);
        request.setOrderAmount(amount.doubleValue());
        request.setPayTypeEnum(bestPayTypeEnum);

        PayResponse response = bestPayService.pay(request);
        log.info("发起支付 response={}",response);
        return response;
    }

    @Override
    public String asyncNotify(String notifyData) {

        //签名校验
        PayResponse payResponse = bestPayService.asyncNotify(notifyData);
        log.info("异步通知 payResponse ={}",payResponse);

        //2.金额校验
        PayInfo payInfo = payInfoMapper.selectByOrderNo(Long.parseLong(payResponse.getOrderId()));
        if(payInfo == null){
            throw new RuntimeException("通过orderNo查询到的结果是null");
        }

        //如果订单支付状态不是"已支付"
        if(!payInfo.getPlatformStatus().equals(OrderStatusEnum.SUCCESS.name())){
            if(payInfo.getPayAmount().compareTo(BigDecimal.valueOf(payResponse.getOrderAmount())) != 0){
                //告警
                throw new RuntimeException("异步通知中的金额与数据库不一致,orderNo="+payResponse.getOrderId());
            }

            //3.修改订单支付状态
            payInfo.setPlatformStatus(OrderStatusEnum.SUCCESS.name());
            payInfo.setPlatformNumber(payResponse.getOutTradeNo());
            payInfo.setUpdateTime(null);
            payInfoMapper.updateByPrimaryKeySelective(payInfo);
        }

        //发送MQ消息
        amqpTemplate.convertAndSend(QUEUE_PAY_NOTIFY,new Gson().toJson(payInfo));

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

    @Override
    public PayInfo queryByOrderId(String orderid) {

        PayInfo payInfo = payInfoMapper.selectByOrderNo(Long.parseLong(orderid));
        return payInfo;
    }
}
