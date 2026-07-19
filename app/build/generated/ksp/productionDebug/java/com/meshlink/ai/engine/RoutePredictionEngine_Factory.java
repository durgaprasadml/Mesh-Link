package com.meshlink.ai.engine;

import com.meshlink.ai.data.LearningRepository;
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
public final class RoutePredictionEngine_Factory implements Factory<RoutePredictionEngine> {
  private final Provider<LearningRepository> learningRepositoryProvider;

  public RoutePredictionEngine_Factory(Provider<LearningRepository> learningRepositoryProvider) {
    this.learningRepositoryProvider = learningRepositoryProvider;
  }

  @Override
  public RoutePredictionEngine get() {
    return newInstance(learningRepositoryProvider.get());
  }

  public static RoutePredictionEngine_Factory create(
      Provider<LearningRepository> learningRepositoryProvider) {
    return new RoutePredictionEngine_Factory(learningRepositoryProvider);
  }

  public static RoutePredictionEngine newInstance(LearningRepository learningRepository) {
    return new RoutePredictionEngine(learningRepository);
  }
}
