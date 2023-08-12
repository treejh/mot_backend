package com.umc.mot.room.service;

import com.umc.mot.exception.BusinessLogicException;
import com.umc.mot.exception.ExceptionCode;
import com.umc.mot.hotel.entity.Hotel;
import com.umc.mot.hotel.service.HotelService;
import com.umc.mot.reserve.service.ReserveService;
import com.umc.mot.room.entity.Room;
import com.umc.mot.room.repository.RoomRepository;
import com.umc.mot.sellMember.entity.SellMember;
import com.umc.mot.sellMember.repository.SellMemberRepository;
import com.umc.mot.token.service.TokenService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class RoomService{

    private final RoomRepository roomRepository;
    private final HotelService hotelService;
    private final TokenService tokenService;
//    private final ReserveService reserveService;

    //Create
    public Room createRoom(Room room,int hotelId) {
        SellMember sellM = tokenService.getLoginSellMember();
        Hotel hotel = hotelService.verifiedHotel(hotelId);
        room.setHotel(hotel);
        return roomRepository.save(room);
    }

    // Read
    public Room findRoomId(int roomId){
        Room room = verifiedRoom(roomId);
        return room;
    }


    // Update
    public Room patchRoom(Room room) {
        SellMember sellM = tokenService.getLoginSellMember();
        Room findRoom = verifiedRoom(room.getId());
        Optional.ofNullable(room.getName()).ifPresent(findRoom::setName);
        Optional.ofNullable(room.getMinPeople()).ifPresent(findRoom::setMinPeople);
        Optional.ofNullable(room.getMaxPeople()).ifPresent(findRoom::setMaxPeople);
        Optional.ofNullable(room.getPrice()).ifPresent(findRoom::setPrice);
        Optional.ofNullable(room.getInfo()).ifPresent(findRoom::setInfo);
        Optional.ofNullable(room.getRoomType()).ifPresent(findRoom::setRoomType);
        Optional.ofNullable(room.getPhotos()).ifPresent(findRoom::setPhotos);



        return roomRepository.save(findRoom);
    }

    // Delete
    public void deleteRoom(int roomId) {
        SellMember sellM = tokenService.getLoginSellMember();
        Room room = verifiedRoom(roomId);
        roomRepository.delete(room);
    }

    // 객실 검증
    public Room verifiedRoom(int roomId) {
        Optional<Room> member = roomRepository.findById(roomId);
        return member.orElseThrow(() -> new BusinessLogicException(ExceptionCode.ROOM_NOT_FOUND));

    }
}
