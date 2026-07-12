package com.meshlink.di;

import android.content.Context;
import com.meshlink.data.media.VoiceRecorder;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideVoiceRecorderFactory implements Factory<VoiceRecorder> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideVoiceRecorderFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public VoiceRecorder get() {
    return provideVoiceRecorder(contextProvider.get());
  }

  public static AppModule_ProvideVoiceRecorderFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideVoiceRecorderFactory(contextProvider);
  }

  public static VoiceRecorder provideVoiceRecorder(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideVoiceRecorder(context));
  }
}
