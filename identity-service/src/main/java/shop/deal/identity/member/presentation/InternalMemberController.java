package shop.deal.identity.member.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.deal.common.response.ApiResponse;
import shop.deal.identity.member.application.MemberService;
import shop.deal.identity.member.presentation.dto.CreateProfileRequest;
import shop.deal.identity.member.presentation.dto.MemberResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/members")
public class InternalMemberController {

    private final MemberService memberService;

    @PostMapping
    public ApiResponse<Long> createProfile(@Valid @RequestBody final CreateProfileRequest request){

        MemberResponse member = MemberResponse.from(memberService.createProfile(request.toCommand()));

        return ApiResponse.successWithData(member.id());
    }

    @GetMapping("/{memberId}")
    public ApiResponse<MemberResponse> getProfile(@PathVariable final Long memberId){

        MemberResponse member = MemberResponse.from(memberService.getProfile(memberId));

        return ApiResponse.successWithData(member);
    }

}
