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
public final class CongestionPredictor_Factory implements Factory<CongestionPredictor> {
  private final Provider<LearningRepository> learningRepositoryProvider;

  public CongestionPredictor_Factory(Provider<LearningRepository> learningRepositoryProvider) {
    this.learningRepositoryProvider = learningRepositoryProvider;
  }

  @Override
  public CongestionPredictor get() {
    return newInstance(learningRepositoryProvider.get());
  }

  public static CongestionPredictor_Factory create(
      Provider<LearningRepository> learningRepositoryProvider) {
    return new CongestionPredictor_Factory(learningRepositoryProvider);
  }

  public static CongestionPredictor newInstance(LearningRepository learningRepository) {
    return new CongestionPredictor(learningRepository);
  }
}
