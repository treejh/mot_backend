package com.umc.mot.reserve.entity;

import com.umc.mot.auditable.Auditable;
import com.umc.mot.hotel.entity.Hotel;
import com.umc.mot.packagee.entity.Package;
import com.umc.mot.purchaseMember.entity.PurchaseMember;
import com.umc.mot.room.entity.Room;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Reserve extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id; //예약 식별아이디

    @Column
    private int paymentPrice; // 결제금액

    @Column
    private LocalDate checkIn; //체크인

    @Column
    private LocalDate checkOut; //체크아웃

    @Column
    private String phone; //핸드폰

    @Column
    private int peopleNum; //예약인원

    @Column
    @ElementCollection
    private List<Integer> roomsId = new ArrayList<>(); // 예약된 객실 식별자

    @Column
    @ElementCollection
    private List<Integer> packagesId = new ArrayList<>(); // 예약된 패지키 식별자


    @ManyToOne
    @JoinColumn(name = "PURCHASE_MEMBER_ID")
    private PurchaseMember purchaseMember;

    @ManyToOne
    @JoinColumn(name = "HOTEL_ID")
    private Hotel hotel;
}