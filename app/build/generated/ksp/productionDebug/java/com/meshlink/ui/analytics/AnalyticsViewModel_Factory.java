package com.meshlink.ui.analytics;

import com.meshlink.analytics.data.MeshAnalytics;
import com.meshlink.domain.repository.MeshRepository;
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

  private final Provider<MeshRepository> meshRepositoryProvider;

  public AnalyticsViewModel_Factory(Provider<MeshAnalytics> analyticsProvider,
      Provider<MeshRepository> meshRepositoryProvider) {
    this.analyticsProvider = analyticsProvider;
    this.meshRepositoryProvider = meshRepositoryProvider;
  }

  @Override
  public AnalyticsViewModel get() {
    return newInstance(analyticsProvider.get(), meshRepositoryProvider.get());
  }

  public static AnalyticsViewModel_Factory create(Provider<MeshAnalytics> analyticsProvider,
      Provider<MeshRepository> meshRepositoryProvider) {
    return new AnalyticsViewModel_Factory(analyticsProvider, meshRepositoryProvider);
  }

  public static AnalyticsViewModel newInstance(MeshAnalytics analytics,
      MeshRepository meshRepository) {
    return new AnalyticsViewModel(analytics, meshRepository);
  }
}
