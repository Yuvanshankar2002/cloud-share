package com.cloudShare.Controller;

import com.cloudShare.Dto.PaymentDto;
import com.cloudShare.Dto.PaymentVerficationDto;
import com.cloudShare.Exception.CloudShareException;
import com.cloudShare.Exception.ErrorCode;
import com.cloudShare.Response.ApiResponse;
import com.cloudShare.Service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody PaymentDto paymentDto){
        PaymentDto response = paymentService.createOrder(paymentDto);
        return new ResponseEntity<>(ApiResponse.success(response, "Order created SuccessFully"), HttpStatus.OK);

    }

    @PostMapping("/verify-payment")
    public ResponseEntity<?> verifyPayment(@RequestBody PaymentVerficationDto request){
        PaymentDto response = paymentService.verifyPayment(request);

        if(!response.getSuccess()){
            throw new CloudShareException(ErrorCode.PAYMENT_VERIFICATION_FAILED);

        }
        return new ResponseEntity<>(ApiResponse.success(response,"Payment Verfication SuccessFully"),HttpStatus.OK);


    }

}
