package io.github.liamtkehoe.nhl_pickem.web;

import io.github.liamtkehoe.nhl_pickem.service.AdminAccess;
import io.github.liamtkehoe.nhl_pickem.service.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminInterceptor implements HandlerInterceptor {
    private final AdminAccess admins;

    public AdminInterceptor(AdminAccess admins) {
        this.admins = admins;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!admins.isAdmin(request.getHeader(PicksController.USER_HEADER))) {
            throw new ForbiddenException("Admins only");
        }

        return true;
    }
}
