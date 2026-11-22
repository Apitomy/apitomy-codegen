package org.example.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * The JAX-RS application.
 */
@ApplicationScoped
@ApplicationPath("/")
public class JaxRsApplication extends Application {
}
