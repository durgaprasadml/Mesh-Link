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
public final class FailurePredictor_Factory implements Factory<FailurePredictor> {
  private final Provider<LearningRepository> learningRepositoryProvider;

  public FailurePredictor_Factory(Provider<LearningRepository> learningRepositoryProvider) {
    this.learningRepositoryProvider = learningRepositoryProvider;
  }

  @Override
  public FailurePredictor get() {
    return newInstance(learningRepositoryProvider.get());
  }

  public static FailurePredictor_Factory create(
      Provider<LearningRepository> learningRepositoryProvider) {
    return new FailurePredictor_Factory(learningRepositoryProvider);
  }

  public static FailurePredictor newInstance(LearningRepository learningRepository) {
    return new FailurePredictor(learningRepository);
  }
}
