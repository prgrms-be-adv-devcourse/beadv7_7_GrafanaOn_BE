package shop.deal.member.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.deal.common.response.ApiResponse;
import shop.deal.member.application.MemberService;
import shop.deal.member.presentation.dto.MemberResponse;
import shop.deal.member.presentation.dto.RegisterRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/signup")
    public ApiResponse<MemberResponse> register(@RequestBody final RegisterRequest request){
        return ApiResponse.successWithData(MemberResponse.from(memberService.register(request.toCommand())));
    }
}
