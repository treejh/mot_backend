package com.umc.mot.oauth2.filter;

import com.umc.mot.oauth2.utils.CustomAuthorityUtils;
import com.umc.mot.purchaseMember.service.PurchaseMemberService;
import com.umc.mot.sellMember.entity.SellMember;
import com.umc.mot.sellMember.service.SellMemberService;
import com.umc.mot.token.entity.Token;
import com.umc.mot.token.service.TokenService;
import com.umc.mot.utils.CustomCookie;
import com.umc.mot.exception.BusinessLogicException;
import com.umc.mot.exception.ExceptionCode;
import com.umc.mot.oauth2.jwt.JwtTokenizer;
import com.umc.mot.purchaseMember.entity.PurchaseMember;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

public class JwtVerificationFilter extends OncePerRequestFilter {
    private final JwtTokenizer jwtTokenizer;
    private final CustomAuthorityUtils authorityUtils;
    private final TokenService tokenService;
    private final SellMemberService sellMemberService;
    private final PurchaseMemberService purchaseMemberService;
    private final CustomCookie cookie;

    public JwtVerificationFilter(JwtTokenizer jwtTokenizer,
                                 CustomAuthorityUtils authorityUtils,
                                 TokenService tokenService,
                                 SellMemberService sellMemberService,
                                 PurchaseMemberService purchaseMemberService,
                                 CustomCookie cookie) {
        System.out.println("!!!! test");
        this.jwtTokenizer = jwtTokenizer;
        this.authorityUtils = authorityUtils;
        this.tokenService = tokenService;
        this.sellMemberService = sellMemberService;
        this.purchaseMemberService = purchaseMemberService;
        this.cookie = cookie;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
         System.out.println("# JwtVerificationFilter");
        cookie.createCookie(request, response);

//        check(request);
        try {
            Map<String, Object> claims = verifyJws(request);
            setAuthenticationToContext(claims);
        } catch (SignatureException se) { // signature 에러
            request.setAttribute("exception", se);
        } catch (ExpiredJwtException ee) { // 기간 만료
            request.setAttribute("exception", ee);

            /*
                 access-Token의 유효기간이 만료되었을 때,
                 refresh-Token을 이용해서 access-Token 재발행
                 단, refresh-Token이 만료되면 재로그인 필요
            */

            verifyRefreshToken(request, response);

        } catch (Exception e) { // 그 외의 에러
            request.setAttribute("exception", e);
        }

        filterChain.doFilter(request, response);
    }

    // refresh 토큰 유효성 검사
    protected void verifyRefreshToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 새로운 AccessToken 발생 및 전달
            String accessToken = delegateAccessTokenByRefreshToken(request);
            response.setHeader("Authorization", accessToken);
            request.setAttribute("new-access-Token", accessToken);
        } catch (SignatureException se) {
            request.setAttribute("exception", se);
            System.out.println("!! RefreshToken의 signiture와 payload가 불일치하면 동작");
        } catch (ExpiredJwtException eee) {
            request.setAttribute("exception", eee);
            System.out.println("!! RefreshToken 유효기간 종료");
        } catch (Exception e) {
            request.setAttribute("exception", e);
            System.out.println("!! RefreshToken의 header 불일치하면 동작");
        }
    }

    // refresh-Token을 이용한 access-Token 재발행
    private String delegateAccessTokenByRefreshToken(HttpServletRequest request) {
        // Refresh 토큰 유효성 검증
        String jws = request.getHeader("Refresh");
        String base64EncodedSecretKey = jwtTokenizer.encodeBase64SecretKey(jwtTokenizer.getSecretKey());
        Map<String, Object> refreshClaims = jwtTokenizer.getClaims(jws, base64EncodedSecretKey).getBody();

        // 새로운 Access 토큰 발행
        String subject = (String) refreshClaims.get("sub");
        Token token;
        if(subject.contains("@")) { // accessToken의 subject 유효성 검사
            SellMember sellMember = sellMemberService.verifiedByEmail(subject);
            token = sellMember.getToken();
            if(sellMember == null) {
                PurchaseMember purchaseMember = purchaseMemberService.verifiedByEmail(subject);
                if(purchaseMember == null) throw new BusinessLogicException(ExceptionCode.PURCHASE_MEMBER_NOT_FOUND);
                token = purchaseMember.getToken();
            }
        } else {
            token = tokenService.verifiedLoginId(subject);
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", authorityUtils.createRoles(subject));

        Date expiration = jwtTokenizer.getTokenExpiration(jwtTokenizer.getAccessTokenExpirationMinutes());

        String accessToken = jwtTokenizer.generateAccessToken(claims, subject, expiration, base64EncodedSecretKey);

        // 새로 생성한 access 토큰의 claims 추출 후 context에 저장
        Map<String, Object> newAccessTokenclaims = jwtTokenizer.getClaims(accessToken, base64EncodedSecretKey).getBody();
        setAuthenticationToContext(newAccessTokenclaims);

        // 새로 발행된 access-Token 업데이트
        request.getHeader("Authorization");
        token.setAccessToken("Bearer " + accessToken);
        tokenService.patchToken(token);

        return "Bearer " + accessToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String authorization = request.getHeader("Authorization");
        System.out.println("!! Access : " + authorization);

        return authorization == null || !authorization.startsWith("Bearer");
    }

    // access token 유효성 검증
    private Map<String, Object> verifyJws(HttpServletRequest request) {
        String jws = request.getHeader("Authorization").replace("Bearer ", "");
        String base64EncodedSecretKey = jwtTokenizer.encodeBase64SecretKey(jwtTokenizer.getSecretKey());
        Map<String, Object> claims = jwtTokenizer.getClaims(jws, base64EncodedSecretKey).getBody();

        return claims;
    }

    private void setAuthenticationToContext(Map<String, Object> claims) {
        System.out.println("!!!!!");
        claims.keySet().stream().forEach(System.out::println);
        List<GrantedAuthority> authorities = authorityUtils.createAuthorities((List)claims.get("roles"));
        Authentication authentication = new UsernamePasswordAuthenticationToken(claims.get("sub"), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // access-Token을 이용해서 user 정보 조회하기
//    private void check(HttpServletRequest request) {
//        String token = request.getHeader("access-Token");
//        String newToken = (String) request.getAttribute("new-access-Token");
//        Optional<PurchaseMember> user = tokenService.findByToken(token);
//        Optional<PurchaseMember> newUser = tokenService.findByToken(newToken);
//
//        if(newToken == null) user.orElseThrow(() -> new BusinessLogicException(ExceptionCode.PURCHASE_MEMBER_NOT_FOUND));
//        else newUser.orElseThrow(() -> new BusinessLogicException(ExceptionCode.PURCHASE_MEMBER_NOT_FOUND));
//    }
}