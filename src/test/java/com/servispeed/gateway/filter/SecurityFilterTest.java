package com.servispeed.gateway.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import com.netflix.zuul.context.RequestContext;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class SecurityFilterTest {

    @InjectMocks
    private SecurityFilter securityFilter;

    private final String SECRET = "Rz/0bjufMPmSHYIxfNABdTgVvLyIcgIvkhH8Y3L37yM=";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        RequestContext.testSetCurrentContext(new RequestContext());
        // Inyecta el valor de @Value("${jwt.secret}")
        ReflectionTestUtils.setField(securityFilter, "jwtSecret", SECRET);
    }

    @Test
    public void filterType_ReturnsPre() {
        assertEquals("pre", securityFilter.filterType());
    }

    @Test
    public void filterOrder_ReturnsOne() {
        assertEquals(1, securityFilter.filterOrder());
    }

    @Test
    public void shouldFilter_ReturnsFalse_ForAuthRoutes() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
        RequestContext.getCurrentContext().setRequest(request);
        assertFalse(securityFilter.shouldFilter());
    }

    @Test
    public void shouldFilter_ReturnsTrue_ForNonAuthRoutes() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/ordenes");
        RequestContext.getCurrentContext().setRequest(request);
        assertTrue(securityFilter.shouldFilter());
    }

    @Test
    public void run_Fails_WhenNoAuthHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.setRequest(request);
        ctx.setResponse(new MockHttpServletResponse());

        assertFalse((Boolean) securityFilter.run());
        assertEquals(401, ctx.getResponseStatusCode());
    }

    @Test
    public void run_Success_WithValidToken() {
        String token = Jwts.builder()
                .setSubject("admin")
                .claim("id", 11)
                .signWith(SignatureAlgorithm.HS256, SECRET.getBytes())
                .compact();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.setRequest(request);

        assertTrue((Boolean) securityFilter.run());
        
        assertEquals("admin", ctx.getZuulRequestHeaders().get("x-user-username"));
        assertEquals("11", ctx.getZuulRequestHeaders().get("x-user-id"));
    }

    @Test
    public void run_Fails_InvalidSignature() {
        String wrongSecret = "Wz/0bjufMPmSHYIxfNABdTgVvLyIcgIvkhH8Y3L37yM="; // Secreto diferente
        String token = Jwts.builder()
                .setSubject("admin")
                .signWith(SignatureAlgorithm.HS256, wrongSecret.getBytes())
                .compact();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.setRequest(request);
        ctx.setResponse(new MockHttpServletResponse());

        assertFalse((Boolean) securityFilter.run());
        assertEquals(401, ctx.getResponseStatusCode());
    }

    @Test
    public void run_Fails_MalformedToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token_invalido_sin_estructura");
        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.setRequest(request);
        ctx.setResponse(new MockHttpServletResponse());

        assertFalse((Boolean) securityFilter.run());
        assertEquals(401, ctx.getResponseStatusCode());
    }

    @Test
    public void run_Fails_ExpiredToken() {
        String token = Jwts.builder()
                .setSubject("admin")
                .setExpiration(new Date(System.currentTimeMillis() - 10000)) // 10 segundos en el pasado
                .signWith(SignatureAlgorithm.HS256, SECRET.getBytes())
                .compact();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.setRequest(request);
        ctx.setResponse(new MockHttpServletResponse());

        assertFalse((Boolean) securityFilter.run());
        assertEquals(401, ctx.getResponseStatusCode());
    }
}