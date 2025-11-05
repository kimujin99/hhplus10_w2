package com.example.hhplus_ecommerce.application.service;

import com.example.hhplus_ecommerce.domain.model.Coupon;
import com.example.hhplus_ecommerce.domain.model.User;
import com.example.hhplus_ecommerce.domain.model.UserCoupon;
import com.example.hhplus_ecommerce.domain.repository.CouponRepository;
import com.example.hhplus_ecommerce.domain.repository.UserCouponRepository;
import com.example.hhplus_ecommerce.domain.repository.UserRepository;
import com.example.hhplus_ecommerce.presentation.common.BusinessException;
import com.example.hhplus_ecommerce.presentation.common.ErrorCode;
import com.example.hhplus_ecommerce.presentation.dto.CouponDto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;

    public List<CouponResponse> getCoupons() {
        List<Coupon> coupons = couponRepository.findAll();
        return CouponResponse.fromList(coupons);
    }

    public UserCouponResponse issueCoupon(Long userId, IssueCouponRequest request) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        Coupon coupon = couponRepository.findById(request.couponId());
        if (coupon == null) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND);
        }
        UserCoupon existingUserCoupon = userCouponRepository.findByUserIdAndCouponId(userId, request.couponId());
        if (existingUserCoupon != null) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        coupon.issue();
        couponRepository.save(coupon);

        UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .couponId(request.couponId())
                .build();
        UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

        return UserCouponResponse.from(savedUserCoupon, coupon);
    }

    public List<UserCouponResponse> getUserCoupons(Long userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);

        List<Long> couponIds = userCoupons.stream()
                .map(UserCoupon::getCouponId)
                .distinct()
                .toList();

        // TODO: 실제 DB로 전환시 로직 제거. JOIN으로 처리
        Map<Long, Coupon> couponMap = couponIds.stream()
                .map(couponRepository::findById)
                .filter(coupon -> coupon != null)
                .collect(Collectors.toMap(Coupon::getId, coupon -> coupon));

        return userCoupons.stream()
                .map(userCoupon -> {
                    Coupon coupon = couponMap.get(userCoupon.getCouponId());
                    if (coupon == null) {
                        throw new BusinessException(ErrorCode.COUPON_NOT_FOUND);
                    }
                    return UserCouponResponse.from(userCoupon, coupon);
                })
                .toList();
    }
}