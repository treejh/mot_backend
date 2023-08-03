package com.umc.mot.purchaseMember.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class PurchaseMemberResponseDto {

    @Getter
    @AllArgsConstructor
    public static class Response {
        private int purchaseMemberId;  //판매자회원식별자
        private String name; //회원 이름
        private String imageUrl; //회원이미지
        private String email; //회원이메일
        private String phone; //회원 전화번호
        private String host; //회원역할
        private String token; //회원 토큰
    }

}
