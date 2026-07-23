package shop.dear.identity.member.application;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.dear.common.exception.BusinessException;
import shop.dear.identity.member.application.dto.CreateProfileCommand;
import shop.dear.identity.member.application.dto.RegisterSellerCommand;
import shop.dear.identity.member.application.dto.SellerInfo;
import shop.dear.identity.member.application.dto.UpdateProfileCommand;
import shop.dear.identity.member.application.dto.UpdateSellerAccountCommand;
import shop.dear.identity.member.application.port.ProductPort;
import shop.dear.identity.member.domain.constract.MemberRoll;
import shop.dear.identity.member.domain.constract.SellerStatus;
import shop.dear.identity.member.domain.model.Member;
import shop.dear.identity.member.domain.repository.MemberRepository;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private Encryptor encryptor;

    @Mock
    private ProductPort productPort;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("프로필생성 성공")
    void createProfileTest(){

        CreateProfileCommand command = new CreateProfileCommand(
            "테스트",
            "서울시 강남구",
            "010-1234-5678"
        );

        given(memberRepository
            .save(any()))
            .willAnswer(invocation -> invocation.getArgument(0));

        memberService.createProfile(command);

        verify(memberRepository).save(any());
    }

    @Test
    @DisplayName("프로필 조회")
    void getProfileTest(){

        Member member = Member.create(
            "테스트",
            "서울시 강남구",
            "010-1234-5678",
            "user_000001");

        given(memberRepository
            .findById(any()))
            .willReturn(Optional.of(member));

        memberService.getProfile(1L);

        verify(memberRepository).findById(any());
    }

    @Test
    @DisplayName("프로필 수정")
    void updateProfileTest(){

        Member member = Member.create(
            "테스트",
            "서울시 강남구",
            "010-1234-5678",
            "user_000001");

        UpdateProfileCommand command = new UpdateProfileCommand(
            "부산시 해운대구",
            "010-9999-9999",
            "user_000001");

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        memberService.updateProfile(command, 1L);

        Assertions.assertEquals("테스트", member.getName());
        Assertions.assertEquals("부산시 해운대구", member.getDefaultShippingAddress());
        Assertions.assertEquals("010-9999-9999", member.getPhoneNumber());
        verify(memberRepository, never()).existsByNickname(any());
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("프로필 수정 (닉네임 중복)")
    void updateProfile_duplicateNickname(){

        Member member = Member.create(
            "테스트",
            "서울시 강남구",
            "010-1234-5678",
            "다람쥐");

        UpdateProfileCommand command = new UpdateProfileCommand(
            "서울시 강남구",
            "010-1234-5678",
            "너구리");

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(memberRepository.existsByNickname("너구리")).willReturn(true);

        Assertions.assertThrows(BusinessException.class, () -> memberService.updateProfile(command, 1L));
    }

    @Test
    @DisplayName("판매자 등록 성공")
    void registerSellerTest(){

        Member member = Member.create(
            "테스트",
            "서울시 강남구",
            "010-1234-5678",
            "user_000001");

        RegisterSellerCommand command = new RegisterSellerCommand("국민은행", "123-456-789");

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(encryptor.encode("123-456-789")).willReturn("encrypted-123-456-789");

        memberService.registerSeller(1L, command);

        Assertions.assertEquals(MemberRoll.SELLER, member.getRoll());
        Assertions.assertEquals(SellerStatus.ACTIVE, member.getSeller().getStatus());
        Assertions.assertEquals("encrypted-123-456-789", member.getSeller().getAccount());
    }

    @Test
    @DisplayName("판매자 계좌 수정 성공")
    void updateSellerAccountTest(){

        Member member = Member.create(
            "테스트",
            "서울시 강남구",
            "010-1234-5678",
            "user_000001");
        member.registerSeller("국민은행", "123-456-789");

        UpdateSellerAccountCommand command = new UpdateSellerAccountCommand("신한은행", "987-654-321");

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(encryptor.encode("987-654-321")).willReturn("encrypted-987-654-321");

        memberService.updateSellerAccount(1L, command);

        Assertions.assertEquals("신한은행", member.getSeller().getBank());
        Assertions.assertEquals("encrypted-987-654-321", member.getSeller().getAccount());
    }

    @Test
    @DisplayName("판매자 계좌 수정 실패 (판매자 아님)")
    void updateSellerAccount_notSeller(){

        Member member = Member.create(
            "테스트",
            "서울시 강남구",
            "010-1234-5678",
            "user_000001");

        UpdateSellerAccountCommand command = new UpdateSellerAccountCommand("신한은행", "987-654-321");

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        Assertions.assertThrows(BusinessException.class, () -> memberService.updateSellerAccount(1L, command));
    }

    @Test
    @DisplayName("판매자 계좌 조회 성공")
    void getMyAccountTest(){

        Member member = Member.create(
            "테스트",
            "서울시 강남구",
            "010-1234-5678",
            "user_000001");
        member.registerSeller("국민은행", "encrypted-123-456-789");

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        SellerInfo sellerInfo = memberService.getMyAccount(1L);

        Assertions.assertEquals("국민은행", sellerInfo.bank());
        Assertions.assertEquals("encrypted-123-456-789", sellerInfo.account());
    }

    @Test
    @DisplayName("판매자 계좌 조회 실패 (판매자 아님)")
    void getMyAccount_notSeller(){

        Member member = Member.create(
            "테스트",
            "서울시 강남구",
            "010-1234-5678",
            "user_000001");

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        Assertions.assertThrows(BusinessException.class, () -> memberService.getMyAccount(1L));
    }

    @Test
    @DisplayName("판매자 여부 확인 (판매자)")
    void isSeller_true(){

        Member member = Member.create(
            "테스트",
            "서울시 강남구",
            "010-1234-5678",
            "user_000001");
        member.registerSeller("국민은행", "123-456-789");

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        Assertions.assertTrue(memberService.isSeller(1L));
    }

    @Test
    @DisplayName("판매자 여부 확인 (판매자 아님)")
    void isSeller_false(){

        Member member = Member.create(
            "테스트",
            "서울시 강남구",
            "010-1234-5678",
            "user_000001");

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        Assertions.assertFalse(memberService.isSeller(1L));
    }

    @Test
    @DisplayName("판매자 등록 해지 성공")
    void unRegisterTest(){

        Member member = Member.create(
            "테스트",
            "서울시 강남구",
            "010-1234-5678",
            "user_000001");
        member.registerSeller("국민은행", "123-456-789");

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(productPort.hasProduct()).willReturn(false);

        memberService.unRegister(1L);

        Assertions.assertEquals(MemberRoll.BUYER, member.getRoll());
        Assertions.assertEquals(SellerStatus.WITHDRAWING, member.getSeller().getStatus());
    }

    @Test
    @DisplayName("판매자 등록 해지 실패 (등록된 판매상품 존재)")
    void unRegister_hasProduct(){

        Member member = Member.create(
            "테스트",
            "서울시 강남구",
            "010-1234-5678",
            "user_000001");
        member.registerSeller("국민은행", "123-456-789");

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(productPort.hasProduct()).willReturn(true);

        Assertions.assertThrows(BusinessException.class, () -> memberService.unRegister(1L));
        Assertions.assertEquals(MemberRoll.SELLER, member.getRoll());
        Assertions.assertEquals(SellerStatus.ACTIVE, member.getSeller().getStatus());
    }

    @Test
    @DisplayName("판매자 등록 해지 실패 (판매자 아님)")
    void unRegister_notSeller(){

        Member member = Member.create(
            "테스트",
            "서울시 강남구",
            "010-1234-5678",
            "user_000001");

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        Assertions.assertThrows(BusinessException.class, () -> memberService.unRegister(1L));
    }
}
