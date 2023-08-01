package com.umc.mot.purchaseMember.entity;

import com.umc.mot.auditable.Auditable;
import com.umc.mot.comment.entity.Comment;
import com.umc.mot.heart.entity.Heart;
import com.umc.mot.reserve.entity.Reserve;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class PurchaseMember extends Auditable {
    @Id
    private int purchaseMemberId; //구매자 회원 식별자

    @Column
    private int id; //회원아이디

    @Column
    private String name ; //회원 이름

    @Column
    private String imageUrl; //회원 이메일

    @Column
    private String phone; //회원 전화번호

    @Column
    private String Pw; //회원 비밀번호

    @Column
    private String host; // 회원 역할

    private String token; //토큰

    @OneToMany(mappedBy = "purchase_member", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "purchase_member", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private List<Heart> hearts = new ArrayList<>();

    @OneToMany(mappedBy = "purchase_member", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private List<Reserve> reserves = new ArrayList<>();
}