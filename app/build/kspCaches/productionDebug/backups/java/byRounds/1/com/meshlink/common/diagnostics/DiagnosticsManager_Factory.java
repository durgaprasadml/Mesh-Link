package com.meshlink.common.diagnostics;

import com.meshlink.common.logger.EventTimeline;
import com.meshlink.common.metrics.MetricsManager;
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
public final class DiagnosticsManager_Factory implements Factory<DiagnosticsManager> {
  private final Provider<MetricsManager> metricsManagerProvider;

  private final Provider<EventTimeline> eventTimelineProvider;

  public DiagnosticsManager_Factory(Provider<MetricsManager> metricsManagerProvider,
      Provider<EventTimeline> eventTimelineProvider) {
    this.metricsManagerProvider = metricsManagerProvider;
    this.eventTimelineProvider = eventTimelineProvider;
  }

  @Override
  public DiagnosticsManager get() {
    return newInstance(metricsManagerProvider.get(), eventTimelineProvider.get());
  }

  public static DiagnosticsManager_Factory create(Provider<MetricsManager> metricsManagerProvider,
      Provider<EventTimeline> eventTimelineProvider) {
    return new DiagnosticsManager_Factory(metricsManagerProvider, eventTimelineProvider);
  }

  public static DiagnosticsManager newInstance(MetricsManager metricsManager,
      EventTimeline eventTimeline) {
    return new DiagnosticsManager(metricsManager, eventTimeline);
  }
}
