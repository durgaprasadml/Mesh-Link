package com.meshlink.enterprise.governance;

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
public final class GovernanceComplianceManager_Factory implements Factory<GovernanceComplianceManager> {
  private final Provider<SecurityGovernanceManager> securityManagerProvider;

  private final Provider<AuditManager> auditManagerProvider;

  public GovernanceComplianceManager_Factory(
      Provider<SecurityGovernanceManager> securityManagerProvider,
      Provider<AuditManager> auditManagerProvider) {
    this.securityManagerProvider = securityManagerProvider;
    this.auditManagerProvider = auditManagerProvider;
  }

  @Override
  public GovernanceComplianceManager get() {
    return newInstance(securityManagerProvider.get(), auditManagerProvider.get());
  }

  public static GovernanceComplianceManager_Factory create(
      Provider<SecurityGovernanceManager> securityManagerProvider,
      Provider<AuditManager> auditManagerProvider) {
    return new GovernanceComplianceManager_Factory(securityManagerProvider, auditManagerProvider);
  }

  public static GovernanceComplianceManager newInstance(SecurityGovernanceManager securityManager,
      AuditManager auditManager) {
    return new GovernanceComplianceManager(securityManager, auditManager);
  }
}
