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

public class SecurityFilter extends ZuulFilter {

    private final String JWT_SECRET = "Rz/0bjufMPmSHYIxfNABdTgVvLyIcgIvkhH8Y3L37yM=";

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
        return true;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();

        String authHeader = request.getHeader("Authorization");

        // 1. Verificar que el encabezado Authorization exista y empiece con "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            rejectRequest(ctx, "Token JWT ausente o formato inválido.");
            return null;
        }

        // 2. Extraer el string puro del token (quitando "Bearer ")
        String token = authHeader.substring(7);

        try {
            // 3. Validar la firma y expirar el token
            // Si la firma es falsa o el token expiró, esto lanzará una excepción automáticamente
            Claims claims = Jwts.parser()
                    .setSigningKey(JWT_SECRET.getBytes())
                    .parseClaimsJws(token)
                    .getBody();

            // Opcional: Puedes extraer información del usuario (como el id o rol) 
            // y pasarla hacia los microservicios mediante headers internos
            String username = claims.getSubject();
            ctx.addZuulRequestHeader("X-User-Username", username);
            
        } catch (SignatureException e) {
            rejectRequest(ctx, "La firma del token no es válida.");
        } catch (ExpiredJwtException e) {
            rejectRequest(ctx, "El token JWT ha expirado.");
        } catch (MalformedJwtException | IllegalArgumentException e) {
            rejectRequest(ctx, "Token JWT mal formado.");
        }

        return null;
    }

    // Método auxiliar para detener la petición en seco si algo sale mal
    private void rejectRequest(RequestContext ctx, String mensaje) {
        ctx.setSendZuulResponse(false); // Le dice a Zuul que NO envíe la petición al microservicio
        ctx.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value()); // 401
        ctx.setResponseBody(String.format("{\"error\": \"No autorizado\", \"detalle\": \"%s\"}", mensaje));
        ctx.getResponse().setContentType("application/json");
    }
}