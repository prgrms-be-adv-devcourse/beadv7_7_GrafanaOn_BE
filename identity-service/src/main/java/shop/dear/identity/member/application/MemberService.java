package shop.dear.identity.member.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.common.exception.BusinessException;
import shop.dear.identity.member.application.dto.*;
import shop.dear.identity.member.application.dto.external.ExistsProduct;
import shop.dear.identity.member.domain.exception.MemberErrorCode;
import shop.dear.identity.member.domain.model.AccountInfo;
import shop.dear.identity.member.domain.model.Member;
import shop.dear.identity.member.domain.repository.MemberRepository;
import shop.dear.identity.member.application.port.ProductPort;
import shop.dear.identity.member.application.port.WalletPort;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService  {

    private final MemberRepository memberRepository;
    private final ProductPort productPort;
    private final WalletPort walletPort;

    @Transactional
    public MemberInfo createProfile(final CreateProfileCommand command) {

        String nickname = createDefaultNickname();

        Member member = Member.create(
            command.name(),
            command.defaultShippingAddress(),
            command.phoneNumber(),
            nickname);

        Member savedMember = memberRepository.save(member);

        walletPort.createWallet(savedMember.getId());

        return MemberInfo.from(savedMember);
    }

    public MemberInfo getProfile(final Long memberId){

        return memberRepository.findById(memberId)
            .map(MemberInfo::from)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    public boolean isActiveMember(final Long memberId) {

        Member member = validateMember(memberId);

        return member.isActive();
    }

    @Transactional
    public MemberInfo updateProfile(final UpdateProfileCommand command, final Long memberId){

        Member member = validateMember(memberId);

        boolean nicknameChanged = !member.getNickname().equals(command.nickname());
        if(nicknameChanged && memberRepository.existsByNickname(command.nickname())){
            throw new BusinessException(MemberErrorCode.DUPLICATE_NICKNAME);
        }

        member.updateProfile(
            command.defaultShippingAddress(),
            command.phoneNumber(),
            command.nickname());

        return MemberInfo.from(member);
    }

    @Transactional
    public void registerSeller(final Long memberId, final RegisterSellerCommand command) {

        Member member = validateMember(memberId);

        AccountInfo accountInfo = AccountInfo.of(command.bank(), command.account());

        member.registerSeller(accountInfo);
    }

    public SellerInfo getSellerAccount(final Long memberId) {

        Member member = validateMember(memberId);

        if (!member.isSellerActive()) {
            throw new BusinessException(MemberErrorCode.NOT_SELLER);
        }

        AccountInfo accountInfo = member.getSeller().getAccountInfo();

        return new SellerInfo(accountInfo.getBank(), maskAccount(accountInfo.getAccount().value()));
    }

    public boolean isSeller(final Long memberId) {

        Member member = validateMember(memberId);

        return member.isSellerActive();
    }

    @Transactional
    public void updateSellerAccount(final Long memberId, final UpdateSellerAccountCommand command) {

        Member member = validateMember(memberId);

        AccountInfo accountInfo = AccountInfo.of(command.bank(), command.account());

        member.updateSellerAccount(accountInfo);
    }

    @Transactional
    public void withdrawSeller(final Long memberId) {

        Member member = validateMember(memberId);

        if (!member.isSellerActive()) {
            throw new BusinessException(MemberErrorCode.NOT_SELLER);
        }

        ExistsProduct existsProduct = productPort.existsProduct();

        if (existsProduct.exists()) {
            throw new BusinessException(MemberErrorCode.WITHDRAWAL_FAILED);
        }

        member.withdrawSeller();
    }

    @Transactional
    public void withdrawProfile(final Long memberId) {

        Member member = validateMember(memberId);

        member.anonymizeProfile();
    }

    private String createDefaultNickname() {

        String nickname;

        do {
            int random = ThreadLocalRandom.current().nextInt(1_000_000);
            nickname = "user_" + String.format("%06d", random);
        } while (memberRepository.existsByNickname(nickname));

        return nickname;
    }

    private String maskAccount(final String account) {

        if (account.length() <= 6) {
            return "*".repeat(account.length());
        }

        String prefix = account.substring(0, 3);
        String suffix = account.substring(account.length() - 3);
        int maskedLength = account.length() - 6;

        return prefix + "*".repeat(maskedLength) + suffix;
    }

    private Member validateMember(final Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}