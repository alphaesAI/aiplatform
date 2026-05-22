# OAuth Client Setup Guide

## Mobile Apps (Public Clients with PKCE)

### Quick Setup

1. **Register the mobile client:**
   ```bash
   pnpm oauth:register-mobile
   ```

2. **If you have existing clients, migrate them:**
   ```bash
   pnpm oauth:migrate
   ```

3. **Restart your server:**
   ```bash
   pnpm dev
   ```

---

## Understanding Client Types

### Public Clients (Mobile/SPA)
- ✅ **Cannot securely store secrets** (code can be decompiled)
- ✅ **Use PKCE** (Proof Key for Code Exchange)
- ✅ **No client_secret required**
- ✅ Examples: Android, iOS, React Native, Flutter apps

### Confidential Clients (Server-to-Server)
- ✅ **Can securely store secrets**
- ✅ **Use client_secret**
- ✅ Examples: Backend services, server-side web apps

---

## OAuth Flow for Mobile Apps

```
1. App generates PKCE challenge
   ├─ code_verifier (random string)
   └─ code_challenge (SHA256 hash)

2. Authorization Request
   GET /oauth2/authorize?
     client_id=phia-mobile-app
     &response_type=code
     &redirect_uri=phia://auth/callback
     &code_challenge=...
     &code_challenge_method=S256

3. User authenticates → Authorization code issued

4. Token Exchange (NO SECRET NEEDED)
   POST /oauth2/token
     grant_type=authorization_code
     &code=...
     &redirect_uri=phia://auth/callback
     &client_id=phia-mobile-app
     &code_verifier=...  ← PKCE replaces client_secret

5. Server validates PKCE → Issues access token
```

---

## Security Benefits

✅ **PKCE prevents authorization code interception**
- Even if code is stolen, attacker can't exchange it without code_verifier

✅ **No secrets in mobile apps**
- Follows OAuth 2.0 RFC 8252 (OAuth for Native Apps)

✅ **Industry standard**
- Used by Google, Microsoft, GitHub, Auth0

---

## Troubleshooting

### Error: "client secret must be provided"
**Cause:** Client is registered as confidential instead of public

**Fix:**
```bash
pnpm oauth:migrate
```

### Error: "PKCE required"
**Cause:** Client doesn't have `requirePKCE: true`

**Fix:** Re-register the client:
```bash
pnpm oauth:register-mobile
```

---

## Configuration Files

- `auth.config.ts` - OAuth provider settings
- `register-mobile-client.ts` - Mobile client registration
- `migrate-public-clients.ts` - Migration script

---

## References

- [OAuth 2.0 RFC 8252 - OAuth for Native Apps](https://datatracker.ietf.org/doc/html/rfc8252)
- [OAuth 2.0 RFC 7636 - PKCE](https://datatracker.ietf.org/doc/html/rfc7636)
- [Better Auth OAuth Provider Docs](https://www.better-auth.com/docs/plugins/oauth-provider)
