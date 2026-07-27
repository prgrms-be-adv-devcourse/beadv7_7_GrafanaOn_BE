package shop.dear.identity.member.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import shop.dear.common.auth.AuthUser;
import shop.dear.common.response.ApiResponse;
import shop.dear.identity.member.application.MemberService;
import shop.dear.identity.member.presentation.dto.response.MemberResponse;
import shop.dear.identity.member.presentation.dto.request.RegisterSellerRequest;
import shop.dear.identity.member.presentation.dto.response.SellerAccountResponse;
import shop.dear.identity.member.presentation.dto.request.UpdateProfileRequest;
import shop.dear.identity.member.presentation.dto.request.UpdateSellerAccountRequest;

import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    @PatchMapping("/profile/me")
    public ApiResponse<MemberResponse> updateProfile(
        @Valid @RequestBody final UpdateProfileRequest request,
        @AuthUser Long memberId
    ){

        MemberResponse member = MemberResponse.from(memberService.updateProfile(request.toCommand(), memberId), true);

        return ApiResponse.successWithData(member);
    }

    @GetMapping("/profile")
    public ApiResponse<MemberResponse> getProfile(
        @AuthUser Long requesterId,
        @RequestParam(value = "memberId", required = false) final Long memberId
    ){

        Long searchId = Objects.requireNonNullElse(memberId, requesterId);
        boolean isOwner = searchId.equals(requesterId);

        MemberResponse member = MemberResponse.from(memberService.getProfile(searchId), isOwner);

        return ApiResponse.successWithData(member);
    }

    @PostMapping("/me/seller")
    public ApiResponse<Void> registerSeller(
        @Valid @RequestBody final RegisterSellerRequest request,
        @AuthUser Long memberId
    ){

        memberService.registerSeller(memberId, request.toCommand());

        return ApiResponse.success();
    }

    @PatchMapping("/me/seller")
    public ApiResponse<Void> updateSellerAccount(
        @Valid @RequestBody final UpdateSellerAccountRequest request,
        @AuthUser Long memberId
    ){

        memberService.updateSellerAccount(memberId, request.toCommand());

        return ApiResponse.success();
    }

    @DeleteMapping("/me/seller")
    public ApiResponse<Void> unRegister(@AuthUser Long memberId){

        memberService.unRegister(memberId);

        return ApiResponse.success();
    }

    @GetMapping("/me/seller")
    public ApiResponse<SellerAccountResponse> getMyAccount(@AuthUser Long memberId){

        SellerAccountResponse account = SellerAccountResponse.from(memberService.getMyAccount(memberId));

        return ApiResponse.successWithData(account);
    }
}
