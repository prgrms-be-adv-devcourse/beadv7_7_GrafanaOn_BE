package shop.dear.identity.member.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shop.dear.common.auth.AuthUser;
import shop.dear.common.response.ApiResponse;
import shop.dear.identity.member.application.MemberService;
import shop.dear.identity.member.presentation.dto.response.MemberResponse;
import shop.dear.identity.member.presentation.dto.request.RegisterSellerRequest;
import shop.dear.identity.member.presentation.dto.response.MyProfileResponse;
import shop.dear.identity.member.presentation.dto.response.PublicProfileResponse;
import shop.dear.identity.member.presentation.dto.response.SellerAccountResponse;
import shop.dear.identity.member.presentation.dto.request.UpdateProfileRequest;
import shop.dear.identity.member.presentation.dto.request.UpdateSellerAccountRequest;

import java.util.Objects;

import static shop.dear.common.response.ApiResponse.success;
import static shop.dear.common.response.ApiResponse.successWithData;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    @PatchMapping("/profile/me")
    public ResponseEntity<ApiResponse<MemberResponse>> updateProfile(
        @Valid @RequestBody final UpdateProfileRequest request,
        @AuthUser Long memberId
    ){

        MemberResponse member = MemberResponse.from(memberService.updateProfile(request.toCommand(), memberId), true);

        return ResponseEntity.ok(successWithData(member));
    }

    //TODO: 프론트엔드에서 endpint 수정 후 삭제 처리
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<MemberResponse>> getProfile(
        @AuthUser Long requesterId,
        @RequestParam(value = "memberId", required = false) final Long memberId
    ){

        Long searchId = Objects.requireNonNullElse(memberId, requesterId);
        boolean isOwner = searchId.equals(requesterId);

        MemberResponse member = MemberResponse.from(memberService.getProfile(searchId), isOwner);

        return ResponseEntity.ok(successWithData(member));
    }

    @GetMapping("/profile/me")
    public ResponseEntity<ApiResponse<MyProfileResponse>> getMyProfile(@AuthUser Long memberId){

        MyProfileResponse member = MyProfileResponse.from(memberService.getProfile(memberId));

        return ResponseEntity.ok(successWithData(member));
    }

    @GetMapping("/profile/{memberId}")
    public ResponseEntity<ApiResponse<PublicProfileResponse>> getProfile(@PathVariable final Long memberId){

        PublicProfileResponse member = PublicProfileResponse.from(memberService.getProfile(memberId));

        return ResponseEntity.ok(successWithData(member));
    }

    @PostMapping("/me/seller")
    public ResponseEntity<ApiResponse<Void>> registerSeller(
        @Valid @RequestBody final RegisterSellerRequest request,
        @AuthUser Long memberId
    ){

        memberService.registerSeller(memberId, request.toCommand());

        return ResponseEntity.ok(success());
    }

    @PatchMapping("/me/seller")
    public ResponseEntity<ApiResponse<Void>> updateSellerAccount(
        @Valid @RequestBody final UpdateSellerAccountRequest request,
        @AuthUser Long memberId
    ){

        memberService.updateSellerAccount(memberId, request.toCommand());

        return ResponseEntity.ok(success());
    }

    @DeleteMapping("/me/seller")
    public ResponseEntity<ApiResponse<Void>> withdrawSeller(@AuthUser Long memberId){

        memberService.withdrawSeller(memberId);

        return ResponseEntity.ok(success());
    }

    @GetMapping("/me/seller")
    public ResponseEntity<ApiResponse<SellerAccountResponse>> getSellerAccount(@AuthUser Long memberId){

        SellerAccountResponse account = SellerAccountResponse.from(memberService.getSellerAccount(memberId));

        return ResponseEntity.ok(successWithData(account));
    }
}