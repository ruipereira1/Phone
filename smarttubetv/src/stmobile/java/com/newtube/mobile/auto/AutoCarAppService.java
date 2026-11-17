package com.newtube.mobile.auto;

import androidx.annotation.NonNull;
import androidx.car.app.CarAppService;
import androidx.car.app.Session;
import androidx.car.app.validation.HostValidator;

/**
 * NEWTUBE-AUTO: Android Auto entry point. Sideloaded builds are only shown by Android Auto with
 * "Unknown sources" on in its developer settings, so any host is accepted.
 */
public class AutoCarAppService extends CarAppService {
    @NonNull
    @Override
    public HostValidator createHostValidator() {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
    }

    @NonNull
    @Override
    public Session onCreateSession() {
        return new AutoSession();
    }
}
