package com.meshlink.analytics.engine;

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
public final class RoutingAnalytics_Factory implements Factory<RoutingAnalytics> {
  @Override
  public RoutingAnalytics get() {
    return newInstance();
  }

  public static RoutingAnalytics_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static RoutingAnalytics newInstance() {
    return new RoutingAnalytics();
  }

  private static final class InstanceHolder {
    private static final RoutingAnalytics_Factory INSTANCE = new RoutingAnalytics_Factory();
  }
}
