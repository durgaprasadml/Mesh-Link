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
public final class BatteryPredictor_Factory implements Factory<BatteryPredictor> {
  private final Provider<LearningRepository> learningRepositoryProvider;

  public BatteryPredictor_Factory(Provider<LearningRepository> learningRepositoryProvider) {
    this.learningRepositoryProvider = learningRepositoryProvider;
  }

  @Override
  public BatteryPredictor get() {
    return newInstance(learningRepositoryProvider.get());
  }

  public static BatteryPredictor_Factory create(
      Provider<LearningRepository> learningRepositoryProvider) {
    return new BatteryPredictor_Factory(learningRepositoryProvider);
  }

  public static BatteryPredictor newInstance(LearningRepository learningRepository) {
    return new BatteryPredictor(learningRepository);
  }
}
