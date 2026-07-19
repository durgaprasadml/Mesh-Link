package com.meshlink.ble.discovery;

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
public final class BatteryAwareScanner_Factory implements Factory<BatteryAwareScanner> {
  private final Provider<Context> contextProvider;

  public BatteryAwareScanner_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public BatteryAwareScanner get() {
    return newInstance(contextProvider.get());
  }

  public static BatteryAwareScanner_Factory create(Provider<Context> contextProvider) {
    return new BatteryAwareScanner_Factory(contextProvider);
  }

  public static BatteryAwareScanner newInstance(Context context) {
    return new BatteryAwareScanner(context);
  }
}
