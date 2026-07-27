package shop.dear.identity.member.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import shop.dear.common.auth.AuthUser;
import shop.dear.common.response.ApiResponse;
import shop.dear.identity.member.application.MemberService;
import shop.dear.identity.member.presentation.dto.request.CreateProfileRequest;
import shop.dear.identity.member.presentation.dto.response.CreateProfileResponse;
import shop.dear.identity.member.presentation.dto.response.SellerCheckResponse;
import shop.dear.identity.member.presentation.dto.response.MemberCheckResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/members")
public class InternalMemberController {

    private final MemberService memberService;

    @PostMapping
    public ApiResponse<CreateProfileResponse> createProfile(@Valid @RequestBody final CreateProfileRequest request) {

        CreateProfileResponse response = CreateProfileResponse.from(memberService.createProfile(request.toCommand()));

        return ApiResponse.successWithData(response);
    }

    @GetMapping("/seller")
    public ApiResponse<SellerCheckResponse> isSeller(@AuthUser final Long memberId){

        boolean isSeller = memberService.isSeller(memberId);

        return ApiResponse.successWithData(SellerCheckResponse.from(isSeller));
    }
    @GetMapping
    public ApiResponse<MemberCheckResponse> existsMember(@AuthUser final Long memberId) {

        boolean exists = memberService.existsMember(memberId);

        return ApiResponse.successWithData(MemberCheckResponse.from(exists));
    }

}
