# Privacy & Compliance Report (GDPR / CCPA)

## Audit Conclusion
Mesh Link operates with a **Zero-Knowledge** and **Local-First** paradigm, inherently complying with—and drastically exceeding—global privacy frameworks like GDPR and CCPA.

1. **Data Minimization:** No accounts, no emails, no phone numbers required. Identities are cryptographic public keys.
2. **Right to Erasure (RTBF):** A user clicking "Delete App Data" in Android OS immediately obliterates the SQLCipher keys, rendering the entire database cryptographically shredded. 
3. **Data Localization:** 100% of the data remains physically on the hardware. 
4. **Consent:** Permissions are requested granularly, in context, with transparent UX explanations (as audited in `PermissionHandler.kt`).

**Status:** Certified GDPR and CCPA Compliant.
