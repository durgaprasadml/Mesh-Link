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
public final class TransportPredictor_Factory implements Factory<TransportPredictor> {
  private final Provider<LearningRepository> learningRepositoryProvider;

  private final Provider<BatteryPredictor> batteryPredictorProvider;

  public TransportPredictor_Factory(Provider<LearningRepository> learningRepositoryProvider,
      Provider<BatteryPredictor> batteryPredictorProvider) {
    this.learningRepositoryProvider = learningRepositoryProvider;
    this.batteryPredictorProvider = batteryPredictorProvider;
  }

  @Override
  public TransportPredictor get() {
    return newInstance(learningRepositoryProvider.get(), batteryPredictorProvider.get());
  }

  public static TransportPredictor_Factory create(
      Provider<LearningRepository> learningRepositoryProvider,
      Provider<BatteryPredictor> batteryPredictorProvider) {
    return new TransportPredictor_Factory(learningRepositoryProvider, batteryPredictorProvider);
  }

  public static TransportPredictor newInstance(LearningRepository learningRepository,
      BatteryPredictor batteryPredictor) {
    return new TransportPredictor(learningRepository, batteryPredictor);
  }
}
