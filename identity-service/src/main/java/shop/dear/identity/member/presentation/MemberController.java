package shop.dear.identity.member.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import shop.dear.common.response.ApiResponse;
import shop.dear.identity.member.application.MemberService;
import shop.dear.identity.member.presentation.dto.request.SellerCheckRequest;
import shop.dear.identity.member.presentation.dto.response.MemberResponse;
import shop.dear.identity.member.presentation.dto.request.RegisterSellerRequest;
import shop.dear.identity.member.presentation.dto.response.SellerAccountResponse;
import shop.dear.identity.member.presentation.dto.response.SellerCheckResponse;
import shop.dear.identity.member.presentation.dto.request.UpdateProfileRequest;
import shop.dear.identity.member.presentation.dto.request.UpdateSellerAccountRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/profile")
    public ApiResponse<MemberResponse> getProfile(@RequestParam(value = "memberId") final Long memberId){

        MemberResponse member = MemberResponse.from(memberService.getProfile(memberId));

        return ApiResponse.successWithData(member);
    }

    //ToDo : 추후 jwt토큰 전달 방식에 따라 어노테이션 수정
    @GetMapping("/profile/me")
    public ApiResponse<MemberResponse> getMyProfile(@RequestHeader("X-Member-Id") Long memberId){

        MemberResponse member = MemberResponse.from(memberService.getProfile(memberId));

        return ApiResponse.successWithData(member);
    }

    @PatchMapping("/profile/me")
    public ApiResponse<MemberResponse> updateProfile(
        @Valid @RequestBody final UpdateProfileRequest request,
        @RequestHeader("X-Member-Id") Long memberId
    ){

        MemberResponse member = MemberResponse.from(memberService.updateProfile(request.toCommand(), memberId));

        return ApiResponse.successWithData(member);
    }

    @PostMapping("/me/seller")
    public ApiResponse<Void> registerSeller(
        @Valid @RequestBody final RegisterSellerRequest request,
        @RequestHeader("X-Member-Id") Long memberId
    ){

        memberService.registerSeller(memberId, request.toCommand());

        return ApiResponse.success();
    }

    @PatchMapping("/me/seller")
    public ApiResponse<Void> updateSellerAccount(
        @Valid @RequestBody final UpdateSellerAccountRequest request,
        @RequestHeader("X-Member-Id") Long memberId
    ){

        memberService.updateSellerAccount(memberId, request.toCommand());

        return ApiResponse.success();
    }

    @DeleteMapping("/me/seller")
    public ApiResponse<Void> unRegister(@RequestHeader("X-Member-Id") Long memberId){

        memberService.unRegister(memberId);

        return ApiResponse.success();
    }

    @GetMapping("/me/seller")
    public ApiResponse<SellerAccountResponse> getMyAccount(@RequestHeader("X-Member-Id") Long memberId){

        SellerAccountResponse account = SellerAccountResponse.from(memberService.getMyAccount(memberId));

        return ApiResponse.successWithData(account);
    }

    @GetMapping("/seller")
    public ApiResponse<SellerCheckResponse> isSeller(@RequestBody final SellerCheckRequest request){

        boolean isSeller = memberService.isSeller(request.memberId());

        return ApiResponse.successWithData(SellerCheckResponse.from(isSeller));
    }


}
