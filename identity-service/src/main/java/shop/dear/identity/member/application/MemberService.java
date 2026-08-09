package shop.dear.identity.member.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.dear.common.exception.BusinessException;
import shop.dear.identity.member.application.dto.CreateProfileCommand;
import shop.dear.identity.member.application.dto.MemberInfo;
import shop.dear.identity.member.application.dto.RegisterSellerCommand;
import shop.dear.identity.member.application.dto.SellerInfo;
import shop.dear.identity.member.application.dto.UpdateProfileCommand;
import shop.dear.identity.member.application.dto.UpdateSellerAccountCommand;
import shop.dear.identity.member.application.dto.external.ExistsProduct;
import shop.dear.identity.member.domain.exception.MemberErrorCode;
import shop.dear.identity.member.domain.model.Member;
import shop.dear.identity.member.domain.model.Seller;
import shop.dear.identity.member.domain.repository.MemberRepository;
import shop.dear.identity.member.application.port.ProductPort;
import shop.dear.identity.member.application.port.WalletPort;
import shop.dear.identity.member.domain.repository.SellerRepository;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService  {

    private final MemberRepository memberRepository;
    private final SellerRepository sellerRepository;
    private final ProductPort productPort;
    private final Encryptor encryptor;
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

    public boolean existsMember(final Long memberId) {

        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

        return member.isActive();
    }

    @Transactional
    public MemberInfo updateProfile(final UpdateProfileCommand command, final Long memberId){

        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

        boolean nicknameChanged = !member.getNickname().equals(command.nickname());
        if(nicknameChanged && memberRepository.existsByNickname(command.nickname())){
            throw new BusinessException(MemberErrorCode.DUPLICATE_NICKNAME);
        }

        member.changeProfile(
            command.defaultShippingAddress(),
            command.phoneNumber(),
            command.nickname());

        return MemberInfo.from(member);
    }

    @Transactional
    public void registerSeller(final Long memberId, final RegisterSellerCommand command) {

        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

        Seller seller = sellerRepository.findByMemberId(memberId)
            .orElse(null);

        if (seller == null) {
            member.registerSeller(command.bank(), encryptor.encode(command.account()));
        } else {
            seller.reRegister(command.bank(), encryptor.encode(command.account()));
        }
    }

    public SellerInfo getMyAccount(final Long memberId) {

        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

        if (!member.isSeller()) {
            throw new BusinessException(MemberErrorCode.NOT_SELLER);
        }

        Seller seller = member.getSeller();
        String decodedAccount = encryptor.decode(seller.getAccount());

        return new SellerInfo(seller.getBank(), maskAccount(decodedAccount));
    }

    public boolean isSeller(final Long memberId) {

        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

        return member.isSeller();
    }

    @Transactional
    public void updateSellerAccount(final Long memberId, final UpdateSellerAccountCommand command) {

        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

        if (!member.isSeller()) {
            throw new BusinessException(MemberErrorCode.NOT_SELLER);
        }

        member.updateSellerAccount(command.bank(), encryptor.encode(command.account()));
    }

    @Transactional
    public void unRegister(final Long memberId) {

        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

        if (!member.isSeller()) {
            throw new BusinessException(MemberErrorCode.NOT_SELLER);
        }

        ExistsProduct existsProduct = productPort.existsProduct();

        if (existsProduct.exists()) {
            throw new BusinessException(MemberErrorCode.WITHDRAWAL_FAILED);
        }

        member.requestSellerWithdrawal();
    }

    @Transactional
    public void withdrawProfile(final Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(
                        MemberErrorCode.MEMBER_NOT_FOUND
                ));

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
}
