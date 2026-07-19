package com.meshlink.scalability.engine;

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
public final class AdaptiveDiscoveryManager_Factory implements Factory<AdaptiveDiscoveryManager> {
  @Override
  public AdaptiveDiscoveryManager get() {
    return newInstance();
  }

  public static AdaptiveDiscoveryManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static AdaptiveDiscoveryManager newInstance() {
    return new AdaptiveDiscoveryManager();
  }

  private static final class InstanceHolder {
    private static final AdaptiveDiscoveryManager_Factory INSTANCE = new AdaptiveDiscoveryManager_Factory();
  }
}
