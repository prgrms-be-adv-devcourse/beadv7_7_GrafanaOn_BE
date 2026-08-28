package shop.dear.identity.auth.authentication.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.dear.common.auth.AuthUser;
import shop.dear.common.response.ApiResponse;
import shop.dear.identity.auth.authentication.application.AuthService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/auth")
public class InternalAuthController {

    private final AuthService authService;

    @PostMapping("/role")
    public ResponseEntity<ApiResponse<Void>> promoteToSeller(@AuthUser final Long memberId) {

        authService.promoteToSeller(memberId);

        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/role")
    public ResponseEntity<ApiResponse<Void>> demoteToBuyer(@AuthUser final Long memberId) {

        authService.demoteToBuyer(memberId);

        return ResponseEntity.ok(ApiResponse.success());
    }
}
