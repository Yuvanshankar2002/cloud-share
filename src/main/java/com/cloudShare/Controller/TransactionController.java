package com.cloudShare.Controller;

import com.cloudShare.Entity.PaymentTransaction;
import com.cloudShare.Entity.ProfileDocument;
import com.cloudShare.Repository.PaymentTransactionRepository;
import com.cloudShare.Response.ApiResponse;
import com.cloudShare.Service.ProfileService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TransactionController {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ProfileService profileService;

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<PaymentTransaction>>> getUserTransactions(){
        ProfileDocument currentProfile = profileService.getCurrentProfile();
        String clerkId = currentProfile.getClerkId();

        List<PaymentTransaction> transactions = paymentTransactionRepository.findByClerkIdAndStatusOrderByTransactionDateDesc(clerkId,"SUCCESS");

        return new ResponseEntity<>(ApiResponse.success(transactions,"Transactions Fetched SuccessFully"), HttpStatus.OK);
    }

}
