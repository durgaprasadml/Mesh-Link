package com.meshlink.di;

import android.content.Context;
import com.meshlink.database.data.local.MeshDatabase;
import com.meshlink.security.data.DatabaseSecurityManager;
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
public final class DatabaseModule_ProvideMeshDatabaseFactory implements Factory<MeshDatabase> {
  private final Provider<Context> contextProvider;

  private final Provider<DatabaseSecurityManager> databaseSecurityManagerProvider;

  public DatabaseModule_ProvideMeshDatabaseFactory(Provider<Context> contextProvider,
      Provider<DatabaseSecurityManager> databaseSecurityManagerProvider) {
    this.contextProvider = contextProvider;
    this.databaseSecurityManagerProvider = databaseSecurityManagerProvider;
  }

  @Override
  public MeshDatabase get() {
    return provideMeshDatabase(contextProvider.get(), databaseSecurityManagerProvider.get());
  }

  public static DatabaseModule_ProvideMeshDatabaseFactory create(Provider<Context> contextProvider,
      Provider<DatabaseSecurityManager> databaseSecurityManagerProvider) {
    return new DatabaseModule_ProvideMeshDatabaseFactory(contextProvider, databaseSecurityManagerProvider);
  }

  public static MeshDatabase provideMeshDatabase(Context context,
      DatabaseSecurityManager databaseSecurityManager) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideMeshDatabase(context, databaseSecurityManager));
  }
}
