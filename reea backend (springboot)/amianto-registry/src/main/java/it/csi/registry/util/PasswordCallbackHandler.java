package it.csi.registry.util;

import org.apache.wss4j.common.ext.WSPasswordCallback;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;

public class PasswordCallbackHandler implements CallbackHandler {

    private final String user;
    private final String password;

    public PasswordCallbackHandler(String user, String password) {
        this.user = user;
        this.password = password;
    }

    @Override
    public void handle(Callback[] callbacks)
            throws IOException, UnsupportedCallbackException {

        for (Callback callback : callbacks) {
            if (callback instanceof WSPasswordCallback pwcb) {
                if (user.equals(pwcb.getIdentifier())) {
                    pwcb.setPassword(password);
                }
            }
        }
    }
}
