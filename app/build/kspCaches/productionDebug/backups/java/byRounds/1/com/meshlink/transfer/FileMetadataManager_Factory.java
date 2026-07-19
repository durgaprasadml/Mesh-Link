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
public final class FileMetadataManager_Factory implements Factory<FileMetadataManager> {
  @Override
  public FileMetadataManager get() {
    return newInstance();
  }

  public static FileMetadataManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static FileMetadataManager newInstance() {
    return new FileMetadataManager();
  }

  private static final class InstanceHolder {
    private static final FileMetadataManager_Factory INSTANCE = new FileMetadataManager_Factory();
  }
}
