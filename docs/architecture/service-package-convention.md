# Service Package Convention

This convention applies to implemented backend services:

- `api-gateway`
- `auth-service`
- `tenant-service`
- `knowledge-base-service`

## Allowed Top-Level Packages

Under each service root package `com.opspilot.<service>`, use only these top-level package names:

- `config`
- `controller`
- `service`
- `repository`
- `domain`
- `dto`
- `mapper`
- `exception`
- `security`
- `util`

## Domain Package Rule

- Domain entities must be under `domain/entity`.
- Do not place entities in a top-level `entity` package.
- If a service has repositories, it must have at least one package declaration under `domain.entity`.

## Subcategory Rule

Use subfolders only under their parent layer when needed. Example patterns:

- `service/storage`
- `service/embedding`
- `service/messaging`
- `service/integration`
- `util/logging`

Do not create ad-hoc top-level folders for those subcategories.

## Forbidden Top-Level Folders

These top-level package folders are not allowed in implemented services:

- `entity`
- `client`
- `logging`
- `chunking`
- `embedding`
- `messaging`
- `storage`

## Automated Verification

Run:

```bash
bash scripts/verify-service-structure.sh
```

The check fails when unsupported/legacy top-level package usage is detected or repository-backed services do not use `domain.entity`.
