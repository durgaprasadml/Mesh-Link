package com.meshlink.ai.data;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class LearningRepository_Factory implements Factory<LearningRepository> {
  private final Provider<Context> contextProvider;

  public LearningRepository_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public LearningRepository get() {
    return newInstance(contextProvider.get());
  }

  public static LearningRepository_Factory create(Provider<Context> contextProvider) {
    return new LearningRepository_Factory(contextProvider);
  }

  public static LearningRepository newInstance(Context context) {
    return new LearningRepository(context);
  }
}
