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
public final class TransportAnalytics_Factory implements Factory<TransportAnalytics> {
  @Override
  public TransportAnalytics get() {
    return newInstance();
  }

  public static TransportAnalytics_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TransportAnalytics newInstance() {
    return new TransportAnalytics();
  }

  private static final class InstanceHolder {
    private static final TransportAnalytics_Factory INSTANCE = new TransportAnalytics_Factory();
  }
}
