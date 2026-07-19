package com.meshlink.di;

import android.bluetooth.BluetoothManager;
import android.content.Context;
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
public final class BluetoothModule_ProvideBluetoothManagerFactory implements Factory<BluetoothManager> {
  private final Provider<Context> contextProvider;

  public BluetoothModule_ProvideBluetoothManagerFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public BluetoothManager get() {
    return provideBluetoothManager(contextProvider.get());
  }

  public static BluetoothModule_ProvideBluetoothManagerFactory create(
      Provider<Context> contextProvider) {
    return new BluetoothModule_ProvideBluetoothManagerFactory(contextProvider);
  }

  public static BluetoothManager provideBluetoothManager(Context context) {
    return Preconditions.checkNotNullFromProvides(BluetoothModule.INSTANCE.provideBluetoothManager(context));
  }
}
