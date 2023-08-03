package com.umc.mot.oauth2.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.mot.oauth2.jwt.JwtTokenizer;
import com.umc.mot.oauth2.utils.CustomAuthorityUtils;
import com.umc.mot.purchaseMember.entity.PurchaseMember;
import com.umc.mot.purchaseMember.service.PurchaseMemberService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class OAuth2MemberSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtTokenizer jwtTokenizer;
    private final CustomAuthorityUtils authorityUtils;
    private final PurchaseMemberService memberService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        var oAuth2PurchaseMember = (OAuth2User) authentication.getPrincipal();

        String email = String.valueOf(oAuth2PurchaseMember.getAttributes().get("email"));
        if(email.equals("null")) email = String.valueOf(oAuth2PurchaseMember.getAttributes().get("html_url"));
        PurchaseMember member = memberService.findByEmail(email);
        List<String> authorities = authorityUtils.createRoles(email);

        // email로 가입된 사람이 없는 경우
        if(member == null) {
            if(request.getServletPath().contains("google")) {
                String name = String.valueOf(oAuth2PurchaseMember.getAttributes().get("name"));
                String imgUrl = String.valueOf(oAuth2PurchaseMember.getAttributes().get("picture"));
                member = savePurchaseMember(email, name, imgUrl);
            }
            else if(request.getServletPath().contains("github")) {
                String name = String.valueOf(oAuth2PurchaseMember.getAttributes().get("login"));
                String imgUrl = String.valueOf(oAuth2PurchaseMember.getAttributes().get("avatar_url"));
                member = savePurchaseMember(email, name, imgUrl);
            }
        }

        // 콘솔 출력 코드
//        oAuth2PurchaseMember.getAttributes().forEach((s, o) -> System.out.println("!! " + s + " : " + String.valueOf(o)));
//        oAuth2PurchaseMember.getAuthorities().stream().forEach(grantedAuthority -> System.out.println("!! granted : " + grantedAuthority.getAuthority()));
//        System.out.println("!! url : " + request.getRequestURI());

        redirect(request, response, member, authorities);
    }

    private PurchaseMember savePurchaseMember(String email, String name, String imgUrl) {
        PurchaseMember member = new PurchaseMember(email, name, imgUrl);
        return memberService.createPurchaseMember(member);
    }

    private void redirect(HttpServletRequest request, HttpServletResponse response,
                          PurchaseMember member,
                          List<String> authorities) throws IOException {
        String accessToken = "Bearer " + delegateAccessToken(member.getName(), member.getEmail(), authorities);
        String refreshToken = delegateRefreshToken(member.getEmail());

        // ---------------------------------------------------------------------------
        // response body 안에 token 값 저장
        saveTokenInResponseBody(response, accessToken, refreshToken);

        // response header 안에 token 값 저장
        response.setHeader("access-Token", accessToken);
        response.setHeader("refresh-Token", refreshToken);

        // access token & refresh token 저장
        member.setToken(accessToken);
        member.setRefresh(refreshToken);
        memberService.updatePurchaseMember(member);

        // 콘솔 출력 코드
//        System.out.println("!! url : " + response.getHeaderNames().toString());
//        System.out.println("!! " + response.getHeader("Authorization"));
        // ---------------------------------------------------------------------------

        String uri = createURI(accessToken, refreshToken, request).toString();
        getRedirectStrategy().sendRedirect(request, response, uri);
    }

    // response body 안에 token 값 저장
    private void saveTokenInResponseBody(HttpServletResponse response, String accessToken, String refreshToken) throws IOException {
        // response.body 설정
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("utf-8");

        // Token을 TokenResponse로 변환
        TokenResponse tokenResponse = new TokenResponse(
                accessToken,
                refreshToken
        );

        // json 형식으로 변환
        ObjectMapper mapper = new ObjectMapper();
        String result = mapper.writeValueAsString(tokenResponse);
        response.getWriter().write(result);
    }

    // access token 생성
    private String delegateAccessToken(String name, String email, List<String> authorities) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("membername", name);
        claims.put("roles", authorities);

        String subject = email;
        Date expiration = jwtTokenizer.getTokenExpiration(jwtTokenizer.getAccessTokenExpirationMinutes());

        String base64EncodedSecretKey = jwtTokenizer.encodeBase64SecretKey(jwtTokenizer.getSecretKey());

        String accessToken = jwtTokenizer.generateAccessToken(claims, subject, expiration, base64EncodedSecretKey);

        return accessToken;
    }

    // refresh token 생성
    private String delegateRefreshToken(String email) {
        String subject = email;
        Date expiration = jwtTokenizer.getTokenExpiration(jwtTokenizer.getRefreshTokenExpirationMinutes());
        String base64EncodedSecretKey = jwtTokenizer.encodeBase64SecretKey(jwtTokenizer.getSecretKey());

        String refreshToken = jwtTokenizer.generateRefreshToken(subject, expiration, base64EncodedSecretKey);

        return refreshToken;
    }

    // 리다이렉트 URL 생성
    private URI createURI(String accessToken, String refreshToken, HttpServletRequest request) {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("access-Token", accessToken);
        queryParams.add("refresh-Token", refreshToken);

        CustomCookie cookie = new CustomCookie();
        String[] url = cookie.readURLfromCookie(request);
        String protocol = url[0];
        String host = url[1];
        String port = url[2];

        if(host.equals("localhost")){
            System.out.println("!! localhost : " + host);
            return UriComponentsBuilder
                    .newInstance()
                    .scheme(protocol)
                    .host(host)
                    .port(port)
                    .path("/Token.html")
                    .queryParams(queryParams)
                    .build()
                    .toUri();
//        } else if(port == null || port.equals("")) {
        } else  {
            System.out.println("!! s3 : " + host);
            return UriComponentsBuilder
                    .newInstance()
                    .scheme(protocol)
                    .host(host)
                    .path("/Token.html")
                    .queryParams(queryParams)
                    .build()
                    .toUri();
        }

        // backend local test
//        return UriComponentsBuilder
//                .newInstance()
//                .scheme("http")
//                .host("localhost")
////                .port(8080)
//                .path("/receive-token.html")
//                .queryParams(queryParams)
//                .build()
//                .toUri();

        // frontend local test
//        return UriComponentsBuilder
//                .newInstance()
//                .scheme("http")
//                .host("localhost")
//                .port(3000)
//                .path("/Token.html")
//                .queryParams(queryParams)
//                .build()
//                .toUri();

        // S3 배포 시 : http://seb41-main-022.s3-website.ap-northeast-2.amazonaws.com/
//        return UriComponentsBuilder
//                .newInstance()
//                .scheme("http")
//                .host("seb41-main-022.s3-website.ap-northeast-2.amazonaws.com/")
//                .path("/Token.html")
//                .queryParams(queryParams)
//                .build()
//                .toUri();
    }

    @AllArgsConstructor
    @Setter
    @Getter
    public class PurchaseMemberResopnse {
        private long memberId;
        private String name;
        private String email;
    }

    @AllArgsConstructor
    @Setter
    @Getter
    public class TokenResponse {
        private String accessToken;
        private String refreshToken;
    }
}
