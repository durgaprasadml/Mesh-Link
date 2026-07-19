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
public final class PeerIntelligenceManager_Factory implements Factory<PeerIntelligenceManager> {
  @Override
  public PeerIntelligenceManager get() {
    return newInstance();
  }

  public static PeerIntelligenceManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static PeerIntelligenceManager newInstance() {
    return new PeerIntelligenceManager();
  }

  private static final class InstanceHolder {
    private static final PeerIntelligenceManager_Factory INSTANCE = new PeerIntelligenceManager_Factory();
  }
}
