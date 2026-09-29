package com.servispeed.gateway.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.springframework.http.HttpStatus;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
public class SecurityFilter extends ZuulFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public String filterType() {
        return "pre"; 
    }

    @Override
    public int filterOrder() {
        return 1;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        
        // Si la petición va dirigida al servicio de autenticación, NO aplica el filtro de JWT
        return !request.getRequestURI().contains("/auth/");
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();

        String authHeader = request.getHeader("Authorization");

        // Verifica que el encabezado Authorization exista y empiece con "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            rejectRequest(ctx, "Token JWT ausente o formato inválido.");
            return Boolean.FALSE;
        }

        // Extraer el string puro del token 
        String token = authHeader.substring(7);

        try {
            // Validar la firma y expirar el token
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecret.getBytes())
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.getSubject();
            ctx.addZuulRequestHeader("X-User-Username", username);

            // Éxito: retorna TRUE para diferir del flujo de error
            return Boolean.TRUE;

        } catch (SignatureException e) {
            rejectRequest(ctx, "La firma del token no es válida.");
        } catch (ExpiredJwtException e) {
            rejectRequest(ctx, "El token JWT ha expirado.");
        } catch (MalformedJwtException | IllegalArgumentException e) {
            rejectRequest(ctx, "Token JWT mal formado.");
        }

        // Fallo en validación: retorna FALSE
        return Boolean.FALSE;
    }

    // Método auxiliar para detener la petición en seco si algo sale mal
    private void rejectRequest(RequestContext ctx, String mensaje) {
        ctx.setSendZuulResponse(false); // Le dice a Zuul que NO envíe la petición al microservicio
        ctx.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value()); // 401
        ctx.setResponseBody(String.format("{\"error\": \"No autorizado\", \"detalle\": \"%s\"}", mensaje));
        ctx.getResponse().setContentType("application/json");
    }
}