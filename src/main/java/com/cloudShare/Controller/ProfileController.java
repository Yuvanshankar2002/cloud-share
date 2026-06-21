package com.cloudShare.Controller;

import com.cloudShare.Dto.ProfileDto;
import com.cloudShare.Service.ProfileService;
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
public class ProfileController {
    private final ProfileService profileService;

    @PostMapping("/register")
    public ResponseEntity<?> registerProfile(@RequestBody ProfileDto profileDto){
        HttpStatus status = profileService.existsByClerkId(profileDto.getClerkId()) ?HttpStatus.OK:HttpStatus.CREATED;
        ProfileDto savedProfile = profileService.createProfile(profileDto);
        return ResponseEntity.status(status).body(savedProfile);
    }
}
