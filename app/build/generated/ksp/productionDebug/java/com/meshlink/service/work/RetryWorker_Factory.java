package com.meshlink.service.work;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class RetryWorker_Factory {
  public RetryWorker_Factory() {
  }

  public RetryWorker get(Context context, WorkerParameters workerParams) {
    return newInstance(context, workerParams);
  }

  public static RetryWorker_Factory create() {
    return new RetryWorker_Factory();
  }

  public static RetryWorker newInstance(Context context, WorkerParameters workerParams) {
    return new RetryWorker(context, workerParams);
  }
}
