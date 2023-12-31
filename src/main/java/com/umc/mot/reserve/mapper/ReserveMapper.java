package com.umc.mot.reserve.mapper;

import com.umc.mot.category.dto.CategoryResponseDto;
import com.umc.mot.category.entity.Category;
import com.umc.mot.hotel.entity.Hotel;
import com.umc.mot.packagee.dto.PackageResponseDto;
import com.umc.mot.packagee.entity.Package;
import com.umc.mot.purchaseMember.entity.PurchaseMember;
import com.umc.mot.reserve.dto.ReserveRequestDto;
import com.umc.mot.reserve.dto.ReserveResponseDto;
import com.umc.mot.reserve.entity.Reserve;
import com.umc.mot.room.entity.Room;
import com.umc.mot.roomPackage.dto.RoomResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Mapper(componentModel = "spring")
public interface ReserveMapper {
    ReserveResponseDto.Response ReserveToReserveResponseDto(Reserve reserve);
    Reserve ReserveRequestDtoPostToReserve(ReserveRequestDto.Post post);
    Reserve ReserveRequestDtoPatchToReserve(ReserveRequestDto.Patch patch);

    ReserveResponseDto.Get ReserveToGetResponseDto(Reserve reserve, ReserveResponseDto.HotelInfo hotelInfo, List<ReserveResponseDto.RoomInfo> roomInfo, List<ReserveResponseDto.PackageInfo> packageInfo);
    ReserveResponseDto.HotelInfo ResponseToHotel(Hotel hotel);
    ReserveResponseDto.PackageInfo ResponseToPackage(Package packagee);
    ReserveResponseDto.RoomInfo ResponseToRoom(Room room);
    @Mapping(source = "reserve.id", target = "id")
    ReserveResponseDto.ReserveInfo ResponseToReserve(Reserve reserve, List<ReserveResponseDto.PackageInfo> packageInfo, List<ReserveResponseDto.RoomInfo> roomInfo, Hotel hotel);
    @Mapping(source = "purchaseMember.name", target = "name")
    @Mapping(source = "reserve.phone", target = "phone")
    ReserveResponseDto.DetailInfo ResponseToDetail(Reserve reserve, PurchaseMember purchaseMember, List<ReserveResponseDto.PackageInfo> packageInfo, List<ReserveResponseDto.RoomInfo> roomInfo);
}
