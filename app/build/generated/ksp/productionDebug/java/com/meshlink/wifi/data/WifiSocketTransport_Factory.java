package com.meshlink.wifi.data;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class WifiSocketTransport_Factory implements Factory<WifiSocketTransport> {
  @Override
  public WifiSocketTransport get() {
    return newInstance();
  }

  public static WifiSocketTransport_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static WifiSocketTransport newInstance() {
    return new WifiSocketTransport();
  }

  private static final class InstanceHolder {
    private static final WifiSocketTransport_Factory INSTANCE = new WifiSocketTransport_Factory();
  }
}
