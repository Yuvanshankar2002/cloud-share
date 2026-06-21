package com.cloudShare.Controller;

import com.cloudShare.Service.WebhookService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ClerkWebhookController {

    @Value("${clerk.webhook.secret}")
    private String webhookSecret;

    private final WebhookService webhookService;

    @PostMapping("/webhooks/clerk")
    public ResponseEntity<?> handleClerkWebHook(@RequestHeader("svix-id") String svixId,
                                                @RequestHeader("svix-timestamp") String svixTimestamp,
                                                @RequestHeader("svix-signature") String svixSignature,
                                                @RequestBody String payload
                                                ) throws JsonProcessingException {
        try{
            boolean isValid = webhookService.verifyWebhookSignature(svixId,svixTimestamp,svixSignature,payload);
            if(!isValid){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Webhook Signature");

            }
            boolean updateEvents = webhookService.updateUserEvents(payload);

            return ResponseEntity.ok().build();
        }catch (Exception e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage());
        }
    }

}
