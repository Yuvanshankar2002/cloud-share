package com.cloudShare.Service;

import com.cloudShare.Dto.PaymentDto;
import com.cloudShare.Dto.PaymentVerficationDto;
import com.cloudShare.Entity.PaymentTransaction;
import com.cloudShare.Entity.ProfileDocument;
import com.cloudShare.Entity.UserCredits;
import com.cloudShare.Exception.CloudShareException;
import com.cloudShare.Exception.ErrorCode;
import com.cloudShare.Repository.PaymentTransactionRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Formatter;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final ProfileService profileService;
    private final UserCreditService userCreditService;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;
    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    public PaymentDto createOrder(PaymentDto paymentDto) {

        try {
            ProfileDocument currentProfile = profileService.getCurrentProfile();
            String clerkId = currentProfile.getClerkId();
            UserCredits userCredit =userCreditService.getUserCredits(clerkId);

            if(userCredit.getPlan().equalsIgnoreCase(
                    "ULTIMATE") || paymentDto.getPlanId().equalsIgnoreCase(userCredit.getPlan())){
                throw new CloudShareException(ErrorCode.PLAN_ALREADY_EXISTS);

            }

            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject orderRequest = new JSONObject();

            orderRequest.put("amount", paymentDto.getAmount());
            orderRequest.put("currency", paymentDto.getCurrency());
            orderRequest.put("receipt", "order_" + System.currentTimeMillis());

            Order order = razorpayClient.orders.create(orderRequest);
            String orderId = order.get("id");

            PaymentTransaction transaction = PaymentTransaction.builder()
                    .clerkId(clerkId)
                    .orderId(orderId)
                    .planId(paymentDto.getPlanId())
                    .amount(paymentDto.getAmount())
                    .currency(paymentDto.getCurrency())
                    .status("PENDING")
                    .transactionDate(LocalDateTime.now())
                    .transactionDate(LocalDateTime.now())
                    .userEmail(currentProfile.getEmail())
                    .userName(currentProfile.getFirstName() + " " + currentProfile.getLastname())
                    .build();

            paymentTransactionRepository.save(transaction);

            return PaymentDto.builder()
                    .orderId(orderId)
                    .success(true)
                    .message("Order Created SuccessFully")
                    .build();

        } catch (CloudShareException ex) {  // child Exception
            throw ex;
        } catch (Exception ex) {
            throw new CloudShareException(ErrorCode.PAYMENT_FAILED, ex.getMessage());

        }


    }

    public PaymentDto verifyPayment(PaymentVerficationDto request) {
        try {
            ProfileDocument currentProfile = profileService.getCurrentProfile();
            String clerkId = currentProfile.getClerkId();
            String data = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
            String generatedSignature = generateHmacSha256Signature(data, razorpayKeySecret);
            if (!generatedSignature.equals(request.getRazorpaySignature())) {
                updateTransactionStatus(request.getRazorpayOrderId(), "FAILED", request.getRazorpayPaymentId(), null);

                return PaymentDto.builder()
                        .success(false)
                        .message("Payment Signature Verification Failed")
                        .build();
            }

            int creditsToAdd = 0;
            String plan = "BASIC";

            switch (request.getPlanId()) {
                case "premium":
                    creditsToAdd = 500;
                    plan = "premium";
                    break;
                case "ultimate":
                    creditsToAdd = 5000;
                    plan = "ULTIMATE";
                    break;
            }
            if (creditsToAdd > 0) {
                userCreditService.addCredits(clerkId, creditsToAdd, plan);
                updateTransactionStatus(request.getRazorpayOrderId(), "SUCCESS", request.getRazorpayPaymentId(), creditsToAdd);
                return PaymentDto.builder()
                        .success(true)
                        .message("Payments verfied and Credits Added Successfully")
                        .credits(userCreditService.getUserCredits(clerkId).getCredits())
                        .build();

            } else {
                updateTransactionStatus(request.getRazorpayOrderId(), "FAILED", request.getRazorpayPaymentId(), null);
                return PaymentDto.builder()
                        .success(false)
                        .message("Invalid plan selected")
                        .build();

            }

        } catch (Exception e) {
            try {
                updateTransactionStatus(request.getRazorpayOrderId(), "ERROR", request.getRazorpayPaymentId(), null);


            } catch (Exception ex) {
                throw new CloudShareException(ErrorCode.PAYMENT_VERIFICATION_FAILED);

            }
            return PaymentDto.builder().
                    success(false)
                    .message("Error Occured while Verifying the Payment :{}" + e.getMessage())
                    .build();

        }
    }

    private void updateTransactionStatus(String razorpayOrderId, String status, String razorpayPaymentId, Integer creditToAdd) {
        paymentTransactionRepository.findAll().stream()
                .filter(t -> t.getOrderId() != null && t.getOrderId().equals(razorpayOrderId))
                .findFirst()
                .map(transaction -> {
                            transaction.setStatus(status);
                            transaction.setPaymentId(razorpayPaymentId);
                            if (creditToAdd != null) {
                                transaction.setCreditsAdded(creditToAdd);
                            }
                            return paymentTransactionRepository.save(transaction);

                        }

                ).orElse(null);
    }

    /**
     * Generate HMAC SHA256 signature for payment verification
     */
    private String generateHmacSha256Signature(String data, String secret)
            throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKey);

        byte[] hmacData = mac.doFinal(data.getBytes());

        return toHexString(hmacData);
    }

    private String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }


}
