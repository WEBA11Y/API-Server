package com.weba11y.server.util;

import com.weba11y.server.domain.Member;
import com.weba11y.server.domain.Token;
import com.weba11y.server.dto.member.TokenInfo;
import com.weba11y.server.exception.custom.ExpiredTokenException;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
public class JwtUtil {
    // Token 정보
    public static TokenInfo getTokenInfo(String token, String secret) {
        // JWT 파싱, Claims 추출
        Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
        Long memberId = claims.get("memberId", Long.class);
        String role = claims.get("role", String.class);
        return TokenInfo.builder()
                .memberId(memberId)
                .role(role)
                .build();
    }

    // Token 만료
    public static void validateToken(String token, String secret) {
        try {
            // JWT 파싱, Claims 추출
            Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
            Date expiration = claims.getExpiration(); // Token 만료일
            if (expiration != null && expiration.before(new Date())) {
                throw new ExpiredTokenException("토큰이 만료되었습니다");
            }
        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException("토큰이 만료되었습니다");
        } catch (SignatureException e) {
            throw new SignatureException("유효하지 않은 토큰입니다.");
        } catch (MalformedJwtException e) {
            throw new MalformedJwtException("잘못된 토큰 유형입니다");
        }
    }

    // Token 생성
    public static Token createToken(Member member, Long accessTokenExpiration, Long refreshTokenExpiration, String secret) {
        Claims claims = Jwts.claims();
        claims.put("memberId", member.getId());
        claims.put("role", member.getRole());
        String accessToken = jwtBuild(claims, accessTokenExpiration, secret);
        String refreshToken = jwtBuild(claims, refreshTokenExpiration, secret);

        return Token.builder()
                .access_token(accessToken)
                .refresh_token(refreshToken)
                .build();
    }

    // JWT builder
    private static String jwtBuild(Claims claims, Long expiration, String secret) {
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date((System.currentTimeMillis())))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    // Access Token 재발급
    public static String reissuingToken(TokenInfo tokenInfo, Long accessTokenExpiration, String secret) {
        Claims claims = Jwts.claims();
        claims.put("memberId", tokenInfo.getMemberId());
        claims.put("role", tokenInfo.getRole());
        return jwtBuild(claims, accessTokenExpiration, secret);
    }

}