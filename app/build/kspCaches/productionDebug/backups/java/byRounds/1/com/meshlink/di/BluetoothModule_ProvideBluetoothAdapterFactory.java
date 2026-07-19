package com.meshlink.di;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
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
public final class BluetoothModule_ProvideBluetoothAdapterFactory implements Factory<BluetoothAdapter> {
  private final Provider<BluetoothManager> bluetoothManagerProvider;

  public BluetoothModule_ProvideBluetoothAdapterFactory(
      Provider<BluetoothManager> bluetoothManagerProvider) {
    this.bluetoothManagerProvider = bluetoothManagerProvider;
  }

  @Override
  public BluetoothAdapter get() {
    return provideBluetoothAdapter(bluetoothManagerProvider.get());
  }

  public static BluetoothModule_ProvideBluetoothAdapterFactory create(
      Provider<BluetoothManager> bluetoothManagerProvider) {
    return new BluetoothModule_ProvideBluetoothAdapterFactory(bluetoothManagerProvider);
  }

  public static BluetoothAdapter provideBluetoothAdapter(BluetoothManager bluetoothManager) {
    return BluetoothModule.INSTANCE.provideBluetoothAdapter(bluetoothManager);
  }
}
