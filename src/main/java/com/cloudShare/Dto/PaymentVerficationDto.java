package com.cloudShare.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentVerficationDto {

    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
    private String planId;

}
