package shop.dear.identity.member.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import shop.dear.common.response.ApiResponse;
import shop.dear.identity.member.application.MemberService;
import shop.dear.identity.member.presentation.dto.CreateProfileRequest;
import shop.dear.identity.member.presentation.dto.MemberResponse;
import shop.dear.identity.member.presentation.dto.UpdateProfileRequest;

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

}
