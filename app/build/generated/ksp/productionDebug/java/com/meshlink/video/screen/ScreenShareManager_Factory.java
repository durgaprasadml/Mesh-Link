package com.meshlink.video.screen;

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
public final class ScreenShareManager_Factory implements Factory<ScreenShareManager> {
  private final Provider<Context> contextProvider;

  public ScreenShareManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ScreenShareManager get() {
    return newInstance(contextProvider.get());
  }

  public static ScreenShareManager_Factory create(Provider<Context> contextProvider) {
    return new ScreenShareManager_Factory(contextProvider);
  }

  public static ScreenShareManager newInstance(Context context) {
    return new ScreenShareManager(context);
  }
}
