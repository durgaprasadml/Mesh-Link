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
public final class AnomalyDetector_Factory implements Factory<AnomalyDetector> {
  private final Provider<LearningRepository> learningRepositoryProvider;

  public AnomalyDetector_Factory(Provider<LearningRepository> learningRepositoryProvider) {
    this.learningRepositoryProvider = learningRepositoryProvider;
  }

  @Override
  public AnomalyDetector get() {
    return newInstance(learningRepositoryProvider.get());
  }

  public static AnomalyDetector_Factory create(
      Provider<LearningRepository> learningRepositoryProvider) {
    return new AnomalyDetector_Factory(learningRepositoryProvider);
  }

  public static AnomalyDetector newInstance(LearningRepository learningRepository) {
    return new AnomalyDetector(learningRepository);
  }
}
