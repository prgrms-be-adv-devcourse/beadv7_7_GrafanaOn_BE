package shop.deal.identity.member.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import shop.deal.common.response.ApiResponse;
import shop.deal.identity.member.application.MemberService;
import shop.deal.identity.member.presentation.dto.CreateProfileRequest;
import shop.deal.identity.member.presentation.dto.MemberResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    //ToDo : 추후 jwt토큰 전달 방식에 따라 어노테이션 수정
    @GetMapping
    public ApiResponse<MemberResponse> getProfile(@RequestParam(required = false,value="memberId") Long memberId,
                                                  @RequestHeader("X-Member-Id") Long loginId){

        Long targetId = (memberId != null) ? memberId : loginId;

        MemberResponse member = MemberResponse.from(memberService.getProfile(targetId));

        return ApiResponse.successWithData(member);
    }
}
