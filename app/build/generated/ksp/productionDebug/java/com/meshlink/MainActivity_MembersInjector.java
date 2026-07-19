package com.meshlink;

import com.meshlink.domain.repository.MeshRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<MeshRepository> meshRepositoryProvider;

  public MainActivity_MembersInjector(Provider<MeshRepository> meshRepositoryProvider) {
    this.meshRepositoryProvider = meshRepositoryProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<MeshRepository> meshRepositoryProvider) {
    return new MainActivity_MembersInjector(meshRepositoryProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectMeshRepository(instance, meshRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.meshlink.MainActivity.meshRepository")
  public static void injectMeshRepository(MainActivity instance, MeshRepository meshRepository) {
    instance.meshRepository = meshRepository;
  }
}
