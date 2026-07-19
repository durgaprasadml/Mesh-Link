package com.meshlink.emergency.medical;

import com.meshlink.security.data.TrustManager;
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
public final class MedicalManager_Factory implements Factory<MedicalManager> {
  private final Provider<TrustManager> trustManagerProvider;

  public MedicalManager_Factory(Provider<TrustManager> trustManagerProvider) {
    this.trustManagerProvider = trustManagerProvider;
  }

  @Override
  public MedicalManager get() {
    return newInstance(trustManagerProvider.get());
  }

  public static MedicalManager_Factory create(Provider<TrustManager> trustManagerProvider) {
    return new MedicalManager_Factory(trustManagerProvider);
  }

  public static MedicalManager newInstance(TrustManager trustManager) {
    return new MedicalManager(trustManager);
  }
}
