package com.meshlink.analytics.engine;

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
public final class PerformanceAdvisor_Factory implements Factory<PerformanceAdvisor> {
  private final Provider<MeshAnalyticsManager> meshAnalyticsProvider;

  private final Provider<RoutingAnalytics> routingAnalyticsProvider;

  private final Provider<TransportAnalytics> transportAnalyticsProvider;

  public PerformanceAdvisor_Factory(Provider<MeshAnalyticsManager> meshAnalyticsProvider,
      Provider<RoutingAnalytics> routingAnalyticsProvider,
      Provider<TransportAnalytics> transportAnalyticsProvider) {
    this.meshAnalyticsProvider = meshAnalyticsProvider;
    this.routingAnalyticsProvider = routingAnalyticsProvider;
    this.transportAnalyticsProvider = transportAnalyticsProvider;
  }

  @Override
  public PerformanceAdvisor get() {
    return newInstance(meshAnalyticsProvider.get(), routingAnalyticsProvider.get(), transportAnalyticsProvider.get());
  }

  public static PerformanceAdvisor_Factory create(
      Provider<MeshAnalyticsManager> meshAnalyticsProvider,
      Provider<RoutingAnalytics> routingAnalyticsProvider,
      Provider<TransportAnalytics> transportAnalyticsProvider) {
    return new PerformanceAdvisor_Factory(meshAnalyticsProvider, routingAnalyticsProvider, transportAnalyticsProvider);
  }

  public static PerformanceAdvisor newInstance(MeshAnalyticsManager meshAnalytics,
      RoutingAnalytics routingAnalytics, TransportAnalytics transportAnalytics) {
    return new PerformanceAdvisor(meshAnalytics, routingAnalytics, transportAnalytics);
  }
}
