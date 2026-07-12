package com.meshlink.ui.analytics;

import com.meshlink.data.analytics.MeshAnalytics;
import com.meshlink.data.ble.MeshRouter;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class AnalyticsViewModel_Factory implements Factory<AnalyticsViewModel> {
  private final Provider<MeshAnalytics> analyticsProvider;

  private final Provider<MeshRouter> meshRouterProvider;

  public AnalyticsViewModel_Factory(Provider<MeshAnalytics> analyticsProvider,
      Provider<MeshRouter> meshRouterProvider) {
    this.analyticsProvider = analyticsProvider;
    this.meshRouterProvider = meshRouterProvider;
  }

  @Override
  public AnalyticsViewModel get() {
    return newInstance(analyticsProvider.get(), meshRouterProvider.get());
  }

  public static AnalyticsViewModel_Factory create(Provider<MeshAnalytics> analyticsProvider,
      Provider<MeshRouter> meshRouterProvider) {
    return new AnalyticsViewModel_Factory(analyticsProvider, meshRouterProvider);
  }

  public static AnalyticsViewModel newInstance(MeshAnalytics analytics, MeshRouter meshRouter) {
    return new AnalyticsViewModel(analytics, meshRouter);
  }
}
