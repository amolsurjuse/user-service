# user-service

Electra Hub user management service.

## Endpoints
- `POST /api/v1/users` register user
- `POST /api/v1/users/authenticate` validate credentials
- `GET /api/v1/users/{userId}/principal` get user principal
- `GET /api/v1/users/{userId}/profile` get user profile details
- `PUT /api/v1/users/{userId}/profile` update profile details
- `GET /api/v1/users?query=&limit=&offset=` list/search users
- `GET /api/v1/users/search/count?query=` count users by search term
- `GET /api/v1/countries` list enabled countries

## Local run
```bash
./mvnw spring-boot:run
```

## TeamCity pipeline
Provision the TeamCity pipeline with:

```bash
TEAMCITY_TOKEN='<token>' ./ci/teamcity/setup_pipeline.sh
```
