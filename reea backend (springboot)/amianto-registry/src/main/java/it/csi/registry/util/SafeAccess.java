package it.csi.registry.util;

import java.util.Optional;
import java.util.function.Supplier;

public final class SafeAccess {

    private SafeAccess() {}

    @FunctionalInterface
    public interface UnsafeSupplier<T> {
        T get() throws Exception;
    }

    public static <T> T safeGet(Supplier<T> supplier) {
        try {
            return Optional.ofNullable(supplier.get()).orElse(null);
        } catch (NullPointerException e) {
            return null;
        }
    }
}
