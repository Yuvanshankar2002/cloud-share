package com.cloudShare.Service;

import com.cloudShare.Dto.ProfileDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class WebhookService {

    private final ProfileService profileService;
    private final UserCreditService userCreditService;

    public boolean verifyWebhookSignature(String svixId,String svixTimestamp,String svixSignature,String payload){
//        By Time Being It will  be ALways True...
        return true;
    }

    public boolean updateUserEvents(String payload) throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(payload);
        String eventType = rootNode.path("type").asText();
        switch (eventType){
            case "user.created":
                handleUserCreated(rootNode.path("data"));
                break;
            case "user.updated":
                handleUserUpdated(rootNode.path("data"));
                break;
            case "user.deleted":
                handleUserDeleted(rootNode.path("deleted"));
                break;
        }

        return false;


    }

    private void handleUserDeleted(JsonNode data) {
        String clerkId =data.path("id").asText();
        profileService.deleteProfile(clerkId);
    }

    private void handleUserUpdated(JsonNode data) {
        String clerkId = data.path("id").asText();

        String email="";
        JsonNode emailAddresses = data.path("email_addresses");
        if(emailAddresses.isArray() && !emailAddresses.isEmpty()){
            email = emailAddresses.get(0).path("email_address").asText();

        }
        String firstName = data.path("first_name").asText("");
        String lastName = data.path("last_name").asText("");
        String photoUrl = data.path("image_url").asText("");

        ProfileDto updatedProfile = ProfileDto.builder()
                .clerkId(clerkId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .photoUrl(photoUrl)
                .build();

        updatedProfile=profileService.updateProfile(updatedProfile);
        if(updatedProfile == null){
            handleUserCreated(data);
        }

    }

    private void handleUserCreated(JsonNode data) {
        String clerkId = data.path("id").asText();

        String email="";
        JsonNode emailAddresses = data.path("email_addresses");
        if(emailAddresses.isArray() && !emailAddresses.isEmpty()){
            email = emailAddresses.get(0).path("email_address").asText();

        }
        String firstName = data.path("first_name").asText("");
        String lastName = data.path("last_name").asText("");
        String photoUrl = data.path("image_url").asText("");

        ProfileDto newProfile = ProfileDto.builder()
                .clerkId(clerkId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .photoUrl(photoUrl)
                .build();

        profileService.createProfile(newProfile);
        userCreditService.createInitialCredits(clerkId);




    }

}
