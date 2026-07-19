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
public final class RouteScorer_Factory implements Factory<RouteScorer> {
  @Override
  public RouteScorer get() {
    return newInstance();
  }

  public static RouteScorer_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static RouteScorer newInstance() {
    return new RouteScorer();
  }

  private static final class InstanceHolder {
    private static final RouteScorer_Factory INSTANCE = new RouteScorer_Factory();
  }
}
