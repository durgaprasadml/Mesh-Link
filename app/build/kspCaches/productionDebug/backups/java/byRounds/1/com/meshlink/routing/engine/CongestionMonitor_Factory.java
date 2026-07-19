package com.meshlink.routing.engine;

import com.meshlink.ai.engine.CongestionPredictor;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class CongestionMonitor_Factory implements Factory<CongestionMonitor> {
  private final Provider<CongestionPredictor> congestionPredictorProvider;

  public CongestionMonitor_Factory(Provider<CongestionPredictor> congestionPredictorProvider) {
    this.congestionPredictorProvider = congestionPredictorProvider;
  }

  @Override
  public CongestionMonitor get() {
    return newInstance(congestionPredictorProvider.get());
  }

  public static CongestionMonitor_Factory create(
      Provider<CongestionPredictor> congestionPredictorProvider) {
    return new CongestionMonitor_Factory(congestionPredictorProvider);
  }

  public static CongestionMonitor newInstance(CongestionPredictor congestionPredictor) {
    return new CongestionMonitor(congestionPredictor);
  }
}
