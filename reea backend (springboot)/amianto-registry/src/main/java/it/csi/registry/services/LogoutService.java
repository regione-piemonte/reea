package it.csi.registry.services;

import jakarta.servlet.http.HttpServletRequest;

public interface LogoutService {

    void invalidateSession(HttpServletRequest request);

    String getLogoutUrl();
}
