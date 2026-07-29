<div align="center">

![logo.png](docs/image/logo.png)

단순한 가격 경쟁이 아닌, 판매자의 스토리를 통해 상품의 가치를 전달하고, 그 가치를 중심으로 거래가 이루어지는 한정판 거래 플랫폼입니다.
</div>

<hr>

## 목차
- [Stack](#stack)
- [프로젝트 구조](#프로젝트-구조)
- [기능 소개](#기능-소개)
- [팀 소개](#팀-소개)

<hr>

## Stack

**Language**
<br>
![Java](https://img.shields.io/badge/Java21-007396?style=for-the-badge&logo=openjdk&logoColor=white)

**Framework**
<br>
![Spring Boot](https://img.shields.io/badge/Spring_Boot4.1.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Spring Cloud Gateway](https://img.shields.io/badge/Spring_Cloud_Gateway-6DB33F?style=for-the-badge&logo=spring&logoColor=white)

**Build Tool**
<br>
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=Gradle&logoColor=white)

**Database**
<br>
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-336791?style=for-the-badge&logo=postgresql&logoColor=white)

**Auth**
<br>
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)

**Infra**
<br>
![AWS S3](https://img.shields.io/badge/AWS_S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

<hr>

## 프로젝트 구조

현재는 멀티모듈로 구성되어 있으며 4계층 (domain / application / infrastructure / presentation)으로 나누어 작업중입니다.

```
dear
├── common                          # 공통 모듈 (API 응답 포맷, 인증, 예외, 이벤트, 감사 로그)
├── gateway-service                 # API Gateway (Spring Cloud Gateway)
├── identity-service                # 인증 · 회원 서비스
│   └── shop.dear.identity
│       ├── auth
│       │   ├── authentication      # 로그인 / 토큰 발급
│       │   └── authorization       # 인가
│       ├── member                  # 회원
│       └── scrap                   # 스크랩
└── commerce-service                # 상품 · 주문 · 정산 서비스
    └── shop.dear.commerce
        ├── product                 # 상품
        ├── order
        │   ├── offer               # 오퍼(제안)
        │   ├── offersnapshot       # 오퍼 스냅샷
        │   └── purchase            # 구매
        ├── financial
        │   ├── payment             # 결제
        │   ├── settlement          # 정산
        │   ├── settlementpolicy    # 정산 정책
        │   └── wallet              # 예치금
        └── search                  # 검색
```

<hr>

## 기능 소개

<table>
    <tr>
        <th>기능</th>
        <th>설명</th>
    </tr>
    <tr>
        <td>인증 · 인가</td>
        <td>이메일,비밀번호 기반 회원가입, 로그인/로그아웃, 회원 탈퇴, JWT 액세스/리프레시 토큰 발급 및 재발급</td>
    </tr>
    <tr>
        <td>회원</td>
        <td>유저 프로필 관리, 판매자 등록 및 계좌 관리</td>
    </tr>
    <tr>
        <td>스크랩</td>
        <td>상품 스크랩, 스크랩 목록 조회</td>
    </tr>
    <tr>
        <td rowspan="2">상품</td>
        <td>판매 상품 관리, 등록 및 스크랩한 상품 목록 조회, 상품 상세 조회, S3 이미지 업로드</td>
    </tr>
    <tr>
        <td>공개 대기 시스템 구현 (매일 20시, 등록 대기상태인 상품을 일괄로 판매 상태 자동 전환)</td>
    </tr>
    <tr>
        <td>검색</td>
        <td>상품명 · 카테고리 · 스토리 기반 상품 검색, 최신순 · 조회수순 · 가격순 정렬, 상품 변경 이벤트 기반 검색 색인 실시간 동기화</td>
    </tr>
    <tr>
        <td>오퍼</td>
        <td>오퍼 목록 조회, 오퍼 생성, 판매자의 오퍼 수락/거절,  오퍼 스냅샷 생성</td>
    </tr>
    <tr>
        <td>즉시구매</td>
        <td>즉시구매 신청/조회/취소/구매확정, 가격제안(오퍼) 생성 · 목록/상세 조회 · 수락/거절</td>
    </tr>
    <tr>
        <td>정산</td>
        <td>정산 이력 조회, 정산예정대금 조회, 판매대금 정산 스케줄러 구현</td>
    </tr>
    <tr>
        <td>예치금</td>
        <td>예치금 입출금내역 조회, 예치금 충전, 조회, 결제 후 차감 기능 구현</td>
    </tr>
</table>

<hr>

## 팀 소개

백엔드 단기심화 7기 2팀 '그라파나나켜야겠다' BE 레포지토리입니다.

유도훈
아러
아러
아러
