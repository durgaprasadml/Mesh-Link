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
public final class UserBehaviorEngine_Factory implements Factory<UserBehaviorEngine> {
  private final Provider<LearningRepository> learningRepositoryProvider;

  public UserBehaviorEngine_Factory(Provider<LearningRepository> learningRepositoryProvider) {
    this.learningRepositoryProvider = learningRepositoryProvider;
  }

  @Override
  public UserBehaviorEngine get() {
    return newInstance(learningRepositoryProvider.get());
  }

  public static UserBehaviorEngine_Factory create(
      Provider<LearningRepository> learningRepositoryProvider) {
    return new UserBehaviorEngine_Factory(learningRepositoryProvider);
  }

  public static UserBehaviorEngine newInstance(LearningRepository learningRepository) {
    return new UserBehaviorEngine(learningRepository);
  }
}
