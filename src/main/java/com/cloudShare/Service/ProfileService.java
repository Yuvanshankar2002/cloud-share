package com.cloudShare.Service;

import com.cloudShare.Dto.ProfileDto;
import com.cloudShare.Entity.ProfileDocument;
import com.cloudShare.Entity.UserCredits;
import com.cloudShare.Repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;

@RequiredArgsConstructor
@Service
public class ProfileService {
    private final ProfileRepository profileRepository;

    public ProfileDto createProfile(ProfileDto profileDto) {
        if (profileRepository.existsByClerkId(profileDto.getClerkId())) {
            return updateProfile(profileDto);
        }
        ProfileDocument profileDocument = ProfileDocument.builder()
                .clerkId(profileDto.getClerkId())
                .firstName(profileDto.getFirstName())
                .email(profileDto.getEmail())
                .lastname(profileDto.getLastName())
                .photoUrl(profileDto.getPhotoUrl())
                .credits(5)
                .createdAt(Instant.now())
                .build();


        profileDocument = profileRepository.save(profileDocument);


        return ProfileDto.builder()
                .id(profileDocument.getId())
                .clerkId(profileDocument.getClerkId())
                .email(profileDocument.getEmail())
                .firstName(profileDocument.getFirstName())
                .lastName(profileDocument.getLastname())
                .photoUrl(profileDocument.getPhotoUrl())
                .credits(profileDocument.getCredits())
                .createdAt(profileDocument.getCreatedAt())
                .build();


    }

    public ProfileDto updateProfile(ProfileDto profileDto) {
        ProfileDocument existingProfile = profileRepository.findByClerkId(profileDto.getClerkId()).orElse(null);

        if (existingProfile != null) {
            if (profileDto.getFirstName() != null && !profileDto.getFirstName().isEmpty()) {
                existingProfile.setFirstName(profileDto.getFirstName());

            }
            if (profileDto.getLastName() != null && !profileDto.getLastName().isEmpty()) {
                existingProfile.setLastname(profileDto.getLastName());

            }
            if (profileDto.getEmail() != null && !profileDto.getEmail().isEmpty()) {
                existingProfile.setEmail(profileDto.getEmail());

            }
            if (profileDto.getPhotoUrl() != null && !profileDto.getPhotoUrl().isEmpty()) {
                existingProfile.setPhotoUrl(profileDto.getPhotoUrl());

            }
            profileRepository.save(existingProfile);
            return ProfileDto.builder()
                    .id(existingProfile.getId())
                    .email(existingProfile.getEmail())
                    .clerkId(existingProfile.getClerkId())
                    .firstName(existingProfile.getFirstName())
                    .lastName(existingProfile.getLastname())
                    .credits(existingProfile.getCredits())
                    .photoUrl(existingProfile.getPhotoUrl())
                    .build();

        }

        return null;

    }

    public boolean existsByClerkId(String clerkId) {
        return profileRepository.existsByClerkId(clerkId);
    }

    public void deleteProfile(String clerkId) {
        ProfileDocument existingProfile = profileRepository.findByClerkId(clerkId).orElse(null);
        if (existingProfile != null) {
            profileRepository.delete(existingProfile);
        }
    }

    public ProfileDocument getCurrentProfile() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            throw new UsernameNotFoundException("User not Authenticated");
        }
        String clerkId = SecurityContextHolder.getContext().getAuthentication().getName();
        return profileRepository.findByClerkId(clerkId).orElse(null);
    }


}
