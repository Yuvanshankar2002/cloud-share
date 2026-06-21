package com.cloudShare.Service;

import com.cloudShare.Entity.UserCredits;
import com.cloudShare.Repository.UserCreditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCreditService {

    private final UserCreditRepository userCreditRepository;
    private final ProfileService profileService;

    public UserCredits createInitialCredits(String clerkId) {
        UserCredits userCredits = UserCredits.builder()
                .clerkId(clerkId)
                .credits(5)
                .plan("BASIC")
                .build();

        return userCreditRepository.save(userCredits);
    }

    public UserCredits getUserCredits(String clerkId) {
        return userCreditRepository.findByClerkId(clerkId)
                .orElseGet(() -> createInitialCredits(clerkId));

    }

    public UserCredits getUserCredits() {
        String clerkId = profileService.getCurrentProfile().getClerkId();
        return getUserCredits(clerkId);

    }

    public Boolean hasEnoughCredits(int requriedCredits){
        UserCredits userCredits = getUserCredits();
        return userCredits.getCredits()>=requriedCredits;
    }

    public UserCredits consumeCredit(){
        UserCredits userCredits = getUserCredits();
        if(userCredits.getCredits()<=0){
            return null;
        }
        userCredits.setCredits(userCredits.getCredits()-1);
        return userCreditRepository.save(userCredits);
    }

    public UserCredits addCredits(String clerkId,Integer creditsToAdd,String plan){
        UserCredits userCredits = userCreditRepository.findByClerkId(clerkId)
                .orElseGet(()->createInitialCredits(clerkId));

        userCredits.setCredits(userCredits.getCredits()+creditsToAdd);
        userCredits.setPlan(plan);

        return userCreditRepository.save(userCredits);

    }



}
