package rocks.stalin.android.app.framework;

/**
 * Created by delusional on 5/13/17.
 */

import java.util.HashMap;
import java.util.Map;

/**
 * Service locator singleton
 *
 * <p>
 *     This is where you put all your global garbage. Please only use it if you can't
 *     inject using any other (simpler) method, such as Activities.
 * </p>
 */
public class ServiceLocator {
    private Map<Class<?>, Object> services = new HashMap<>();

    private static ServiceLocator instance;

    private ServiceLocator() {
    }

    /**
     * Get the global singleton service locator
     * @return the service locator
     */
    public static ServiceLocator getInstance() {
        if(instance == null)
            instance = new ServiceLocator();
        return instance;
    }

    /**
     * Add a global service
     * @param type The type key of the service
     * @param instance The instance to add to the application context
     */
    public <T> void putService(Class<T> type, T instance) {
        services.put(type, instance);
    }

    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> type) {
        return (T) services.get(type);
    }
}
