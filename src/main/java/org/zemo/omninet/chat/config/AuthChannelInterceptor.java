package org.zemo.omninet.chat.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.zemo.omninet.security.model.User;
import org.zemo.omninet.security.service.JwtService;
import org.zemo.omninet.security.service.UserService;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserService userService;

    @Override
    public Message<?> preSend(@NotNull Message<?> message, @NotNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);

                try {
                    if (jwtService.isTokenValid(jwt) && jwtService.isAccessToken(jwt)) {
                        String userId = jwtService.getUserIdFromToken(jwt);
                        Optional<User> userOpt = userService.getUserById(userId);

                        if (userOpt.isPresent()) {
                            User user = userOpt.get();
                            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
                            );
                            accessor.setUser(authToken);
                            log.info("User {} authenticated for WebSocket session.", user.getEmail());
                        } else {
                            log.warn("User not found for JWT token in WebSocket connect frame: {}", userId);
                            // You might want to throw an exception here to reject the connection
                        }
                    } else {
                        log.warn("Invalid JWT token provided in WebSocket connect frame.");
                        // Throw exception to reject
                    }
                } catch (Exception e) {
                    log.error("Error processing JWT token for WebSocket: {}", e.getMessage());
                    // Throw exception to reject
                }
            } else {
                log.warn("No Authorization header found in WebSocket connect frame.");
                // Throw exception to reject
            }
        }
        return message;
    }
}
