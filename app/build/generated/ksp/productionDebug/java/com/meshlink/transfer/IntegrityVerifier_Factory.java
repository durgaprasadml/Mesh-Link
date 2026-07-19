package com.meshlink.transfer;

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
public final class IntegrityVerifier_Factory implements Factory<IntegrityVerifier> {
  @Override
  public IntegrityVerifier get() {
    return newInstance();
  }

  public static IntegrityVerifier_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static IntegrityVerifier newInstance() {
    return new IntegrityVerifier();
  }

  private static final class InstanceHolder {
    private static final IntegrityVerifier_Factory INSTANCE = new IntegrityVerifier_Factory();
  }
}
