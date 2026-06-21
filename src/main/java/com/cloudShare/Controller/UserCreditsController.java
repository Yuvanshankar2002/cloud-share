package com.cloudShare.Controller;

import com.cloudShare.Dto.FileMetaDataDocumentDto;
import com.cloudShare.Dto.UserCreditsDto;
import com.cloudShare.Entity.UserCredits;
import com.cloudShare.Response.ApiResponse;
import com.cloudShare.Service.UserCreditService;
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
public class UserCreditsController {

    private final UserCreditService userCreditService;

    @GetMapping("/get-credits")
    public ResponseEntity<ApiResponse<?>> getFileForCurrentUser() {
        UserCredits userCredits = userCreditService.getUserCredits();
        UserCreditsDto userCreditsDto = UserCreditsDto.builder()
                .credits(userCredits.getCredits())
                .plan(userCredits.getPlan())
                .build();

        return new ResponseEntity<>(ApiResponse.success(userCreditsDto, "Successfully  Credits Fetched"), HttpStatus.OK);


    }

}
