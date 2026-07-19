package com.meshlink.routing.engine;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class RouteCache_Factory implements Factory<RouteCache> {
  @Override
  public RouteCache get() {
    return newInstance();
  }

  public static RouteCache_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static RouteCache newInstance() {
    return new RouteCache();
  }

  private static final class InstanceHolder {
    private static final RouteCache_Factory INSTANCE = new RouteCache_Factory();
  }
}
